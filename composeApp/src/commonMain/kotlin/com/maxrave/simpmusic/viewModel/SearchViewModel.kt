package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.type.SearchResultType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toQueryList
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts
import simpmusic.composeapp.generated.resources.songs

// State cho tìm kiếm
data class SearchScreenState(
    val searchType: SearchType = SearchType.SONGS,
    // Songs and videos in one list, best match first: see [mergeByMatch].
    val searchSongsResult: List<SearchResultType> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<SearchResultType> = emptyList(),
)

// Loại tìm kiếm
enum class SearchType {
    SONGS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): StringResource =
    when (this) {
        SearchType.SONGS -> Res.string.songs
        SearchType.ALBUMS -> Res.string.albums
        SearchType.ARTISTS -> Res.string.artists
        SearchType.PLAYLISTS -> Res.string.playlists
        SearchType.PODCASTS -> Res.string.podcasts
    }

// UI state cho tìm kiếm
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

class SearchViewModel(
    private val dataStoreManager: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    /**
     * Fork: the songs played most recently, newest first, for the row at the top of the search
     * landing — YouTube Music shows the same row there. Re-read each time the landing shows.
     */
    private val _recentSongs: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val recentSongs: StateFlow<List<SongEntity>> get() = _recentSongs.asStateFlow()

    fun loadRecentSongs() {
        viewModelScope.launch {
            _recentSongs.value = runCatching { songRepository.getRecentSong(RECENT_SONG_COUNT, 0) }.getOrDefault(emptyList())
        }
    }

    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    /** Browse categories shown on the empty search screen, the way YouTube Music itself does. */
    private val _moodAndGenres: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val moodAndGenres: StateFlow<Mood?> get() = _moodAndGenres.asStateFlow()

    /** Cover art per category params, filled in as tiles scroll into view. */
    private val _moodArtwork: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    val moodArtwork: StateFlow<Map<String, String>> get() = _moodArtwork.asStateFlow()

    private val requestedArtwork = mutableSetOf<String>()

    var regionCode: String? = null
    var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        getSearchHistory()
        getMoodAndGenres()
    }

    /**
     * Fetched once for the app's lifetime — this ViewModel is a Koin `single`, and the category
     * list changes about as often as YouTube ships a new mood, so re-fetching on every visit to
     * the search tab would be pure waste.
     */
    private fun getMoodAndGenres() {
        if (_moodAndGenres.value != null) return
        viewModelScope.launch {
            homeRepository.getMoodAndMomentsData().collect { resource ->
                if (resource is Resource.Success) {
                    _moodAndGenres.value = resource.data
                }
            }
        }
    }

    /**
     * Resolve the cover for one category. Called from the tile itself, so only categories the user
     * actually scrolls to cost a request — [requestedArtwork] keeps a tile scrolling in and out of
     * view from firing it again.
     */
    fun loadMoodArtwork(params: String) {
        if (!requestedArtwork.add(params)) return
        viewModelScope.launch {
            homeRepository.getMoodCategoryArtwork(params).collect { url ->
                if (url != null) {
                    _moodArtwork.update { it + (params to url) }
                }
            }
        }
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            searchRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().reversed().let { list ->
                        _searchHistory.value = list
                        log("Search history updated: $list")
                    }
                } else {
                    _searchHistory.value = emptyList()
                    log("Search history is empty")
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            searchRepository.insertSearchHistory(SearchHistory(query = query)).collectLatest {
                Logger.d(tag, "Inserted search history: $query, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            searchRepository.deleteSearchHistory()
            delay(1000)
            getSearchHistory()
        }
    }

    /**
     * Songs and videos together: a lot of music (remixes, covers, uploads) exists only as a video,
     * and YouTube's Songs filter never shows it. Both filters are asked and the results merged by
     * how well they match the query.
     */
    fun searchSongs(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            val songs = async { searchRepository.getSearchDataSong(query).first() }
            val videos = async { searchRepository.getSearchDataVideo(query).first() }
            val songResult = songs.await()
            val videoResult = videos.await()
            if (songResult is Resource.Error && videoResult is Resource.Error) {
                _searchScreenUIState.value = SearchScreenUIState.Error
                return@launch
            }
            _searchScreenState.update { state ->
                state.copy(
                    searchType = SearchType.SONGS,
                    searchSongsResult = mergeByMatch(query, songResult.data.orEmpty(), videoResult.data.orEmpty()),
                )
            }
            _searchScreenUIState.value = SearchScreenUIState.Success
        }
    }

    fun suggestQuery(query: String) {
        viewModelScope.launch {
            searchRepository.getSuggestQuery(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { suggestData ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    suggestQueries = suggestData.queries,
                                    suggestYTItems = suggestData.recommendedItems,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        // Không cần xử lý lỗi đặc biệt cho gợi ý
                        log("Error fetching suggest queries: ${values.message}", LogLevel.ERROR)
                    }
                }
            }
        }
    }

    fun searchAlbums(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataAlbum(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { albumsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ALBUMS,
                                    searchAlbumsResult = albumsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchPodcast(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataPodcast(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { podcastList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PODCASTS,
                                    searchPodcastsResult = podcastList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchArtists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            searchRepository.getSearchDataArtist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { artistsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ARTISTS,
                                    searchArtistsResult = artistsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }

                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    /** YouTube's own (featured) playlists first, then community ones. */
    fun searchPlaylists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            val featured = async { searchRepository.getSearchDataFeaturedPlaylist(query).first() }
            val community = async { searchRepository.getSearchDataPlaylist(query).first() }
            val featuredResult = featured.await()
            val communityResult = community.await()
            if (featuredResult is Resource.Error && communityResult is Resource.Error) {
                _searchScreenUIState.value = SearchScreenUIState.Error
                return@launch
            }
            _searchScreenState.update { state ->
                state.copy(
                    searchType = SearchType.PLAYLISTS,
                    searchPlaylistsResult =
                        (featuredResult.data.orEmpty() + communityResult.data.orEmpty()).distinctBy { it.browseId },
                )
            }
            _searchScreenUIState.value = SearchScreenUIState.Success
        }
    }

    fun setSearchType(searchType: SearchType) {
        _searchScreenState.update { state ->
            state.copy(searchType = searchType)
        }
    }
}
// How many recently played songs the search landing offers.
private const val RECENT_SONG_COUNT = 15

/**
 * Songs and videos merged, best match first. A result scores one point per query word found among
 * the words of its title and artists, compared without case or accents ("nuoc mat" matches "Nước
 * Mắt", đ as d). Ties keep each list's own order, interleaved song, video, song…, so with no word
 * to tell them apart YouTube's ranking still decides.
 */
internal fun mergeByMatch(
    query: String,
    songs: List<SongsResult>,
    videos: List<VideosResult>,
): List<SearchResultType> {
    val queryWords = searchWords(query)
    fun score(title: String?, artists: List<String>): Int {
        val words = searchWords(title.orEmpty() + " " + artists.joinToString(" "))
        return queryWords.count { it in words }
    }
    data class Ranked(val item: SearchResultType, val videoId: String, val score: Int, val rank: Int, val isVideo: Boolean)
    val ranked =
        songs.mapIndexed { i, it -> Ranked(it, it.videoId, score(it.title, it.artists.orEmpty().map { a -> a.name }), i, false) } +
            videos.mapIndexed { i, it -> Ranked(it, it.videoId, score(it.title, it.artists.orEmpty().map { a -> a.name }), i, true) }
    return ranked
        .sortedWith(compareByDescending<Ranked> { it.score }.thenBy { it.rank }.thenBy { it.isVideo })
        .distinctBy { it.videoId }
        .map { it.item }
}

private fun searchWords(text: String): Set<String> =
    Normalizer
        .normalize(text.lowercase().replace('đ', 'd'), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() }
        .toSet()
