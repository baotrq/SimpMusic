package com.maxrave.simpmusic.lyricsfix

import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.playbacksync.PlaybackSync
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val TAG = "LyricsFixes"

/**
 * Lyrics fixes shared with every Nhạc Nhẽo install, through the download site's Worker
 * (scripts/download-site/public/_worker.js). A fix says which source has the right lyrics for a
 * song, that it should show none, or carries lyrics a user made. Anyone can suggest one; it reaches
 * other installs only after the owner approves it. Devices holding the admin key publish at once.
 */
object LyricsFixes {
    private const val BASE = "https://nhacnheo.pages.dev/lyrics"
    private const val FETCH_TIMEOUT_MS = 2_500L
    private const val SEND_TIMEOUT_MS = 15_000L

    /** Unset counts as on: users get the owner's fixes unless they turn them off. */
    const val KEY_ENABLED = "community_lyrics_fixes"

    /** The owner's key. Never shipped in the app; typed into the hidden tools on the owner's devices. */
    const val KEY_ADMIN = "community_lyrics_admin_key"
    private const val KEY_INSTALL_ID = "community_lyrics_install_id"

    const val KIND_SOURCE = "source"
    const val KIND_LYRICS = "lyrics"
    const val KIND_NONE = "none"

    @Serializable
    data class Fix(
        val kind: String,
        val source: String? = null,
        val lyrics: Lyrics? = null,
        val updatedAt: Long = 0,
    )

    @Serializable
    private data class Suggestion(
        val videoId: String,
        val kind: String,
        val source: String? = null,
        val lyrics: Lyrics? = null,
        val title: String? = null,
        val artist: String? = null,
        val duration: Int? = null,
        val installId: String? = null,
    )

    enum class SendResult { SUGGESTED, PUBLISHED, REMOVED, TOO_MANY, FAILED }

    // Asked once per song per run: switching sources re-runs the lookup, and the answer cannot
    // change faster than the server's own cache does. Null values are kept too (no fix).
    private class Cached(
        val fix: Fix?,
    )

    private val cache = ConcurrentHashMap<String, Cached>()

    fun forget(videoId: String) {
        cache.remove(videoId)
    }

    suspend fun enabled(dataStore: DataStoreManager): Boolean = dataStore.getString(KEY_ENABLED).first() != DataStoreManager.FALSE

    private suspend fun adminKey(dataStore: DataStoreManager): String? = dataStore.getString(KEY_ADMIN).first()?.trim()?.takeIf { it.isNotEmpty() }

    suspend fun isAdmin(dataStore: DataStoreManager): Boolean = adminKey(dataStore) != null

    /** The approved fix for [videoId], or null (none, switched off, or the server could not be reached in time). */
    suspend fun fetch(
        videoId: String,
        dataStore: DataStoreManager,
    ): Fix? {
        if (!enabled(dataStore)) return null
        cache[videoId]?.let { return it.fix }
        val response =
            runCatching {
                withContext(Dispatchers.IO) {
                    PlaybackSync.client.get("$BASE/fix/$videoId") { timeout { requestTimeoutMillis = FETCH_TIMEOUT_MS } }
                }
            }.getOrNull() ?: return null // Not cached: try again on the next play.
        val fix =
            when {
                response.status.isSuccess() ->
                    runCatching { PlaybackSync.json.decodeFromString<Fix>(response.bodyAsText()) }.getOrNull()
                response.status.value == 404 -> null
                else -> return null
            }
        cache[videoId] = Cached(fix)
        if (fix != null) Logger.i(TAG, "Community fix for $videoId: ${fix.kind} ${fix.source.orEmpty()}")
        return fix
    }

    /**
     * Sends a fix for [videoId]: published at once from a device holding the admin key, otherwise
     * queued for the owner to review. [lyrics] rides along with a source pick only so the owner can
     * see what it showed; the server publishes the pick alone.
     */
    suspend fun send(
        dataStore: DataStoreManager,
        videoId: String,
        kind: String,
        source: String?,
        lyrics: Lyrics?,
        title: String?,
        artist: String?,
        duration: Int?,
    ): SendResult =
        withContext(Dispatchers.IO) {
            val admin = adminKey(dataStore)
            val body =
                Suggestion(
                    videoId = videoId,
                    kind = kind,
                    source = source,
                    // Only the fields the server reads: provider-side ids and votes stay off the wire.
                    lyrics = lyrics?.let { Lyrics(lines = it.lines, syncType = it.syncType) },
                    title = title,
                    artist = artist,
                    duration = duration?.takeIf { it > 0 },
                    installId = if (admin == null) installId(dataStore) else null,
                )
            val json = PlaybackSync.json.encodeToString(Suggestion.serializer(), body)
            val response =
                runCatching {
                    if (admin != null) {
                        PlaybackSync.client.put("$BASE/admin/fix/$videoId") {
                            bearerAuth(admin)
                            contentType(ContentType.Application.Json)
                            setBody(json)
                            timeout { requestTimeoutMillis = SEND_TIMEOUT_MS }
                        }
                    } else {
                        PlaybackSync.client.post("$BASE/suggest") {
                            contentType(ContentType.Application.Json)
                            setBody(json)
                            timeout { requestTimeoutMillis = SEND_TIMEOUT_MS }
                        }
                    }
                }.onFailure { Logger.w(TAG, "Sending the fix for $videoId failed: ${it.message}") }
                    .getOrNull() ?: return@withContext SendResult.FAILED
            when {
                response.status.isSuccess() -> {
                    Logger.i(TAG, "Sent a $kind fix for $videoId (${if (admin != null) "published" else "for review"})")
                    if (admin != null) {
                        forget(videoId)
                        SendResult.PUBLISHED
                    } else {
                        SendResult.SUGGESTED
                    }
                }
                response.status.value == 429 -> SendResult.TOO_MANY
                else -> {
                    Logger.w(TAG, "Sending the fix for $videoId: HTTP ${response.status.value}")
                    SendResult.FAILED
                }
            }
        }

    /** Takes the shared fix for [videoId] down. Admin key only; FAILED without it. */
    suspend fun remove(
        dataStore: DataStoreManager,
        videoId: String,
    ): SendResult =
        withContext(Dispatchers.IO) {
            val admin = adminKey(dataStore) ?: return@withContext SendResult.FAILED
            val ok =
                runCatching {
                    PlaybackSync.client
                        .delete("$BASE/admin/fix/$videoId") {
                            bearerAuth(admin)
                            timeout { requestTimeoutMillis = SEND_TIMEOUT_MS }
                        }.status
                        .isSuccess()
                }.onFailure { Logger.w(TAG, "Removing the fix for $videoId failed: ${it.message}") }
                    .getOrDefault(false)
            if (ok) {
                forget(videoId)
                Logger.i(TAG, "Removed the shared fix for $videoId")
                SendResult.REMOVED
            } else {
                SendResult.FAILED
            }
        }

    /** A random id per install, so the server can limit and group suggestions without knowing who sent them. */
    private suspend fun installId(dataStore: DataStoreManager): String =
        dataStore.getString(KEY_INSTALL_ID).first()?.takeIf { it.length >= 16 }
            ?: List(32) { Random.nextInt(16).toString(16) }.joinToString("").also { dataStore.putString(KEY_INSTALL_ID, it) }
}
