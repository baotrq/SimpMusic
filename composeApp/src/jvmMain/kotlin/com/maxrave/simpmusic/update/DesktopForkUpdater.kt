package com.maxrave.simpmusic.update

import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.ForkBuildCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.fork_update_check_failed
import simpmusic.composeapp.generated.resources.fork_update_checking
import simpmusic.composeapp.generated.resources.fork_update_downloading
import simpmusic.composeapp.generated.resources.fork_update_failed
import simpmusic.composeapp.generated.resources.fork_update_found
import simpmusic.composeapp.generated.resources.fork_update_mac_ready
import simpmusic.composeapp.generated.resources.fork_update_none
import simpmusic.composeapp.generated.resources.fork_update_windows_ready
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Fork: the Mac and Windows update check, the Desktop twin of androidApp's ForkUpdater. It reads the
 * download site's manifest (scripts/download-site), or the showcase repo's releases when the site cannot
 * answer, and offers a release whose version is newer than this build's. It looks 10 s after launch and
 * then every 6 hours, since a desktop app can stay open for days; Settings → About asks any time.
 *
 * Update downloads the file and opens it: on a Mac the DMG, from which the user drags the app over the
 * old one; on Windows the zip's install.bat, which closes the app, installs and reopens it. Nothing is
 * replaced behind the user's back.
 */
object DesktopForkUpdater {
    private const val TAG = "ForkUpdater"
    private const val SITE = "https://nhacnheo.pages.dev"
    private const val GITHUB_RELEASES = "https://api.github.com/repos/baotrq/simpmusic-bao-showcase/releases?per_page=10"
    private const val TAG_PREFIX = "bao-"
    private const val FIRST_CHECK_DELAY_MS = 10_000L
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    private val VERSION_IN_NAME = Regex("""\d+(?:\.\d+)+""")

