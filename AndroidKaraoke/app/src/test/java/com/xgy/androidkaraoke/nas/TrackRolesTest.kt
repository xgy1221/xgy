package com.xgy.androidkaraoke.nas

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackRolesTest {
    @Test
    fun originalIsFullMixNotDryVocals() {
        assertEquals(TrackRole.Original, TrackRoles.classify("original.m4a"))
        assertEquals(TrackRole.Original, TrackRoles.classify("原唱.mp3"))
        assertEquals(TrackRole.Original, TrackRoles.classify("full_mix.wav"))
    }

    @Test
    fun demucsVocalsAreIgnored() {
        assertEquals(TrackRole.Ignore, TrackRoles.classify("vocals.wav"))
        assertEquals(TrackRole.Ignore, TrackRoles.classify("vocals.m4a"))
        assertEquals(TrackRole.Ignore, TrackRoles.classify("stem-vocal.wav"))
        assertEquals(TrackRole.Ignore, TrackRoles.classify("人声干声.wav"))
    }

    @Test
    fun accompanimentAndMv() {
        assertEquals(TrackRole.Accompaniment, TrackRoles.classify("accompaniment.m4a"))
        assertEquals(TrackRole.Accompaniment, TrackRoles.classify("no_vocals.wav"))
        assertEquals(TrackRole.Accompaniment, TrackRoles.classify("伴奏.mp3"))
        assertEquals(TrackRole.Mv, TrackRoles.classify("mv.mp4"))
        assertEquals(TrackRole.Lyrics, TrackRoles.classify("lyrics.lrc"))
        assertEquals(TrackRole.Ignore, TrackRoles.classify("source.mp4"))
    }

    @Test
    fun prefersM4aOverWav() {
        val wav = "https://nas/original.wav"
        val m4a = "https://nas/original.m4a"
        assertEquals(m4a, TrackRoles.pickPreferred(wav, "original.wav", m4a, "original.m4a"))
        assertEquals(m4a, TrackRoles.pickPreferred(m4a, "original.m4a", wav, "original.wav"))
    }
}
