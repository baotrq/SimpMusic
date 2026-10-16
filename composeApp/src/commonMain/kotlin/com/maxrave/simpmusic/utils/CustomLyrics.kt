package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.simpmusic.viewModel.getFileDir
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Lyrics the user timed themselves with tap-along, one JSON file per song. They win over every
 * provider for that song and never leave the device. A file per song rather than DataStore: the
 * settings file is rewritten every few seconds (the playback position), and lyrics would bloat it.
 */
object CustomLyrics {
    private val json = Json { ignoreUnknownKeys = true }

    private val dir: File
        get() = File(getFileDir().ifEmpty { File(System.getProperty("user.home"), ".simpmusic").path }, "custom_lyrics")

    private fun file(videoId: String) = File(dir, "$videoId.json")

    fun exists(videoId: String): Boolean = file(videoId).isFile

    fun load(videoId: String): Lyrics? =
        runCatching { json.decodeFromString<Lyrics>(file(videoId).readText()) }.getOrNull()

    /** [lines] are (start time in ms, text) in the order they are sung; untimed lyrics pass 0 and [timed] false. */
    fun save(
        videoId: String,
        lines: List<Pair<Long, String>>,
        timed: Boolean = true,
    ) {
        val lyrics =
            Lyrics(
                lines = lines.map { (start, text) -> Line(endTimeMs = "0", startTimeMs = start.toString(), syllables = listOf(), words = text) },
                syncType = if (timed) "LINE_SYNCED" else "UNSYNCED",
            )
        write(videoId, lyrics)
    }

    fun write(
        videoId: String,
        lyrics: Lyrics,
    ) {
        dir.mkdirs()
        // Write then rename, so a crash mid-write never leaves a half file that loads as nothing.
        val tmp = File(dir, "$videoId.json.tmp")
        tmp.writeText(json.encodeToString(lyrics))
        Files.move(tmp.toPath(), file(videoId).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun delete(videoId: String) {
        file(videoId).delete()
    }
}
