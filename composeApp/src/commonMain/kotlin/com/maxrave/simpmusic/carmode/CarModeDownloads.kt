package com.maxrave.simpmusic.carmode

import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.player.LiveStreamRegistry
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toLyricsEntity
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.CustomLyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ln
import kotlin.time.Clock

/**
 * Fork: Car mode's offline library. While Car mode is on it keeps downloaded, at the streaming
 * quality:
 * - **every liked song**, never deleted, and
 * - **a pool of about [POOL_BYTES]**: every song heard at least [LISTENED_FRACTION] of the way, plus,
 *   once, the play history and the [TOP_PLAYED] most played. The top songs rank first, the rest
 *   by [keepScore]. Nothing leaves it until it is over budget; then the lowest-placed goes first.
 *
 * A song heard in Car mode is already fully in the player cache (Car mode buffers whole songs), and
 * the download reads through that cache, so downloading it costs almost no data.
 *
 * - **Gently**: one download at a time, nothing in the first [LAUNCH_QUIET_MS] after the app
 *   starts, and none started while a song is loading — the player always gets the connection first.
 * - **Owns only its own downloads.** It remembers which ones it made ([KEY_OWNED]) and only ever
 *   deletes those. Songs downloaded by hand count toward [CAP_BYTES] but are never touched.
 * - **Stops at [CAP_BYTES]** of downloads in total: liked songs, the pool and manual downloads.
 * - Runs every [CYCLE_MS], when Car mode is switched on, after each newly listened song, and on
 *   [downloadNow].
 */
object CarModeDownloads : KoinComponent {
    private const val TAG = "CarModeDownloads"
    private const val CAP_BYTES = 20L * 1024 * 1024 * 1024
    private const val POOL_BYTES = 10L * 1024 * 1024 * 1024
    private const val LISTENED_FRACTION = 0.8

    /** Also kept: the most played songs, whenever they were last heard. */
    private const val TOP_PLAYED = 500
    private const val HISTORY_PAGE = 200

    /** Shorter tracks (jingles, intros) are not worth a download. */
    private const val MIN_LISTENED_TRACK_MS = 60_000L

    /** Pool sizes are estimated: 256 kbps is 32 kB a second, and a song of unknown length counts as 4 minutes. */
    private const val BYTES_PER_SECOND = 32_000L
    private const val UNKNOWN_LENGTH_SECONDS = 240

    private const val LAUNCH_QUIET_MS = 30_000L
    private const val CYCLE_MS = 20 * 60_000L

    /** A song that starts within this of a new track waits: the player is resolving and buffering. */
    private const val SONG_START_QUIET_MS = 15_000L
    private const val START_TIMEOUT_MS = 60_000L
    private const val FINISH_TIMEOUT_MS = 10 * 60_000L
    private const val KEY_OWNED = "car_mode_downloads"

    /** `videoId:epochMs` of each song eligible for the pool (heard to 80%, or seeded), comma separated. */
    private const val KEY_LISTENED = "car_mode_listened"

    /** Set once the pool has been seeded from play history and the top songs. */
    private const val KEY_SEEDED = "car_mode_seeded"
    private const val GB = 1024L * 1024 * 1024

