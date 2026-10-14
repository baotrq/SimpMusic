package com.maxrave.simpmusic.playbacksync

import com.maxrave.domain.data.entities.TranslatedLyricsEntity
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.SignInSyncRepository
import com.maxrave.logger.Logger
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "SettingsSync"

/** The settings blob as stored: everything but its age is AES-GCM ciphertext (see [seal]). */
@Serializable
private data class SettingsEnvelope(
    val deviceId: String,
    val updatedAt: Long,
    val sealed: String,
)

/** What the sealed blob holds. Defaults let a blob from an older build decode as empty. */
@Serializable
private data class SyncedState(
    val settings: Map<String, String> = emptyMap(),
    val signIns: String? = null,
)

/** A translated lyric sheet as shared between devices. */
@Serializable
private data class SharedTranslation(
    val syncType: String?,
    val lines: List<Line>,
)

/**
 * Fork: keeps sign-ins (YouTube, Spotify, Discord, Last.fm) and the AI API key with the settings it
 * needs the same on every device, and shares AI-translated lyrics so a song translated on one phone
 * is not paid for again on the other. Preferences stay per device. Uses the playback sync server
 * ([PlaybackSync.server]).
 *
 * The blob holds credentials, so it travels sealed with a key derived from the sync key and the
 * server only ever stores ciphertext. A change uploads a moment later; every app open pulls, and the
 * newest change wins. Sync only adds or refreshes: a sign-out or a cleared key stays on its device.
 */
object SettingsSync : KoinComponent {
    /** The AI key and what it cannot work without. All strings. Preferences are not synced. */
    private val SYNCED_KEYS =
        listOf(
            "ai_provider",
            "ai_gemini_api_key",
            "custom_model_id",
            "custom_openai_base_url",
            "custom_openai_headers",
        )

    /** DataStore keys whose change means a sign-in changed (the bundle itself is read from data). */
    private val SIGN_IN_KEYS = listOf("cookie", "logged_in", "sp_dc", "discord_token", "lastfm_session_key")

    /** The state as last uploaded or applied here, as canonical JSON. */
    private const val KEY_SYNCED = "settings_sync_synced"

    /** `updatedAt` of the newest settings blob this device has uploaded or applied. */
    private const val KEY_SYNCED_AT = "settings_sync_at"

    /** Typing a key fires a write per character; upload once it settles. */
    private const val SETTLE_MS = 2_000L

    /** Waiting on the server must never hold a translation up for long. */
    private const val TRANSLATION_FETCH_TIMEOUT_MS = 5_000L

    private val dataStore: DataStoreManager by inject()
    private val signIns: SignInSyncRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    /**
     * Completed by the first pull that gets an answer (settings applied, or none stored yet). Until
     * then nothing uploads: a fresh install, or a phone offline at launch, holds empty or stale
     * settings that would otherwise overwrite the good ones on the server.
     */
    private val firstPull = CompletableDeferred<Unit>()

    fun start() {
        scope.launch { pull() }
        scope.launch {
            firstPull.await()
            combine((SYNCED_KEYS + SIGN_IN_KEYS).map { key -> dataStore.getString(key).map { key to it } }) { it.toList() }
                .distinctUntilChanged()
                .map { canonical(it.filter { (key, _) -> key in SYNCED_KEYS }) }
                .collectLatest { current ->
                    if (current == dataStore.getString(KEY_SYNCED).first()) return@collectLatest
                    delay(SETTLE_MS)
                    upload(current)
                }
        }
    }

    /** Applies a newer settings blob from another device. Called on start and whenever the app opens. */
    suspend fun pull() {
        val (base, secret) = PlaybackSync.server() ?: return
        mutex.withLock {
            val response =
                runCatching { withContext(Dispatchers.IO) { PlaybackSync.client.get("$base/settings") { bearerAuth(secret) } } }
                    .getOrElse {
                        Logger.w(TAG, "Fetch failed: ${it.message}")
                        return
                    }
            if (response.status == HttpStatusCode.NotFound) {
                // Nothing stored yet: this device's settings may go up first.
                firstPull.complete(Unit)
                return
            }
            if (!response.status.isSuccess()) {
                Logger.w(TAG, "Fetch failed: HTTP ${response.status.value}")
                return
            }
            val envelope =
                runCatching { PlaybackSync.json.decodeFromString<SettingsEnvelope>(response.bodyAsText()) }
                    .getOrElse {
                        Logger.w(TAG, "Unreadable settings: ${it.message}")
                        return
                    }
            val syncedAt = dataStore.getString(KEY_SYNCED_AT).first()?.toLongOrNull() ?: 0L
            if (envelope.updatedAt > syncedAt) {
                val plain =
                    runCatching { open(envelope.sealed, secret) }
                        .getOrElse {
                            Logger.w(TAG, "Could not decrypt settings: ${it.message}")
                            return
                        }
                val state = PlaybackSync.json.decodeFromString(SyncedState.serializer(), plain)
                // Recorded first, so the writes below read as already in sync and are not uploaded back.
                dataStore.putString(KEY_SYNCED, plain)
                dataStore.putString(KEY_SYNCED_AT, envelope.updatedAt.toString())
                state.settings.forEach { (key, value) ->
                    val local = dataStore.getString(key).first()
                    // A blank never replaces a filled-in value: a phone that never had an API key
                    // must not wipe the one set here. Clearing a key therefore stays per phone.
                    if (key in SYNCED_KEYS && local != value && (value.isNotBlank() || local.isNullOrBlank())) {
                        dataStore.putString(key, value)
                    }
                }
                val signedIn = state.signIns?.let { runCatching { signIns.import(it) }.onFailure { e -> Logger.w(TAG, "Sign-ins not applied: ${e.message}") }.getOrNull() } ?: 0
                Logger.i(TAG, "Applied settings and $signedIn sign-in(s) from another device")
            }
            firstPull.complete(Unit)
        }
    }

