package com.xgy.androidkaraoke.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesStandardLinesAndMeta() {
        val doc = LrcParser.parse(
            """
            [ti:丑八怪]
            [ar:薛之谦]
            [00:12.00]丑八怪 唔
            [00:18.50]别化妆了
            [01:02.00]你是我的
            """.trimIndent(),
        )
        assertEquals("丑八怪", doc.title)
        assertEquals("薛之谦", doc.artist)
        assertEquals(3, doc.lines.size)
        assertEquals(12_000L, doc.lines[0].timeMs)
        assertEquals("别化妆了", doc.lines[1].text)
        assertEquals(0, doc.lineIndexAt(12_000))
        assertEquals(1, doc.lineIndexAt(18_500))
        assertEquals(-1, doc.lineIndexAt(1_000))
        assertTrue(doc.lineProgressAt(15_000) in 0.01f..0.99f)
    }

    @Test
    fun parsesEnhancedWordTags() {
        val doc = LrcParser.parse("[00:10.00]<00:10.00>你<00:10.50>好<00:11.00>啊")
        assertEquals(1, doc.lines.size)
        assertEquals("你好啊", doc.lines[0].text)
        assertEquals(3, doc.lines[0].words.size)
        assertTrue(doc.lineProgressAt(10_250) in 0.05f..0.45f)
    }

    @Test
    fun emptyOrGarbageYieldsEmptyDoc() {
        assertTrue(LrcParser.parse("").isEmpty)
        assertTrue(LrcParser.parse("not lyrics").isEmpty)
        assertTrue(LrcParser.parse("[ti:only meta]").isEmpty)
    }
}
