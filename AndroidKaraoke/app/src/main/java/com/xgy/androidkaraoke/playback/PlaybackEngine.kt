package com.xgy.androidkaraoke.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode
import okhttp3.OkHttpClient
import kotlin.math.abs

class PlaybackEngine(
    context: Context,
    http: OkHttpClient,
) {
    private val mediaSourceFactory = DefaultMediaSourceFactory(OkHttpDataSource.Factory(http))

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()

    private val audioPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()

    var onEnded: () -> Unit = {}
    var onError: (String) -> Unit = {}

    private var externalAudio = false
    private var ended = false

    init {
        videoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) finishIfCurrent(fromVideo = true)
            }

            override fun onPlayerError(error: PlaybackException) {
                onError("播放失败，请检查 NAS 是否在线")
            }
        })
        audioPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) finishIfCurrent(fromVideo = false)
            }

            override fun onPlayerError(error: PlaybackException) {
                onError("音源播放失败，请检查 NAS 是否在线")
            }
        })
    }

    private fun finishIfCurrent(fromVideo: Boolean) {
        if (ended) return
        val videoActive = videoPlayer.mediaItemCount > 0
        if (fromVideo != videoActive) return
        ended = true
        onEnded()
    }

    fun play(song: Song, vocal: VocalMode) {
        ended = false
        val audioUrl = audioUrlFor(song, vocal)
        val mv = song.mvUrl
        if (mv.isNullOrBlank() && audioUrl.isNullOrBlank()) {
            onError("这首没有可播放的 MV 或音源")
            return
        }
        videoPlayer.stop()
        audioPlayer.stop()
        videoPlayer.clearMediaItems()
        audioPlayer.clearMediaItems()
        externalAudio = !audioUrl.isNullOrBlank()
        if (!mv.isNullOrBlank()) {
            videoPlayer.volume = if (externalAudio) 0f else 1f
            videoPlayer.setMediaItem(MediaItem.fromUri(mv))
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
        }
        if (externalAudio) {
            audioPlayer.volume = 1f
            audioPlayer.setMediaItem(MediaItem.fromUri(audioUrl!!))
            audioPlayer.prepare()
            audioPlayer.playWhenReady = true
        }
    }

    fun setVocal(song: Song, vocal: VocalMode): Boolean {
        val audioUrl = when (vocal) {
            VocalMode.Accompaniment -> song.accompanyUrl
            VocalMode.Original -> song.originalUrl
        }
        if (audioUrl.isNullOrBlank()) {
            if (vocal == VocalMode.Original && !song.mvUrl.isNullOrBlank()) {
                externalAudio = false
                audioPlayer.playWhenReady = false
                videoPlayer.volume = 1f
                return true
            }
            return false
        }
        val position = currentPosition()
        externalAudio = true
        videoPlayer.volume = 0f
        audioPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
        audioPlayer.prepare()
        audioPlayer.seekTo(position)
        audioPlayer.playWhenReady = isPlaying()
        return true
    }

    fun togglePause() {
        val play = !isPlaying()
        if (videoPlayer.mediaItemCount > 0) videoPlayer.playWhenReady = play
        if (externalAudio) audioPlayer.playWhenReady = play
    }

    fun seekTo(positionMs: Long) {
        if (videoPlayer.mediaItemCount > 0) videoPlayer.seekTo(positionMs)
        if (externalAudio) audioPlayer.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val duration = durationMs()
        val target = (currentPosition() + deltaMs).coerceAtLeast(0L)
        seekTo(if (duration > 0) target.coerceAtMost((duration - 500).coerceAtLeast(0)) else target)
    }

    fun sync() {
        if (!externalAudio || videoPlayer.mediaItemCount == 0 || audioPlayer.mediaItemCount == 0) return
        if (abs(videoPlayer.currentPosition - audioPlayer.currentPosition) > 400) {
            audioPlayer.seekTo(videoPlayer.currentPosition)
        }
    }

    fun currentPosition(): Long =
        if (videoPlayer.mediaItemCount > 0) videoPlayer.currentPosition else audioPlayer.currentPosition

    fun durationMs(): Long {
        val video = videoPlayer.duration
        val audio = audioPlayer.duration
        return when {
            video > 0 -> video
            audio > 0 -> audio
            else -> 0L
        }
    }

    fun isPlaying(): Boolean = videoPlayer.playWhenReady || (externalAudio && audioPlayer.playWhenReady)

    fun stop() {
        ended = true
        videoPlayer.stop()
        audioPlayer.stop()
        videoPlayer.clearMediaItems()
        audioPlayer.clearMediaItems()
    }

    fun release() {
        videoPlayer.release()
        audioPlayer.release()
    }

    private fun audioUrlFor(song: Song, vocal: VocalMode): String? = when (vocal) {
        VocalMode.Accompaniment -> song.accompanyUrl ?: song.originalUrl
        VocalMode.Original -> song.originalUrl ?: song.mvUrl?.takeIf { song.accompanyUrl == null }
    }
}