    private suspend fun upload(current: String) {
        val (base, secret) = PlaybackSync.server() ?: return
        mutex.withLock {
            val updatedAt = System.currentTimeMillis()
            val envelope = SettingsEnvelope(PlaybackSync.deviceId(), updatedAt, seal(current, secret))
            val ok =
                runCatching {
                    withContext(Dispatchers.IO) {
                        PlaybackSync.client.put("$base/settings") {
                            bearerAuth(secret)
                            contentType(ContentType.Application.Json)
                            setBody(PlaybackSync.json.encodeToString(SettingsEnvelope.serializer(), envelope))
                        }
                    }.status
                        .isSuccess()
                }.getOrElse {
                    Logger.w(TAG, "Upload failed: ${it.message}")
                    false
                }
            if (ok) {
                dataStore.putString(KEY_SYNCED, current)
                dataStore.putString(KEY_SYNCED_AT, updatedAt.toString())
                Logger.i(TAG, "Uploaded settings")
            }
        }
    }

    /** Another device's translation of [videoId] into [language], if one was shared. */
    suspend fun fetchTranslation(
        videoId: String,
        language: String,
    ): TranslatedLyricsEntity? {
        val (base, secret) = PlaybackSync.server() ?: return null
        val response =
            runCatching {
                withContext(Dispatchers.IO) {
                    PlaybackSync.client.get("$base/${translationKey(videoId, language)}") {
                        bearerAuth(secret)
                        timeout { requestTimeoutMillis = TRANSLATION_FETCH_TIMEOUT_MS }
                    }
                }
            }.getOrNull() ?: return null
        if (!response.status.isSuccess()) return null
        val shared = runCatching { PlaybackSync.json.decodeFromString<SharedTranslation>(response.bodyAsText()) }.getOrNull() ?: return null
        Logger.i(TAG, "Using the translation of $videoId shared by another device")
        return TranslatedLyricsEntity(videoId = videoId, language = language, error = false, lines = shared.lines, syncType = shared.syncType)
    }

    /** Shares a translation this device just paid for. */
    fun shareTranslation(
        videoId: String,
        language: String,
        lines: List<Line>,
        syncType: String?,
    ) {
        scope.launch {
            val (base, secret) = PlaybackSync.server() ?: return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    PlaybackSync.client.put("$base/${translationKey(videoId, language)}") {
                        bearerAuth(secret)
                        contentType(ContentType.Application.Json)
                        setBody(PlaybackSync.json.encodeToString(SharedTranslation.serializer(), SharedTranslation(syncType, lines)))
                    }
                }
            }.onFailure { Logger.w(TAG, "Sharing the translation failed: ${it.message}") }
        }
    }

    /** Set values in a fixed order plus this device's sign-ins, so the same state gives the same string. */
    private suspend fun canonical(settings: List<Pair<String, String?>>): String =
        PlaybackSync.json.encodeToString(
            SyncedState.serializer(),
            SyncedState(
                settings = settings.mapNotNull { (key, value) -> value?.let { key to it } }.sortedBy { it.first }.toMap(),
                signIns = signIns.export(),
            ),
        )
}

/**
 * The server stores objects under lowercase names, while video ids are case-sensitive, so the id
 * and language travel hex-encoded: `tl_` + hex("videoId|language"), well under the 64-character limit.
 */
internal fun translationKey(
    videoId: String,
    language: String,
): String = "tl_" + "$videoId|$language".encodeToByteArray().joinToString("") { "%02x".format(it) }

/** AES-256-GCM, `nonce || ciphertext || tag` in Base64. The key is derived from the sync key. */
internal fun seal(
    plain: String,
    secret: String,
): String {
    val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, settingsKey(secret), GCMParameterSpec(128, nonce))
    return Base64.getEncoder().encodeToString(nonce + cipher.doFinal(plain.encodeToByteArray()))
}

internal fun open(
    sealed: String,
    secret: String,
): String {
    val bytes = Base64.getDecoder().decode(sealed)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, settingsKey(secret), GCMParameterSpec(128, bytes, 0, 12))
    return cipher.doFinal(bytes, 12, bytes.size - 12).decodeToString()
}

// The sync key is 24 random bytes, so a hash is enough to turn it into an AES key; the prefix keeps
// this key apart from any other use of the same secret.
private fun settingsKey(secret: String) =
    SecretKeySpec(MessageDigest.getInstance("SHA-256").digest("simpmusic-sync/v1/settings:$secret".encodeToByteArray()), "AES")