    private val dataStore: DataStoreManager by inject()
    private val songRepository: SongRepository by inject()
    private val downloadHandler: DownloadHandler by inject()
    private val cacheRepository: CacheRepository by inject()
    private val player: MediaPlayerHandler by inject()
    private val lyricsRepository: LyricsCanvasRepository by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)

    /** Guards [KEY_LISTENED]: the listener writes it while a cycle prunes it. */
    private val listenedLock = Mutex()

    /** Songs that failed this session: tried again after a restart or [downloadNow], not every cycle. */
    private val failed = mutableSetOf<String>()

    private var lastTrackChangeAtMs = 0L

    data class Status(
        val songs: Int = 0,
        val bytes: Long = 0,
        val capBytes: Long = CAP_BYTES,
        val poolBytes: Long = 0,
        val poolCapBytes: Long = POOL_BYTES,
        val waiting: Int = 0,
        val downloadingTitle: String? = null,
        val full: Boolean = false,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private suspend fun carModeOn() = dataStore.carMode.first() == DataStoreManager.TRUE

    fun start() {
        val startedAtMs = nowMs()
        scope.launch {
            player.nowPlaying
                .map { it?.mediaId }
                .distinctUntilChanged()
                .collectLatest { lastTrackChangeAtMs = nowMs() }
        }
        scope.launch { watchListening() }
        // Settings shows real numbers from the start, not zeros until the first cycle.
        scope.launch { runCatching { publish(poolBytes = 0, waiting = 0) } }
        scope.launch {
            dataStore.carMode
                .map { it == DataStoreManager.TRUE }
                .distinctUntilChanged()
                .collectLatest { on ->
                    if (!on) return@collectLatest
                    val quietLeft = LAUNCH_QUIET_MS - (nowMs() - startedAtMs)
                    if (quietLeft > 0) delay(quietLeft)
                    while (true) {
                        runCatching { cycle() }.onFailure { Logger.e(TAG, "Cycle failed: ${it.message}", it) }
                        withTimeoutOrNull(CYCLE_MS) { wakeUp.receive() }
                    }
                }
        }
    }

    /** Settings → Car mode → Offline songs: runs a cycle at once and retries earlier failures. */
    fun downloadNow() {
        failed.clear()
        wakeUp.trySend(Unit)
    }

    /**
     * Credits the playing song to the pool once its position passes [LISTENED_FRACTION] of its
     * length, once per play. Sampled every two seconds: the position itself is published at 20 Hz.
     */
    @OptIn(FlowPreview::class)
    private suspend fun watchListening() {
        var creditedId: String? = null
        var lastId: String? = null
        player.simpleMediaState
            .filterIsInstance<SimpleMediaState.Progress>()
            .sample(2_000)
            .collect { progress ->
                val videoId = player.nowPlaying.value?.mediaId ?: return@collect
                if (videoId != lastId) {
                    lastId = videoId
                    creditedId = null
                }
                if (creditedId == videoId || !carModeOn() || LiveStreamRegistry.isLive(videoId)) return@collect
                val durationMs = withContext(Dispatchers.Main) { player.getPlayerDuration() }
                if (durationMs < MIN_LISTENED_TRACK_MS) return@collect
                if (progress.progress < durationMs * LISTENED_FRACTION) return@collect
                creditedId = videoId
                listenedLock.withLock {
                    val listened = readListened()
                    listened[videoId] = nowMs()
                    writeListened(listened)
                }
                Logger.i(TAG, "Listened to $videoId: added to the offline pool")
                wakeUp.trySend(Unit)
            }
    }

    private suspend fun cycle() {
        val liked = songRepository.getLikedSongs().first()
        val likedIds = liked.mapTo(mutableSetOf()) { it.videoId }
        val owned = readOwned()

        // Everything that may hold a place in the pool: songs heard to 80%, the one-time seed, and
        // whatever Car mode already downloaded — nothing leaves just because it fell out of a list.
        val eligible =
            listenedLock.withLock {
                val record = readListened()
                if (dataStore.getString(KEY_SEEDED).first() != DataStoreManager.TRUE) {
                    seedFromHistory(record)
                    writeListened(record)
                    dataStore.putString(KEY_SEEDED, DataStoreManager.TRUE)
                }
                record.keys.toSet()
            }
        // The top 500 first, by rank, then everything else by score: the most played are the ones
        // most worth having offline, so they get a place (and download) before play history does.
        val topRank =
            songRepository
                .getTopPlayedSongs(TOP_PLAYED)
                .withIndex()
                .associate { (rank, song) -> song.videoId to rank }
        val candidates =
            (eligible + owned)
                .filter { it !in likedIds }
                .mapNotNull { songRepository.getSongById(it).first() }
                .sortedWith(compareBy<SongEntity> { topRank[it.videoId] ?: Int.MAX_VALUE }.thenByDescending { keepScore(it) })

        // The pool, in that order, as far as POOL_BYTES reaches. The rest is over the budget.
        val pool = mutableListOf<SongEntity>()
        var poolBytes = 0L
        val overBudget = mutableListOf<SongEntity>()
        for (song in candidates) {
            val size = estimatedBytes(song)
            if (poolBytes + size <= POOL_BYTES) {
                pool.add(song)
                poolBytes += size
            } else {
                overBudget.add(song)
            }
        }

        // 1. Over budget: delete the lowest-placed downloads: outside the top 500, played least and
        // longest ago.
        // Day to day that is a song or two. A cycle that would drop more than half of everything
        // means the data changed under it (a rule change once deleted 52 songs in one go), so it
        // keeps them and says so rather than guessing.
        val evict = overBudget.filter { it.videoId in owned && it.downloadState == DownloadState.STATE_DOWNLOADED }
        val massRemoval = owned.size >= 10 && evict.size * 2 > owned.size
        if (massRemoval) Logger.w(TAG, "Would remove ${evict.size} of ${owned.size} downloads at once; keeping them")
        if (!massRemoval) {
            for (song in evict) {
                Logger.i(TAG, "Removing ${song.videoId}: pool over ${POOL_BYTES / GB} GB, lowest score")
                downloadHandler.removeDownload(song.videoId)
                owned.remove(song.videoId)
            }
        }
        // Forget what is no longer downloaded (deleted by hand, or above), and what did not make the
        // pool: hearing it to 80% again brings it back.
        val gone = owned.filter { id -> songRepository.getSongById(id).first()?.downloadState == DownloadState.STATE_NOT_DOWNLOADED }
        owned.removeAll(gone.toSet())
        writeOwned(owned)
        if (!massRemoval) {
            listenedLock.withLock {
                val record = readListened()
                val keep = pool.mapTo(mutableSetOf()) { it.videoId }
                // Only drops what this cycle judged; a song credited meanwhile stays for the next one.
                record.keys.removeAll { it in eligible && it !in keep }
                writeListened(record)
            }
        }

        // 2. Fetch what is missing — the top 500, the rest of the pool by score, then liked songs —
        // one at a time, until the total cap.
        val wanted = (pool + liked).distinctBy { it.videoId }
        var missing = wanted.filter { it.downloadState == DownloadState.STATE_NOT_DOWNLOADED && it.videoId !in failed }
        publish(poolBytes, waiting = missing.size)
        Logger.i(TAG, "Liked ${liked.size}, pool ${pool.size} (~${poolBytes / 1_048_576} MB), over budget ${overBudget.size}, missing ${missing.size}, owned ${owned.size}")
        while (missing.isNotEmpty()) {
            if (!carModeOn()) return
            if (cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE) >= CAP_BYTES) {
                Logger.i(TAG, "Download cap reached, stopping with ${missing.size} still missing")
                publish(poolBytes, waiting = missing.size, full = true)
                return
            }
            waitForPlayerToSettle()
            val song = missing.first()
            missing = missing.drop(1)
            publish(poolBytes, waiting = missing.size + 1, downloadingTitle = song.title)
            if (download(song)) {
                owned.add(song.videoId)
                writeOwned(owned)
                // Offline means the whole song: its cover and its lyrics, not just the audio.
                prefetchCovers(song)
                prefetchLyrics(song)
            } else {
                failed.add(song.videoId)
            }
        }
        publish(poolBytes, waiting = 0)
    }

    /**
     * How much a song deserves its place: more plays keep it, each month since its last play counts
     * against it. `ln(1 + plays)` so the tenth play matters less than the second; a month without
     * playing costs one point. A song played once two months ago (−1.3) goes before one played 30
     * times four months ago (−0.6).
     */
    private fun keepScore(song: SongEntity): Double {
        val plays = song.totalPlayTime.coerceAtLeast(0)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val ageDays = (now.toInstant(TimeZone.UTC) - song.inLibrary.toInstant(TimeZone.UTC)).inWholeDays.coerceAtLeast(0)
        return ln(1.0 + plays) - ageDays / 30.0
    }

    /**
     * Once, the first time the pool runs: the songs already played (`totalPlayTime > 0`, most
     * recently played first, about [POOL_BYTES] of them) and the [TOP_PLAYED] most played. The app
     * has no record of how far each was heard, so every played song counts. Brings back what the
     * earlier top-200 rule had downloaded.
     */
    private suspend fun seedFromHistory(record: MutableMap<String, Long>) {
        val now = nowMs()
        var offset = 0
        var budget = 0L
        while (budget < POOL_BYTES) {
            val page = songRepository.getRecentSong(HISTORY_PAGE, offset)
            if (page.isEmpty()) break
            for (song in page) {
                if (song.totalPlayTime <= 0 || LiveStreamRegistry.isLive(song.videoId)) continue
                record.putIfAbsent(song.videoId, now)
                budget += estimatedBytes(song)
            }
            offset += page.size
        }
        songRepository.getTopPlayedSongs(TOP_PLAYED).forEach { record.putIfAbsent(it.videoId, now) }
        Logger.i(TAG, "Seeded the pool with ${record.size} songs from play history and the top $TOP_PLAYED")
    }

    /** True once the song is fully downloaded; false on failure or when it takes far too long. */
    private suspend fun download(song: SongEntity): Boolean {
        Logger.i(TAG, "Downloading ${song.videoId}")
        downloadHandler.downloadTrack(song.videoId, song.title, song.thumbnails.orEmpty())
        val terminal = setOf(DownloadState.STATE_DOWNLOADED, DownloadState.STATE_NOT_DOWNLOADED)
        // The task map may still hold a failure from an earlier attempt: wait for this one to begin.
        withTimeoutOrNull(START_TIMEOUT_MS) {
            downloadHandler.downloadTask.first { it[song.videoId]?.let { state -> state !in terminal } == true }
        } ?: run {
            // Already finished before we looked (it came straight from the player cache), or never began.
            return songRepository.getSongById(song.videoId).first()?.downloadState == DownloadState.STATE_DOWNLOADED
        }
        val state =
            withTimeoutOrNull(FINISH_TIMEOUT_MS) {
                downloadHandler.downloadTask.first { it[song.videoId] in terminal }[song.videoId]
            }
        if (state != DownloadState.STATE_DOWNLOADED) Logger.w(TAG, "Download of ${song.videoId} did not finish (state $state)")
        return state == DownloadState.STATE_DOWNLOADED
    }

    /**
     * Puts the song's cover in the image cache at both sizes the app asks for: the stored link (lists)
     * and its 544 px version (the player rewrites `=wN-hN` links to that). In Car mode the cache is
     * 2 GB and covers count as fresh for 30 days, so they stay there.
     */
    private suspend fun prefetchCovers(song: SongEntity) {
        val stored = song.thumbnails?.takeIf { it.isNotBlank() } ?: return
        val urls = setOf(stored, Regex("=w\\d+-h\\d+").replace(stored, "=w544-h544"))
        val loader = SingletonImageLoader.get(imageContext())
        for (url in urls) {
            val request =
                ImageRequest
                    .Builder(imageContext())
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            if (loader.execute(request) !is SuccessResult) Logger.w(TAG, "Cover for ${song.videoId} not cached")
        }
    }

    /**
     * Saves lyrics for the song if it has none yet, the way playback would find them: SimpMusic's
     * database first, then LRCLIB, preferring timed lyrics and settling for plain ones. A song heard
     * in Car mode usually has them already, saved when it played.
     */
    private suspend fun prefetchLyrics(song: SongEntity) {
        val videoId = song.videoId
        if (lyricsRepository.getSavedLyrics(videoId).first() != null || CustomLyrics.load(videoId) != null) return
        val artist = song.artistName?.firstOrNull().orEmpty()
        val sources =
            listOf(
                { lyricsRepository.getSimpMusicLyrics(videoId) },
                { lyricsRepository.getLrclibLyricsData(artist, song.title, song.durationSeconds) },
            )
        var plain: Lyrics? = null
        for (source in sources) {
            val lyrics = runCatching { source().first() }.getOrNull()?.takeIf { it is Resource.Success }?.data ?: continue
            if (lyrics.syncType == "LINE_SYNCED" || lyrics.syncType == "RICH_SYNCED") {
                lyricsRepository.insertLyrics(lyrics.toLyricsEntity(videoId))
                return
            }
            if (plain == null) plain = lyrics
        }
        plain?.let { lyricsRepository.insertLyrics(it.toLyricsEntity(videoId)) }
            ?: Logger.i(TAG, "No lyrics found for $videoId")
    }

    /** Waits while a song is being loaded, so the download never competes with a song starting. */
    private suspend fun waitForPlayerToSettle() {
        while (true) {
            val loading =
                player.simpleMediaState.value.let { it is SimpleMediaState.Loading || it is SimpleMediaState.Buffering }
            val sinceTrackChange = nowMs() - lastTrackChangeAtMs
            if (!loading && sinceTrackChange >= SONG_START_QUIET_MS) return
            delay(2_000)
        }
    }

    private suspend fun publish(
        poolBytes: Long,
        waiting: Int,
        downloadingTitle: String? = null,
        full: Boolean = false,
    ) {
        val songs = songRepository.getDownloadedSongs().first()?.size ?: 0
        val bytes = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
        _status.value =
            Status(
                songs = songs,
                bytes = bytes,
                poolBytes = poolBytes,
                waiting = waiting,
                downloadingTitle = downloadingTitle,
                full = full,
            )
    }

    private fun estimatedBytes(song: SongEntity): Long =
        (song.durationSeconds.takeIf { it > 0 } ?: UNKNOWN_LENGTH_SECONDS) * BYTES_PER_SECOND

    private suspend fun readOwned(): MutableSet<String> =
        dataStore
            .getString(KEY_OWNED)
            .first()
            .orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .toMutableSet()

    private suspend fun writeOwned(owned: Set<String>) = dataStore.putString(KEY_OWNED, owned.joinToString(","))

    private suspend fun readListened(): MutableMap<String, Long> =
        dataStore
            .getString(KEY_LISTENED)
            .first()
            .orEmpty()
            .split(',')
            .mapNotNull { entry ->
                val videoId = entry.substringBeforeLast(':', "")
                val at = entry.substringAfterLast(':', "").toLongOrNull()
                if (videoId.isBlank() || at == null) null else videoId to at
            }.toMap(LinkedHashMap())

    private suspend fun writeListened(listened: Map<String, Long>) =
        dataStore.putString(KEY_LISTENED, listened.entries.joinToString(",") { "${it.key}:${it.value}" })

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()
}
