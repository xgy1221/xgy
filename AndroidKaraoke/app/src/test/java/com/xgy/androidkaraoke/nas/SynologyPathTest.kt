package com.xgy.androidkaraoke.nas

import org.junit.Assert.assertEquals
import org.junit.Test

class SynologyPathTest {
    @Test
    fun volumeAbsoluteMapsToSharePath() {
        assertEquals("/music", SynologyFileStation.normalizeLibraryPathForSave("/volume1/music"))
        assertEquals("/music/ktv", SynologyFileStation.normalizeLibraryPathForSave("/volume1/music/ktv"))
        assertEquals("/music", SynologyFileStation.normalizeLibraryPathForSave("/music"))
        assertEquals("/", SynologyFileStation.normalizeLibraryPathForSave("/"))
    }

    @Test
    fun parseFlatMp4FileName() {
        assertEquals("丑八怪" to "薛之谦", KaraokeFileNames.parseTitleArtist("丑八怪-薛之谦.mp4"))
        assertEquals(
            "你不是真正的快乐" to "G.E.M. 邓紫棋",
            KaraokeFileNames.parseTitleArtist("你不是真正的快乐-G.E.M. 邓紫棋.mp4"),
        )
        assertEquals("APT" to "NAS", KaraokeFileNames.parseTitleArtist("APT.mp4"))
    }
}
