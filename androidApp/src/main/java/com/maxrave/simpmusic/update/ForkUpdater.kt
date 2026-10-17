package com.maxrave.simpmusic.update

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.BuildConfig
import com.maxrave.simpmusic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Fork: updates the personal build from the fork's own GitHub releases (`bao-YYYY.MM.DD.N`, published
 * by `scripts/release-bao.sh`). Run from Settings → About → Check for new build, never on its own:
 * it looks for a release whose commit is newer than this build's ([BuildConfig.BAO_COMMIT_TIME]),
 * offers it, downloads the arm64 APK and hands it to the system installer, which asks for one tap.
 * Android installs it as an update only when it is signed with the same key as the installed app, so
 * a tampered download cannot replace it.
 */
object ForkUpdater {
    private const val TAG = "ForkUpdater"
    private const val API = "https://api.github.com/repos/baotrq/SimpMusic"
    private const val TAG_PREFIX = "bao-"
    private const val ASSET_SUFFIX = "-arm64-v8a.apk"
    private class Release(
        val tag: String,
        val title: String,
        val apkUrl: String,
        val apkSize: Long,
    )

    /** False in debug builds and in vivoAndroidApp, which have no bao- releases to compare against. */
    val isAvailable: Boolean get() = !BuildConfig.DEBUG && BuildConfig.BAO_COMMIT_TIME != 0L

    /** Asked for from Settings, so every outcome is answered, "nothing newer" included. */
    fun check(activity: AppCompatActivity) {
        if (!isAvailable) return
        Toast.makeText(activity, R.string.fork_update_checking, Toast.LENGTH_SHORT).show()
        activity.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { newerRelease() } }
            result
                .onSuccess { release ->
                    if (release == null) {
                        Toast.makeText(activity, R.string.fork_update_none, Toast.LENGTH_SHORT).show()
                    } else {
                        Logger.i(TAG, "Offering ${release.tag}")
                        offer(activity, release)
                    }
                }.onFailure {
                    Logger.w(TAG, "Check failed: ${it.message}")
                    Toast.makeText(activity, activity.getString(R.string.fork_update_check_failed, it.message), Toast.LENGTH_LONG).show()
                }
        }
    }

    /** The newest bao- release, if its commit is newer than this build's. */
    private fun newerRelease(): Release? {
        val releases = JSONArray(fetchText("$API/releases?per_page=10"))
        // Newest first, so the first usable release decides.
        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val tag = release.getString("tag_name")
            if (release.optBoolean("draft") || !tag.startsWith(TAG_PREFIX)) continue
            val assets = release.getJSONArray("assets")
            val apk =
                (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(ASSET_SUFFIX) } ?: continue
            val committedAt =
                JSONObject(fetchText("$API/commits/$tag"))
                    .getJSONObject("commit")
                    .getJSONObject("committer")
                    .getString("date")
            if (Instant.parse(committedAt).epochSecond <= BuildConfig.BAO_COMMIT_TIME) return null
            return Release(
                tag = tag,
                title = release.optString("name").ifBlank { tag },
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.getLong("size"),
            )
        }
        return null
    }

    private fun offer(
        activity: AppCompatActivity,
        release: Release,
    ) {
        AlertDialog
            .Builder(activity)
            .setTitle(activity.getString(R.string.fork_update_title, release.tag))
            .setMessage(activity.getString(R.string.fork_update_message, release.title))
            .setPositiveButton(R.string.fork_update_install) { _, _ -> install(activity, release) }
            .setNegativeButton(R.string.fork_update_later, null)
            .show()
    }

    private fun install(
        activity: AppCompatActivity,
        release: Release,
    ) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, R.string.fork_update_allow_install, Toast.LENGTH_LONG).show()
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${activity.packageName}".toUri()),
            )
            // Still on screen when they come back from Settings, ready for the second tap.
            offer(activity, release)
            return
        }
        val progress =
            AlertDialog
                .Builder(activity)
                .setTitle(activity.getString(R.string.fork_update_title, release.tag))
                .setMessage(activity.getString(R.string.fork_update_downloading, 0))
                .setCancelable(false)
                .show()
        activity.lifecycleScope.launch {
            val apk = File(activity.cacheDir, "update/SimpMusic-${release.tag}.apk")
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        download(release, apk) { percent ->
                            withContext(Dispatchers.Main) {
                                progress.setMessage(activity.getString(R.string.fork_update_downloading, percent))
                            }
                        }
                    }
                }
            progress.dismiss()
            result
                .onSuccess {
                    val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.FileProvider", apk)
                    activity.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    )
                }.onFailure {
                    Logger.w(TAG, "Download failed: ${it.message}")
                    Toast.makeText(activity, activity.getString(R.string.fork_update_failed, it.message), Toast.LENGTH_LONG).show()
                }
        }
    }

    private suspend fun download(
        release: Release,
        target: File,
        onProgress: suspend (Int) -> Unit,
    ) {
        // One APK at a time: earlier downloads are of no use once a newer one is wanted.
        target.parentFile?.apply { deleteRecursively() }?.mkdirs()
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
            var done = 0L
            var shown = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        val percent = (done * 100 / release.apkSize).toInt()
                        if (percent != shown) {
                            shown = percent
                            onProgress(percent)
                        }
                    }
                }
            }
            if (done != release.apkSize) error("got $done of ${release.apkSize} bytes")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchText(url: String): String {
        val connection = open(url).apply { setRequestProperty("Accept", "application/vnd.github+json") }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode} for $url")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
}
