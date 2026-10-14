package com.maxrave.simpmusic.playbacksync

import com.maxrave.domain.data.model.browse.album.Track
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSyncTest {
    @Test
    fun windowKeepsTheCurrentTrackInPlace() {
        val queue = (0 until 1000).toList()
        assertEquals(queue.subList(450, 801) to 50, windowQueue(queue, 500))
        assertEquals(queue.subList(0, 304) to 3, windowQueue(queue, 3))
        assertEquals(queue.subList(949, 1000) to 50, windowQueue(queue, 999))
        assertEquals(listOf(7) to 0, windowQueue(listOf(7), 0))
    }

    @Test
    fun snapshotSurvivesJson() {
        val track =
            Track(
                album = null,
                artists = null,
                duration = "3:30",
                durationSeconds = 210,
                isAvailable = true,
                isExplicit = false,
                likeStatus = null,
                thumbnails = null,
                title = "Song",
                videoId = "abc_123",
                videoType = null,
                category = null,
                feedbackTokens = null,
                resultType = null,
            )
        val snapshot = PlaybackSnapshot("dev", "Phone", 1L, listOf(track), 0, 61_000L, "RDAMVMabc_123", "Radio", "RADIO", "cont")
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString<PlaybackSnapshot>(json.encodeToString(PlaybackSnapshot.serializer(), snapshot))
        assertEquals(snapshot, decoded)
        assertEquals("abc_123", decoded.track?.videoId)
    }
}
