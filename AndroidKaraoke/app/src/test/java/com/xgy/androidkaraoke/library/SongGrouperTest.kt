package com.xgy.androidkaraoke.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongGrouperTest {
    @Test
    fun folderGroupsMvAndTwoAudioTracks() {
        val songs = SongGrouper.group(
            listOf(
                MediaFile("周杰伦/青花瓷/青花瓷.mp4", "http://nas/mv"),
                MediaFile("周杰伦/青花瓷/青花瓷-原唱.mp3", "http://nas/ori"),
                MediaFile("周杰伦/青花瓷/青花瓷-伴唱.mp3", "http://nas/acc"),
            ),
        )
        assertEquals(1, songs.size)
        val song = songs.single()
        assertEquals("青花瓷", song.title)
        assertEquals("周杰伦", song.artist)
        assertEquals("http://nas/mv", song.mvUrl)
        assertEquals("http://nas/ori", song.originalUrl)
        assertEquals("http://nas/acc", song.accompanyUrl)
    }

    @Test
    fun flatFileUsesTitleArtistFromName() {
        val songs = SongGrouper.group(
            listOf(
                MediaFile("后来-刘若英.mkv", "http://nas/mv"),
                MediaFile("后来-刘若英.原唱.flac", "http://nas/ori"),
                MediaFile("后来-刘若英.伴奏.mp3", "http://nas/acc"),
            ),
        )
        assertEquals(1, songs.size)
        assertEquals("后来", songs[0].title)
        assertEquals("刘若英", songs[0].artist)
        assertTrue(songs[0].hasMv)
        assertTrue(songs[0].hasOriginal)
        assertTrue(songs[0].hasAccompaniment)
    }

    @Test
    fun collectionFolderBecomesPlaylist() {
        val songs = SongGrouper.group(
            listOf(
                MediaFile("欢唱金曲/周杰伦/青花瓷/青花瓷.mp4", "http://nas/mv"),
                MediaFile("欢唱金曲/周杰伦/青花瓷/青花瓷-伴唱.mp3", "http://nas/acc"),
            ),
        )
        assertEquals("欢唱金曲", songs.single().playlist)
        assertEquals("周杰伦", songs.single().artist)
        assertEquals("青花瓷", songs.single().title)
    }
}
