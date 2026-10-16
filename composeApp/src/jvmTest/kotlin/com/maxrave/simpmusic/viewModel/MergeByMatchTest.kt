package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import kotlin.test.Test
import kotlin.test.assertEquals

class MergeByMatchTest {
    private fun song(
        id: String,
        title: String,
        artist: String,
    ) = SongsResult(null, listOf(Artist(null, artist)), null, null, null, null, null, null, null, title, id, null, "")

    private fun video(
        id: String,
        title: String,
        artist: String,
    ) = VideosResult(listOf(Artist(null, artist)), null, null, null, null, null, title, id, null, null, "")

    @Test
    fun videoOnlyRemixMatchingMoreWordsComesFirst() {
        val merged =
            mergeByMatch(
                "nước mắt em lau bằng tình yêu mới cm1x",
                songs = listOf(song("s1", "Nước Mắt Em Lau Bằng Tình Yêu Mới", "Da LAB")),
                videos = listOf(video("v1", "Nước Mắt Em Lau Bằng Tình Yêu Mới (CM1X Remix)", "CM1X")),
            )
        assertEquals(listOf("v1", "s1"), merged.map { (it as? VideosResult)?.videoId ?: (it as SongsResult).videoId })
    }

    @Test
    fun tiesInterleaveSongBeforeVideoInYouTubeOrder() {
        val merged =
            mergeByMatch(
                "abc",
                songs = listOf(song("s1", "x", "y"), song("s2", "x", "y")),
                videos = listOf(video("v1", "x", "y"), video("v2", "x", "y")),
            )
        assertEquals(listOf("s1", "v1", "s2", "v2"), merged.map { (it as? VideosResult)?.videoId ?: (it as SongsResult).videoId })
    }

    @Test
    fun accentsAndDAreIgnored() {
        val merged =
            mergeByMatch(
                "con mua cuoi",
                songs = listOf(song("s1", "Other", "z")),
                videos = listOf(video("v1", "Cơn Mưa Cuối", "z")),
            )
        assertEquals("v1", (merged.first() as VideosResult).videoId)
    }
}