    private class Release(
        val version: String,
        val title: String,
        val url: String,
        val fileName: String,
        val size: Long,
    )

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, e -> Logger.w(TAG, "Update task failed: ${e.message}") },
        )
    private val json = Json { ignoreUnknownKeys = true }
    private var currentVersion = ""

    /** The site's name for this machine's file: "mac-arm", "mac-intel" or "windows"; null elsewhere. */
    private val platform: String? by lazy { platformKey() }

    @Volatile
    private var found: Release? = null

    /** Called once at startup. Dev runs and Linux keep the upstream check. */
    fun start(
        currentVersion: String,
        isDevBuild: Boolean,
    ) {
        if (isDevBuild || platform == null) return
        this.currentVersion = currentVersion
        ForkBuildCheck.check = { scope.launch { checkFromSettings() } }
        ForkBuildCheck.install = { scope.launch { install() } }
        scope.launch {
            delay(FIRST_CHECK_DELAY_MS)
            while (true) {
                try {
                    publish(newerRelease())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Quiet: offline or a site hiccup is not worth a message; the next round asks again.
                    Logger.w(TAG, "Check failed: ${e.message}")
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun publish(release: Release?) {
        // A line about an older release's download must not stay under a newer one.
        if (release?.version != found?.version && !ForkBuildCheck.downloading.value) ForkBuildCheck.status.value = null
        found = release
        ForkBuildCheck.available.value = release?.let { ForkBuildCheck.AvailableBuild(it.version, it.title) }
        if (release != null) Logger.i(TAG, "Offering ${release.version} for $platform")
    }

    /** Asked for from Settings, so every outcome is answered, "nothing newer" included. */
    private suspend fun checkFromSettings() {
        toast(getString(Res.string.fork_update_checking))
        try {
            val release = newerRelease()
            publish(release)
            toast(
                if (release == null) {
                    getString(Res.string.fork_update_none)
                } else {
                    getString(Res.string.fork_update_found, release.version)
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "Check failed: ${e.message}")
            toast(getString(Res.string.fork_update_check_failed, e.message ?: e::class.simpleName.orEmpty()))
        }
    }

    /** The newest release for this machine, if newer than this build. GitHub only answers when the site cannot. */
    private fun newerRelease(): Release? =
        try {
            newerOnSite()
        } catch (e: Exception) {
            Logger.w(TAG, "Site check failed, trying GitHub: ${e.message}")
            newerOnGitHub()
        }

    private fun newerOnSite(): Release? {
        val manifest = json.parseToJsonElement(fetchText("$SITE/latest.json")).jsonObject
        val file = manifest["files"]?.jsonObject?.get(platform)?.jsonObject ?: return null
        val version = file.text("version")
        if (!isNewer(version, currentVersion)) return null
        // The release title is written for Android; it describes this file only when both are the same version.
        val title =
            manifest["android"]?.jsonObject?.takeIf { it.text("version") == version }?.text("title")
                ?: "SimpMusic $version"
        return Release(
            version = version,
            title = title,
            url = "$SITE/f/${file.text("key")}",
            fileName = file.text("name"),
            size = file.getValue("size").jsonPrimitive.long,
        )
    }

    private fun newerOnGitHub(): Release? {
        val suffix =
            when (platform) {
                "mac-arm" -> "-mac-aarch64.dmg"
                "mac-intel" -> "-mac-amd64.dmg"
                // The Windows zip's name carries no version, so its GitHub copy cannot be compared.
                else -> return null
            }
        val releases = json.parseToJsonElement(fetchText(GITHUB_RELEASES)).jsonArray
        // Newest first, so the first release carrying this machine's file decides.
        for (element in releases) {
            val release = element.jsonObject
            if (release["draft"]?.jsonPrimitive?.content == "true") continue
            if (!release.text("tag_name").startsWith(TAG_PREFIX)) continue
            val asset =
                release.getValue("assets").jsonArray
                    .map { it.jsonObject }
                    .firstOrNull { it.text("name").endsWith(suffix) } ?: continue
            val name = asset.text("name")
            val version = VERSION_IN_NAME.find(name)?.value ?: continue
            if (!isNewer(version, currentVersion)) return null
            return Release(
                version = version,
                title = release.text("name").ifBlank { "SimpMusic $version" },
                url = asset.text("browser_download_url"),
                fileName = name,
                size = asset.getValue("size").jsonPrimitive.long,
            )
        }
        return null
    }

    private suspend fun install() {
        val release = found ?: return
        if (!ForkBuildCheck.downloading.compareAndSet(expect = false, update = true)) return
        try {
            val file = download(release)
            if (platform == "windows") {
                val bat = unzip(file).walkTopDown().first { it.name.equals("install.bat", ignoreCase = true) }
                // `start` gives the script a console window of its own; it asks for admin rights itself.
                ProcessBuilder("cmd.exe", "/c", "start", "", bat.absolutePath).directory(bat.parentFile).start()
                ForkBuildCheck.status.value = getString(Res.string.fork_update_windows_ready)
            } else {
                ProcessBuilder("open", file.absolutePath).start()
                ForkBuildCheck.status.value = getString(Res.string.fork_update_mac_ready)
            }
            Logger.i(TAG, "Opened the installer for ${release.version}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "Update failed: ${e.message}")
            ForkBuildCheck.status.value = getString(Res.string.fork_update_failed, e.message ?: e::class.simpleName.orEmpty())
        } finally {
            ForkBuildCheck.downloading.value = false
        }
    }

    /** Into Downloads on a Mac, where the user expects a DMG; into a temp folder on Windows. Reused if already complete. */
    private suspend fun download(release: Release): File {
        val folder =
            if (platform == "windows") {
                File(System.getProperty("java.io.tmpdir"), "nhacnheo-update")
            } else {
                File(System.getProperty("user.home"), "Downloads")
            }
        folder.mkdirs()
        val target = File(folder, release.fileName)
        if (target.isFile && target.length() == release.size) return target
        val part = File(folder, "${release.fileName}.part")
        val connection = open(release.url)
        try {
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    var shownPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        val percent = (written * 100 / release.size.coerceAtLeast(1)).toInt().coerceAtMost(100)
                        if (percent != shownPercent) {
                            shownPercent = percent
                            ForkBuildCheck.status.value = getString(Res.string.fork_update_downloading, "$percent%")
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        // A cut-off download must not be opened as if it were the installer.
        check(part.length() == release.size) { "got ${part.length()} of ${release.size} bytes" }
        target.delete()
        check(part.renameTo(target)) { "could not save ${target.name}" }
        return target
    }

    private fun unzip(zip: File): File {
        val dest = File(zip.parentFile, zip.nameWithoutExtension).canonicalFile
        dest.deleteRecursively()
        dest.mkdirs()
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val out = File(dest, entry.name).canonicalFile
                // An entry may not climb out of the folder ("zip slip").
                check(out.path.startsWith(dest.path + File.separator)) { "bad entry ${entry.name}" }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile.mkdirs()
                    out.outputStream().use { input.copyTo(it) }
                }
            }
        }
        return dest
    }

    private fun platformKey(): String? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val arch = System.getProperty("os.arch").orEmpty().lowercase()
        return when {
            // An Intel build on Apple Silicon runs under Rosetta and sees x86_64; it is offered the native build.
            os.contains("mac") -> if (arch == "aarch64" || arch == "arm64" || runsUnderRosetta()) "mac-arm" else "mac-intel"
            os.contains("win") -> "windows"
            else -> null
        }
    }

    private fun runsUnderRosetta(): Boolean =
        try {
            val process = ProcessBuilder("sysctl", "-n", "sysctl.proc_translated").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText().trim() == "1"
        } catch (_: Exception) {
            false
        }

    private fun fetchText(url: String): String {
        val connection = open(url)
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            // GitHub's API refuses requests without one.
            setRequestProperty("User-Agent", "SimpMusic-Desktop/$currentVersion")
            val code = responseCode
            if (code !in 200..299) {
                disconnect()
                error("HTTP $code from ${URI(url).host}")
            }
        }

    private suspend fun toast(message: String) = withContext(Dispatchers.Main) { showToast(message) }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}

/**
 * Whether [candidate] is a newer version than [current], comparing every number ("2.5.0.11" > "2.5.0.10").
 * VersionManager's isNewerRelease ignores the fourth number on purpose (it compares against upstream's
 * tags); this fork's own releases differ in exactly that number.
 */
internal fun isNewer(
    candidate: String,
    current: String,
): Boolean {
    fun parts(version: String) =
        version.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
