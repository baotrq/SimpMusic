package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.networkBecameAvailable
import com.maxrave.simpmusic.playbacksync.PlaybackSnapshot
import com.maxrave.simpmusic.playbacksync.PlaybackSync
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString as resourceString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release
import simpmusic.composeapp.generated.resources.song
import simpmusic.composeapp.generated.resources.view_count

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(null)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(null)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(true)
    val loadingChart = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _mainHomeThumbnail: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainHomeThumbnail: StateFlow<String?> = _mainHomeThumbnail

    init {
        viewModelScope.launch {
            if (dataStoreManager.cookie.first().isEmpty() &&
                dataStoreManager.shouldShowLogInRequiredAlert.first() == TRUE
            ) {
                _showLogInAlert.update { true }
            }
        }
        viewModelScope.launch {
            // One load per real change. This used to be five separate watchers (region, language,
            // cookie twice, params), each loading Home on its first value, so a launch sent Home's
            // four requests up to five times, and a signed-in launch threw its first second of
            // loading away on purpose (a delayed reload "to wait for the cookie"). The data layer now
            // waits for the session to reach YouTube itself (YouTubeSession), so one load is right.
            var lastCookie: String? = null
            combine(
                dataStoreManager.location.distinctUntilChanged(),
                dataStoreManager.language.distinctUntilChanged(),
                dataStoreManager.cookie.distinctUntilChanged(),
                params,
            ) { location, language, cookie, params -> HomeLoadKey(location, language, cookie, params) }
                .distinctUntilChanged()
                .collectLatest { key ->
                    regionCode = key.location
                    language = key.language
                    val signedInJustNow = lastCookie != null && lastCookie != key.cookie && key.cookie.isNotEmpty()
                    lastCookie = key.cookie
                    if (signedInJustNow) {
                        // A fresh sign-in writes the cookie before the account's other rows land.
                        loading.value = true
                        delay(1000)
                    }
                    getHomeItemList(key.params)
                    _accountInfo.emit(
                        Pair(
                            dataStoreManager.getString("AccountName").first(),
                            dataStoreManager.getString("AccountThumbUrl").first(),
                        ),
                    )
                }
        }
        viewModelScope.launch {
            homeItemList.collectLatest { list ->
                _mainHomeThumbnail.value =
                    list
                        .firstOrNull()
                        ?.contents
                        ?.firstOrNull()
                        ?.thumbnails
                        ?.lastOrNull()
                        ?.url
            }
        }
    }

    private data class HomeLoadKey(
        val location: String,
        val language: String,
        val cookie: String,
        val params: String?,
    )

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    /**
     * Loads the four parts of Home side by side, each shown the moment it arrives: the main feed no
     * longer waits for the chart, moods and new releases (it used to `combine` all four, so the
     * slowest decided when anything appeared).
     *
     * A part that fails is tried again by itself, quietly, when the phone gets a working connection
     * or after a growing wait — right after boot the radio is often not up yet, and nothing used to
     * try again until the user pulled to refresh. A failure keeps what is already on screen.
     */
    fun getHomeItemList(params: String? = null) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                val viewString = resourceString(Res.string.view_count)
                val songString = resourceString(Res.string.song)
                launch {
                    loadWithRetry("home", { homeRepository.getHomeData(params, viewString, songString) }) { home, attempt ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                _homeItemList.value = home.data?.second ?: listOf()
                                _homeListState.value =
                                    if (continuation.value.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                                if (dataStoreManager.cookie.first().isNotEmpty()) {
                                    _accountInfo.emit(
                                        Pair(
                                            dataStoreManager.getString("AccountName").first(),
                                            dataStoreManager.getString("AccountThumbUrl").first(),
                                        ),
                                    )
                                }
                            }

                            is Resource.Error -> {
                                Logger.w(tag, "Home failed (attempt $attempt): ${home.message}")
                                if (_homeItemList.value.isEmpty()) {
                                    _continuation.value = null
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                                if (attempt == 0) showSnackBarErrorState.emit(home.message ?: "")
                            }
                        }
                        loading.value = false
                    }
                }
                launch {
                    loadWithRetry("chart", { homeRepository.getChartData(dataStoreManager.chartKey.first()) }) { chart, _ ->
                        if (chart is Resource.Success) {
                            _chart.value = chart.data
                            regionCodeChart.value = dataStoreManager.chartKey.first()
                        }
                        loadingChart.value = false
                    }
                }
                launch {
                    loadWithRetry("new release", {
                        homeRepository.getNewRelease(
                            resourceString(Res.string.new_release),
                            resourceString(Res.string.music_video),
                        )
                    }) { newRelease, _ ->
                        if (newRelease is Resource.Success) _newRelease.value = newRelease.data ?: arrayListOf()
                    }
                }
                launch {
                    loadWithRetry("moods", { homeRepository.getMoodAndMomentsData() }) { mood, _ ->
                        if (mood is Resource.Success) _exploreMoodItem.value = mood.data
                    }
                }
            }
    }

    /**
     * Collects [source] until it answers with a success. After a failure it waits for a working
     * connection to come up, or for a growing delay, whichever is first, and asks again; it gives up
     * once [RETRY_DELAYS_MS] runs out, by which time the user has pulled to refresh or moved on.
     */
    private suspend fun <T> loadWithRetry(
        name: String,
        source: suspend () -> Flow<Resource<T>>,
        onResult: suspend (result: Resource<T>, attempt: Int) -> Unit,
    ) {
        for (attempt in 0..RETRY_DELAYS_MS.size) {
            var succeeded = false
            source().collect { result ->
                if (result is Resource.Success) succeeded = true
                onResult(result, attempt)
            }
            if (succeeded || attempt == RETRY_DELAYS_MS.size) return
            val waitMs = RETRY_DELAYS_MS[attempt]
            val cameOnline = withTimeoutOrNull(waitMs) { networkBecameAvailable().first() } != null
            Logger.d(tag, "Retrying $name (attempt ${attempt + 1}) after ${if (cameOnline) "connection came up" else "${waitMs}ms"}")
        }
    }

    /** A shelf's "Play all": YouTube's own finite queue of that shelf's songs. */
    fun playAll(item: HomeItem) {
        item.playAllEndpoint?.let { playFromEndpoint(it, item.title) }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val newItems = home.data?.second ?: listOf()
                                _homeItemList.update { it + newItems }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    /** Fork: plays the queue another phone uploaded, from the song and position it stopped at. */
    fun continueFromOtherDevice(snapshot: PlaybackSnapshot) {
        val track = snapshot.track ?: return
        PlaybackSync.markHandled(snapshot)
        val type = snapshot.playlistType?.let { name -> PlaylistType.entries.find { it.name == name } }
        // A local playlist id names a row in the other phone's database, not this one's.
        val isLocal = type == PlaylistType.LOCAL_PLAYLIST
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(snapshot.tracks),
                firstPlayedTrack = track,
                playlistId = snapshot.playlistId.takeUnless { isLocal },
                playlistName = snapshot.playlistName,
                playlistType = if (isLocal) PlaylistType.PLAYLIST else type ?: PlaylistType.PLAYLIST,
                continuation = snapshot.continuation,
            ),
        )
        loadMediaItem(track, Config.PLAYLIST_CLICK, snapshot.index)
        if (snapshot.positionMs < SKIP_SEEK_BELOW_MS) return
        viewModelScope.launch {
            val landed =
                withTimeoutOrNull(20_000) {
                    mediaPlayerHandler.nowPlaying.first { it?.mediaId == track.videoId }
                    mediaPlayerHandler.simpleMediaState.first { it is SimpleMediaState.Ready || it is SimpleMediaState.Progress }
                    mediaPlayerHandler.player.seekTo(snapshot.positionMs)
                }
            if (landed == null) Logger.w("PlaybackSync", "Track did not start in time, playing from the beginning")
        }
    }

    companion object {
        /** Waits between automatic retries of a part of Home that failed (ms); a connection coming up cuts each short. */
        private val RETRY_DELAYS_MS = longArrayOf(3_000, 6_000, 12_000, 20_000, 30_000, 30_000, 30_000, 60_000, 60_000, 60_000)

        /** A song only seconds in simply starts over. */
        private const val SKIP_SEEK_BELOW_MS = 3_000L

        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}