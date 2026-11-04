package com.xgy.androidkaraoke.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode

/**
 * MV（视频）+ 原唱/伴唱（音频）播放。
 * 原唱 = 完整混音；伴唱 = 纯伴奏（非人声干声）。
 * 有独立音频轨时：视频静音播 MV，音频播选中轨；仅有 MV 时直接播 MV 音画。
 */
class KaraokePlayer(context: Context) {
    private val appContext = context.applicationContext

    private val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("AndroidKaraoke/TV")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20_000)
        .setReadTimeoutMs(45_000)

    private val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
        .setDataSourceFactory(httpFactory)

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
    val audioPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()

    var song: Song? = null
        private set
    var vocalMode: VocalMode = VocalMode.Accompaniment
        private set

    private var usingSeparateAudio: Boolean = false

    /**
     * 无外挂 LRC 时启用内嵌/软字幕；有 LRC 时关掉，避免叠两层字。
     */
    fun setPreferEmbeddedSubtitles(enabled: Boolean) {
        val params = videoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !enabled)
            .setSelectUndeterminedTextLanguage(enabled)
            .build()
        videoPlayer.trackSelectionParameters = params
        audioPlayer.trackSelectionParameters = audioPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            .build()
    }

    fun play(song: Song, mode: VocalMode = vocalMode, autoPlay: Boolean = true) {
        this.song = song
        this.vocalMode = mode
        val mv = song.mvUrl
        val audio = song.audioUrl(mode)
        val separate = !mv.isNullOrBlank() && !audio.isNullOrBlank() && audio != mv
        usingSeparateAudio = separate

        videoPlayer.stop()
        audioPlayer.stop()
        videoPlayer.clearMediaItems()
        audioPlayer.clearMediaItems()
        videoPlayer.volume = if (separate) 0f else 1f
        audioPlayer.volume = 1f

        when {
            separate -> {
                videoPlayer.setMediaItem(MediaItem.fromUri(mv!!))
                audioPlayer.setMediaItem(MediaItem.fromUri(audio!!))
                videoPlayer.prepare()
                audioPlayer.prepare()
                if (autoPlay) {
                    videoPlayer.playWhenReady = true
                    audioPlayer.playWhenReady = true
                }
            }
            !mv.isNullOrBlank() -> {
                videoPlayer.setMediaItem(MediaItem.fromUri(mv))
                videoPlayer.prepare()
                if (autoPlay) videoPlayer.playWhenReady = true
            }
            !audio.isNullOrBlank() -> {
                // 无 MV：用视频播放器播音频也行，但无画面
                audioPlayer.setMediaItem(MediaItem.fromUri(audio))
                audioPlayer.prepare()
                if (autoPlay) audioPlayer.playWhenReady = true
            }
        }
    }

    fun switchVocalMode(mode: VocalMode) {
        val current = song ?: return
        if (mode == vocalMode) return
        val position = currentPositionMs()
        val wasPlaying = isPlaying()
        play(current, mode, autoPlay = wasPlaying)
        seekTo(position)
    }

    fun togglePlayPause() {
        if (isPlaying()) pause() else resume()
    }

    fun pause() {
        videoPlayer.pause()
        audioPlayer.pause()
    }

    fun resume() {
        if (usingSeparateAudio || song?.hasMv == true) {
            videoPlayer.play()
        }
        if (usingSeparateAudio || song?.hasMv != true) {
            audioPlayer.play()
        }
        if (!usingSeparateAudio && song?.hasMv == true) {
            videoPlayer.play()
        }
    }

    fun isPlaying(): Boolean =
        videoPlayer.isPlaying || audioPlayer.isPlaying ||
            videoPlayer.playWhenReady || audioPlayer.playWhenReady

    fun currentPositionMs(): Long =
        when {
            usingSeparateAudio -> audioPlayer.currentPosition.coerceAtLeast(videoPlayer.currentPosition)
            song?.hasMv == true -> videoPlayer.currentPosition
            else -> audioPlayer.currentPosition
        }

    fun durationMs(): Long {
        val d = when {
            usingSeparateAudio -> maxOf(audioPlayer.duration, videoPlayer.duration)
            song?.hasMv == true -> videoPlayer.duration
            else -> audioPlayer.duration
        }
        return if (d > 0) d else (song?.durationSec ?: 0) * 1000
    }

    fun seekTo(ms: Long) {
        if (usingSeparateAudio || song?.hasMv == true) videoPlayer.seekTo(ms)
        if (usingSeparateAudio || song?.hasMv != true) audioPlayer.seekTo(ms)
        if (!usingSeparateAudio && song?.hasMv == true) videoPlayer.seekTo(ms)
    }

    fun skipForward(ms: Long = 30_000) {
        seekTo((currentPositionMs() + ms).coerceAtMost(durationMs().coerceAtLeast(0)))
    }

    fun addListener(listener: Player.Listener) {
        videoPlayer.addListener(listener)
        audioPlayer.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        videoPlayer.removeListener(listener)
        audioPlayer.removeListener(listener)
    }

    fun release() {
        videoPlayer.release()
        audioPlayer.release()
    }
}
