package com.maxrave.simpmusic.playbacksync

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.LaunchRestoreGate
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

private const val TAG = "PlaybackSync"

/** What one phone was playing, as uploaded for the other phones to continue from. */
@Serializable
data class PlaybackSnapshot(
    val deviceId: String,
    val deviceName: String,
    val savedAt: Long,
    val tracks: List<Track>,
    val index: Int,
    val positionMs: Long,
    val playlistId: String? = null,
    val playlistName: String? = null,
    val playlistType: String? = null,
    val continuation: String? = null,
) {
    val track: Track? get() = tracks.getOrNull(index)
}

/**
 * Keeps the part of the queue around [index]: a radio queue grows without end, and the whole of it
 * would only make the upload large. Returns the kept tracks and where [index] landed in them.
 */
internal fun <T> windowQueue(
    tracks: List<T>,
    index: Int,
    keepBehind: Int = 50,
    keepAhead: Int = 300,
): Pair<List<T>, Int> {
    val from = (index - keepBehind).coerceAtLeast(0)
    val to = (index + 1 + keepAhead).coerceAtMost(tracks.size)
    return tracks.subList(from, to) to index - from
}

/**
 * Fork: "continue on the other phone" through the user's own Cloudflare Worker
 * (`scripts/playback-sync-worker`). Each phone uploads its song, queue and position. When the app
 * starts fresh, a newer upload from another phone is written where the launch restore reads, before
 * that restore runs, so the app opens on the other phone's song, paused, with no switch on screen.
 * Coming back to an app that was still running keeps its own song, and Home offers the other
 * phone's instead ([remote]). Only the last upload is kept, so whichever phone played last wins.
 */
object PlaybackSync : KoinComponent {
    const val KEY_URL = "playback_sync_url"
    const val KEY_SECRET = "playback_sync_key"
    const val KEY_DEVICE_NAME = "playback_sync_device_name"
    private const val KEY_DEVICE_ID = "playback_sync_device_id"

    /** `savedAt` of the last snapshot this phone loaded, continued from or dismissed. */
    private const val KEY_HANDLED_AT = "playback_sync_handled_at"

    /** Covers a phone that dies mid-song without pausing first; song changes and pauses upload at once. */
    private const val PERIODIC_UPLOAD_MS = 3 * 60_000L

    /** Skipping through several songs uploads once, not once per song. */
    private const val SETTLE_MS = 1_500L

    /** The launch fetch gives up after this, so an unreachable server barely delays the player. */
    private const val LAUNCH_FETCH_TIMEOUT_MS = 2_500L

    enum class ConnectionCheck { OK, NOT_SET, UNAUTHORIZED, FAILED }

    private val dataStore: DataStoreManager by inject()
    private val handler: MediaPlayerHandler by inject()
    private val songRepository: SongRepository by inject()

    private val json = Json { ignoreUnknownKeys = true }
    private val client by lazy { HttpClient(CIO) { install(HttpTimeout) { requestTimeoutMillis = 15_000 } } }

    // Main: the player's position is read on its own thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var defaultDeviceName = "SimpMusic"
        private set

    private val _remote = MutableStateFlow<PlaybackSnapshot?>(null)

    /** Another phone's snapshot for Home to offer: newer than anything this phone has handled. */
    val remote: StateFlow<PlaybackSnapshot?> = _remote.asStateFlow()

    /**
     * Starts the launch pull, then uploading. Must run before anything creates the player handler:
     * the gate has to exist by the time the handler's launch restore checks it.
     */
    fun start(defaultDeviceName: String) {
        this.defaultDeviceName = defaultDeviceName
        val pulled = CompletableDeferred<Unit>()
        LaunchRestoreGate.pending = pulled
        scope.launch {
            try {
                pullOnLaunch()
            } finally {
                pulled.complete(Unit)
            }
        }
        // Nothing is uploaded until this phone plays in this process: the queue restored at launch,
        // whether its own or just pulled from another phone, is not new listening and must not
        // overwrite the newer snapshot.
        var hasPlayed = false
        scope.launch {
            combine(
                handler.nowPlaying.map { it?.mediaId },
                handler.controlState.map { it.isPlaying },
            ) { mediaId, isPlaying -> mediaId to isPlaying }
                .distinctUntilChanged()
                .collectLatest { (mediaId, isPlaying) ->
                    if (isPlaying) hasPlayed = true
                    if (!hasPlayed || mediaId == null) return@collectLatest
                    delay(SETTLE_MS)
                    upload()
                    while (isPlaying) {
                        delay(PERIODIC_UPLOAD_MS)
                        upload()
                    }
                }
        }
    }

    /** Writes another phone's newer snapshot where the launch restore reads, before it runs. */
    private suspend fun pullOnLaunch() {
        val snapshot = fetchNewer(LAUNCH_FETCH_TIMEOUT_MS) ?: return
        val track = snapshot.track ?: return
        Logger.i(TAG, "Opening on ${track.videoId} at ${snapshot.positionMs} ms from ${snapshot.deviceName}")
        // The restore looks the playing song up by id, so it must be in this phone's database.
        songRepository.insertSong(track.toSongEntity()).first()
        songRepository.recoverQueue(snapshot.tracks)
        dataStore.setPlaylistFromSaved(snapshot.playlistName.orEmpty())
        dataStore.saveRecentSong(track.videoId, snapshot.positionMs)
        dataStore.putString(KEY_HANDLED_AT, snapshot.savedAt.toString())
    }

