package com.xgy.androidkaraoke.recommend

import com.xgy.androidkaraoke.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private fun song(
        id: String,
        artist: String,
        genres: List<String>,
        hot: Int = 10,
    ) = Song(
        id = id,
        title = id,
        artist = artist,
        genres = genres,
        hotScore = hot,
    )

    @Test
    fun prefersSameArtistAndGenreOverHotOnly() {
        val candidates = listOf(
            song("hot", "陌生人", listOf("电子"), hot = 99),
            song("same-artist", "刀郎", listOf("摇滚"), hot = 20),
            song("same-genre", "呼斯楞", listOf("民谣", "西部"), hot = 30),
            song("both", "刀郎", listOf("民谣", "西部"), hot = 25),
        )
        val ranked = RecommendationEngine.rank(
            candidates = candidates,
            artistWeights = mapOf("刀郎" to 5f),
            genreWeights = mapOf("民谣" to 4f, "西部" to 3f),
            nowPlayingId = "hot",
            limit = 4,
        )
        assertEquals("both", ranked.first().id)
        assertTrue(ranked.indexOfFirst { it.id == "same-artist" } < ranked.indexOfFirst { it.id == "hot" })
        assertTrue(ranked.indexOfFirst { it.id == "same-genre" } < ranked.indexOfFirst { it.id == "hot" })
    }

    @Test
    fun nowPlayingIsNotPrimarySignal() {
        val playing = song("playing", "周杰伦", listOf("流行"), hot = 50)
        val habitual = song("habit", "刀郎", listOf("民谣"), hot = 20)
        val ranked = RecommendationEngine.rank(
            candidates = listOf(playing, habitual),
            artistWeights = mapOf("刀郎" to 6f),
            genreWeights = mapOf("民谣" to 5f),
            nowPlayingId = playing.id,
            limit = 2,
        )
        assertEquals("habit", ranked.first().id)
    }
}
