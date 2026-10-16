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
 * chosen for it (empty = the main provider) and the user's own lyrics (null = none). A removal
 * travels as a newer record with them cleared — the server has no DELETE.
 */
@Serializable
private data class SongLyricsState(
    val provider: String = "",
    val lyrics: Lyrics? = null,
    val updatedAt: Long,
)

object SongLyricsSync {
    private const val FETCH_TIMEOUT_MS = 2_500L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun providerKey(videoId: String) = "lyrics_provider_song_$videoId"

    private fun syncedAtKey(videoId: String) = "lyrics_song_synced_at_$videoId"

    /** The server names objects in lowercase and video ids are case-sensitive, so the id goes hex-encoded. */
    private fun remoteKey(videoId: String) = "sl_" + videoId.encodeToByteArray().joinToString("") { "%02x".format(it) }

    /**
     * Takes another device's fixes for [videoId] if they are newer than this device's. Called before
     * every lyrics lookup, so a song fixed elsewhere is right the next time it plays here.
     */
    suspend fun pull(
        videoId: String,
        dataStore: DataStoreManager,
    ) {
        val (base, secret) = PlaybackSync.server() ?: return
        val response =
            runCatching {
                withContext(Dispatchers.IO) {
                    PlaybackSync.client.get("$base/${remoteKey(videoId)}") {
                        bearerAuth(secret)
                        timeout { requestTimeoutMillis = FETCH_TIMEOUT_MS }
                    }
                }
            }.getOrNull() ?: return
        val local = dataStore.getString(syncedAtKey(videoId)).first()?.toLongOrNull()
        if (!response.status.isSuccess()) {
            // Fixes made before this device synced them (or while offline) have never been
            // uploaded: share them now, while the song is being looked at.
            if (local == null && (CustomLyrics.exists(videoId) || !dataStore.getString(providerKey(videoId)).first().isNullOrEmpty())) {
                push(videoId, dataStore)
            }
            return
        }
        val remote = runCatching { PlaybackSync.json.decodeFromString<SongLyricsState>(response.bodyAsText()) }.getOrNull() ?: return
        if (remote.updatedAt <= (local ?: 0L)) return
        withContext(Dispatchers.IO) {
            if (remote.lyrics != null) CustomLyrics.write(videoId, remote.lyrics) else CustomLyrics.delete(videoId)
        }
        dataStore.putString(providerKey(videoId), remote.provider)
        dataStore.putString(syncedAtKey(videoId), remote.updatedAt.toString())
        Logger.i(TAG, "Took the lyrics fixes for $videoId from another device")
    }

    /** Shares this device's current fixes for [videoId]; call after changing either of them. */
    fun push(
        videoId: String,
        dataStore: DataStoreManager,
    ) {
        scope.launch {
            val (base, secret) = PlaybackSync.server() ?: return@launch
            val state =
                SongLyricsState(
                    provider = dataStore.getString(providerKey(videoId)).first().orEmpty(),
                    lyrics = CustomLyrics.load(videoId),
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
