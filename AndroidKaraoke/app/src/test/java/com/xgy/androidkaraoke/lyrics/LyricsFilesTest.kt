package com.xgy.androidkaraoke.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsFilesTest {
    @Test
    fun prefersLyricsDotLrcThenTitleArtist() {
        val names = listOf("mv.mp4", "original.m4a", "丑八怪-薛之谦.lrc", "readme.txt")
        val pick = LyricsFiles.pickLyricsName(
            names,
            LyricsFiles.preferredNames("丑八怪", "薛之谦", "丑八怪-薛之谦"),
        )
        assertEquals("丑八怪-薛之谦.lrc", pick)

        val withCanonical = names + "lyrics.lrc"
        assertEquals(
            "lyrics.lrc",
            LyricsFiles.pickLyricsName(
                withCanonical,
                LyricsFiles.preferredNames("丑八怪", "薛之谦", "pack"),
            ),
        )
    }

    @Test
    fun returnsNullWhenNoLrc() {
        assertNull(LyricsFiles.pickLyricsName(listOf("mv.mp4", "original.m4a"), listOf("lyrics.lrc")))
    }
}