    /** Fetches what Home offers in [remote]. Called each time Home comes to the front. */
    suspend fun refreshRemote() {
        // At launch, after the launch pull, so Home never offers the song that pull just loaded.
        LaunchRestoreGate.await()
        val snapshot = fetchNewer() ?: return run { _remote.value = null }
        val alreadyHere = handler.nowPlaying.value?.mediaId == snapshot.track?.videoId
        _remote.value = snapshot.takeUnless { alreadyHere }
    }

    /** Hides [snapshot] from Home for good, whether it was continued or dismissed. */
    fun markHandled(snapshot: PlaybackSnapshot) {
        _remote.value = null
        scope.launch { dataStore.putString(KEY_HANDLED_AT, snapshot.savedAt.toString()) }
    }

    /** The stored snapshot, if it comes from another device and is newer than any handled here. */
    private suspend fun fetchNewer(timeoutMs: Long? = null): PlaybackSnapshot? {
        val (url, secret) = endpoint() ?: return null
        val response =
            runCatching {
                withContext(Dispatchers.IO) {
                    client.get(url) {
                        bearerAuth(secret)
                        timeoutMs?.let { timeout { requestTimeoutMillis = it } }
                    }
                }
            }.getOrElse {
                Logger.w(TAG, "Fetch failed: ${it.message}")
                return null
            }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) {
            Logger.w(TAG, "Fetch failed: HTTP ${response.status.value}")
            return null
        }
        val snapshot =
            runCatching { json.decodeFromString<PlaybackSnapshot>(response.bodyAsText()) }
                .getOrElse {
                    Logger.w(TAG, "Unreadable snapshot: ${it.message}")
                    return null
                }
        val handledAt = dataStore.getString(KEY_HANDLED_AT).first()?.toLongOrNull() ?: 0L
        return snapshot.takeIf { it.deviceId != deviceId() && it.savedAt > handledAt && it.track != null }
    }

    suspend fun checkConnection(): ConnectionCheck {
        val (url, secret) = endpoint() ?: return ConnectionCheck.NOT_SET
        val status =
            runCatching { withContext(Dispatchers.IO) { client.get(url) { bearerAuth(secret) } }.status }
                .getOrElse {
                    Logger.w(TAG, "Connection check failed: ${it.message}")
                    return ConnectionCheck.FAILED
                }
        return when {
            status.isSuccess() || status == HttpStatusCode.NotFound -> ConnectionCheck.OK
            status == HttpStatusCode.Unauthorized -> ConnectionCheck.UNAUTHORIZED
            else -> ConnectionCheck.FAILED
        }
    }

    suspend fun deviceName(): String = dataStore.getString(KEY_DEVICE_NAME).first()?.takeIf { it.isNotBlank() } ?: defaultDeviceName

    private suspend fun upload() {
        val (url, secret) = endpoint() ?: return
        val queue = handler.queueData.value?.takeIf { it.queueState == QueueData.StateSource.STATE_INITIALIZED }?.data ?: return
        val mediaId = handler.nowPlaying.value?.mediaId ?: return
        val index =
            handler.currentOrderIndex().takeIf { queue.listTracks.getOrNull(it)?.videoId == mediaId }
                ?: queue.listTracks.indexOfFirst { it.videoId == mediaId }
        if (index < 0) return
        val (tracks, windowIndex) = windowQueue(queue.listTracks, index)
        val snapshot =
            PlaybackSnapshot(
                deviceId = deviceId(),
                deviceName = deviceName(),
                savedAt = System.currentTimeMillis(),
                tracks = tracks,
                index = windowIndex,
                positionMs = handler.getProgress().coerceAtLeast(0L),
                playlistId = queue.playlistId,
                playlistName = queue.playlistName,
                playlistType = queue.playlistType?.name,
                continuation = queue.continuation,
            )
        runCatching {
            withContext(Dispatchers.IO) {
                client.put(url) {
                    bearerAuth(secret)
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(PlaybackSnapshot.serializer(), snapshot))
                }
            }
        }.onSuccess {
            if (it.status.isSuccess()) {
                // This phone now holds the newest snapshot, so nothing older is worth offering here.
                _remote.value = null
                Logger.d(TAG, "Uploaded ${tracks.size} tracks at ${snapshot.positionMs} ms")
            } else {
                Logger.w(TAG, "Upload failed: HTTP ${it.status.value}")
            }
        }.onFailure { Logger.w(TAG, "Upload failed: ${it.message}") }
    }

    /** The snapshot URL and key, or null until both are set. HTTPS only: the key travels in a header. */
    private suspend fun endpoint(): Pair<String, String>? {
        val url = dataStore.getString(KEY_URL).first()?.trim()?.trimEnd('/')
        val secret = dataStore.getString(KEY_SECRET).first()?.trim()
        if (url.isNullOrEmpty() || secret.isNullOrEmpty() || !url.startsWith("https://")) return null
        return "$url/playback" to secret
    }

    private suspend fun deviceId(): String =
        dataStore.getString(KEY_DEVICE_ID).first()
            ?: List(16) { Random.nextInt(16).toString(16) }.joinToString("").also { dataStore.putString(KEY_DEVICE_ID, it) }
}
