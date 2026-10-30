package com.maxrave.simpmusic.playbacksync

import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.CustomLyrics
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val TAG = "SongLyricsSync"

/**
 * One song's lyrics fixes, shared between devices through the playback-sync server: the source
 * chosen for it (empty = the main provider) and the user's own lyrics. Lyrics are only ever
 * removed on another device when [lyricsRemoved] is set, which only the delete button does:
 * a record with no lyrics and no flag (a source change from a phone that has none, or a record
 * from an older build) leaves the receiving device's lyrics alone.
 */
@Serializable
private data class SongLyricsState(
    val provider: String = "",
    val lyrics: Lyrics? = null,
    val lyricsRemoved: Boolean = false,
    val updatedAt: Long,
)

/** [reached] is false when the server could not be asked; [state] is null when it has nothing for the song. */
private class Remote(
    val reached: Boolean,
    val state: SongLyricsState?,
)

object SongLyricsSync {
    private const val FETCH_TIMEOUT_MS = 2_500L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The per-song source that means "show no lyrics": for songs every provider gets wrong. It rides
     * the same synced source field as a real provider, so it reaches the other devices for free.
     */
    const val NO_LYRICS = "none"

    fun providerKey(videoId: String) = "lyrics_provider_song_$videoId"

    private fun syncedAtKey(videoId: String) = "lyrics_song_synced_at_$videoId"

    /** The server names objects in lowercase and video ids are case-sensitive, so the id goes hex-encoded. */
    private fun remoteKey(videoId: String) = "sl_" + videoId.encodeToByteArray().joinToString("") { "%02x".format(it) }

    private suspend fun fetch(videoId: String): Remote {
        val (base, secret) = PlaybackSync.server() ?: return Remote(false, null)
        val response =
            runCatching {
                withContext(Dispatchers.IO) {
                    PlaybackSync.client.get("$base/${remoteKey(videoId)}") {
                        bearerAuth(secret)
                        timeout { requestTimeoutMillis = FETCH_TIMEOUT_MS }
                    }
                }
            }.getOrNull() ?: return Remote(false, null)
        if (!response.status.isSuccess()) return Remote(response.status.value == 404, null)
        val state = runCatching { PlaybackSync.json.decodeFromString<SongLyricsState>(response.bodyAsText()) }.getOrNull()
        return Remote(state != null, state)
    }

    /**
     * Takes another device's fixes for [videoId] if they are newer than this device's. Called before
     * every lyrics lookup, so a song fixed elsewhere is right the next time it plays here.
     */
    suspend fun pull(
        videoId: String,
        dataStore: DataStoreManager,
    ) {
        val remote = fetch(videoId)
        if (!remote.reached) return
        val local = dataStore.getString(syncedAtKey(videoId)).first()?.toLongOrNull()
        val state = remote.state
        if (state == null) {
            // Fixes made before this device synced them (or while offline) have never been
            // uploaded: share them now, while the song is being looked at.
            if (local == null && (CustomLyrics.exists(videoId) || !dataStore.getString(providerKey(videoId)).first().isNullOrEmpty())) {
                push(videoId, dataStore)
            }
            return
        }
        if (state.updatedAt <= (local ?: 0L)) return
        withContext(Dispatchers.IO) {
            when {
                state.lyrics != null -> CustomLyrics.write(videoId, state.lyrics)
                state.lyricsRemoved -> CustomLyrics.delete(videoId)
            }
        }
        dataStore.putString(providerKey(videoId), state.provider)
        dataStore.putString(syncedAtKey(videoId), state.updatedAt.toString())
        Logger.i(TAG, "Took the lyrics fixes for $videoId from another device")
        // The server lost (or never had) lyrics this device still holds: put them back.
        if (state.lyrics == null && !state.lyricsRemoved && CustomLyrics.exists(videoId)) push(videoId, dataStore)
    }

    /**
     * Shares this device's current fixes for [videoId]; call after changing either of them.
     * [lyricsRemoved] is for the delete button only.
     */
    fun push(
        videoId: String,
        dataStore: DataStoreManager,
        lyricsRemoved: Boolean = false,
    ) {
        scope.launch {
            val (base, secret) = PlaybackSync.server() ?: return@launch
            var lyrics = if (lyricsRemoved) null else CustomLyrics.load(videoId)
            var removed = lyricsRemoved
            if (lyrics == null && !lyricsRemoved) {
                // This device has no lyrics of its own for the song (a source change, say): keep
                // whatever the server holds instead of overwriting it with nothing. If the server
                // cannot be asked, send nothing — the PUT would fail anyway.
                val remote = fetch(videoId)
                if (!remote.reached) {
                    Logger.w(TAG, "Not sharing the source for $videoId: could not check the server first")
                    return@launch
                }
                lyrics = remote.state?.lyrics
                removed = remote.state?.lyricsRemoved ?: false
            }
            val state =
                SongLyricsState(
                    provider = dataStore.getString(providerKey(videoId)).first().orEmpty(),
                    lyrics = lyrics,
                    lyricsRemoved = removed && lyrics == null,
                    updatedAt = System.currentTimeMillis(),
                )
            dataStore.putString(syncedAtKey(videoId), state.updatedAt.toString())
            // ponytail: no retry; a push made offline is only retried if the server has nothing for the song.
            runCatching {
                PlaybackSync.client.put("$base/${remoteKey(videoId)}") {
                    bearerAuth(secret)
                    contentType(ContentType.Application.Json)
                    setBody(PlaybackSync.json.encodeToString(SongLyricsState.serializer(), state))
                }
            }.onFailure { Logger.w(TAG, "Sharing the lyrics fixes for $videoId failed: ${it.message}") }
        }
    }
}
