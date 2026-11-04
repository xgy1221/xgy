package com.xgy.androidkaraoke.nas

import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

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

    @Test
    fun extractDownloadPathDecodesChineseSharePath() {
        val path = "/music/丑八怪-薛之谦.mp4"
        val enc = URLEncoder.encode(path, "UTF-8")
        val url = "https://nas.example:9527/webapi/entry.cgi?api=SYNO.FileStation.Download" +
            "&version=2&method=download&mode=open&path=$enc&_sid=abc123"
        assertEquals(path, SynologyFileStation.extractDownloadPath(url))
        assertTrue(SynologyFileStation.isFileStationDownloadUrl(url))
        assertNull(SynologyFileStation.extractDownloadPath("https://example/file.mp4"))
    }

    @Test
    fun flatMp4DoesNotAdvertiseDualTracks() {
        val song = Song(
            id = "/music/丑八怪-薛之谦.mp4",
            title = "丑八怪",
            artist = "薛之谦",
            source = MediaSource.Nas,
            relativePath = "/music/丑八怪-薛之谦.mp4",
            mvUrl = "https://nas/x.mp4",
            originalUrl = null,
            accompanimentUrl = null,
        )
        assertTrue(song.hasMv)
        assertFalse(song.hasOriginal)
        assertFalse(song.hasAccompaniment)
    }

    @Test
    fun sameUrlOnAllTracksDoesNotCountAsDual() {
        val url = "https://nas/same.mp4"
        val song = Song(
            id = "1",
            title = "x",
            artist = "y",
            mvUrl = url,
            originalUrl = url,
            accompanimentUrl = url,
        )
        assertTrue(song.hasMv)
        assertFalse(song.hasOriginal)
        assertFalse(song.hasAccompaniment)
    }
}
