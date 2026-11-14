package com.xgy.androidkaraoke.ui.playback

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.KaraokeApp
import kotlinx.coroutines.delay
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaybackScreen(vm: AppViewModel) {
    val song = vm.nowPlaying
    val app = LocalContext.current.applicationContext as KaraokeApp
    val player = remember { app.container.player }
    val playbackKeys = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }
    var chromeVisible by remember { mutableStateOf(true) }
    var idleTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            delay(48)
            runCatching { menuFocus.requestFocus() }
        } else {
            runCatching { playbackKeys.requestFocus() }
        }
    }
    LaunchedEffect(idleTick, chromeVisible) {
        if (!chromeVisible) return@LaunchedEffect
        delay(5_000)
        chromeVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playbackKeys)
            .focusProperties { canFocus = !chromeVisible }
            .focusable()
            .onPreviewKeyEvent { event ->
                handlePlaybackKey(
                    vm = vm,
                    event = event,
                    chromeVisible = chromeVisible,
                    onReveal = {
                        chromeVisible = true
                        idleTick++
                    },
                    onInteract = { idleTick++ },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player.videoPlayer
                }
            },
            update = { view ->
                if (view.player !== player.videoPlayer) {
                    view.player = player.videoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
        )

        DisposableEffect(Unit) {
            onDispose {
                // 离开全屏后由首页迷你 PlayerView 重新挂上同一 ExoPlayer
            }
        }

        if (song?.hasMv != true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background((song?.coverColor ?: Color(0xFF334455)).copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = song?.displayTitle ?: "准备播放",
                    color = KaraokeColors.textPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "正在播放：${song?.displayTitle ?: "—"}    下一首：${vm.nextSong?.title ?: "—"}",
                color = KaraokeColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = buildString {
                    append(if (song?.hasMv == true) "MV" else "无MV")
                    append(" · ")
                    append(
                        if (vm.vocalMode == VocalMode.Accompaniment) "伴唱（纯伴奏）"
                        else "原唱（完整混音）",
                    )
                    when {
                        song?.canToggleVocal == true -> append(" · 可切换")
                        song?.hasAccompaniment == true -> append(" · 仅伴奏")
                        song?.hasOriginal == true -> append(" · 仅原曲")
                    }
                    if (song?.hasLyrics == true || vm.lyricsDoc != null) append(" · 有歌词")
                },
                color = KaraokeColors.textSecondary,
                fontSize = 15.sp,
            )
            vm.playbackError?.let { err ->
                Text(
                    text = err,
                    color = KaraokeColors.danger,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // 同步歌词 / 明确空态（不放假词）
        val lyricsDoc = vm.lyricsDoc
        when {
            lyricsDoc != null && !lyricsDoc.isEmpty -> {
                LyricsOverlay(
                    doc = lyricsDoc,
                    currentIndex = vm.lyricLineIndex,
                    lineProgress = vm.lyricLineProgress,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(bottom = 120.dp),
                )
            }
            else -> {
                LyricsEmptyHint(
                    loading = vm.lyricsLoading,
                    missing = vm.lyricsMissing,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 120.dp),
                )
            }
        }

        if (chromeVisible) Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 48.dp, vertical = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(vm.formatTime(vm.currentTimeSec), color = KaraokeColors.textSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(vm.progress)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(2.dp)),
                    )
                }
                Text(vm.formatTime(vm.durationSec), color = KaraokeColors.textSecondary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlBtn("后退", Icons.Outlined.Replay10) { vm.seekBy(-10_000) }
                ControlBtn("快进", Icons.Outlined.Forward10) { vm.seekBy(10_000) }
                ControlBtn("跳过前奏", Icons.Outlined.FastForward) { vm.skipPrelude() }
                ControlBtn("顺序", Icons.Outlined.Shuffle) { vm.cycleOrder() }
                ControlBtn("重唱", Icons.Outlined.Replay) { vm.replay() }
                ControlBtn(
                    if (vm.isPlaying) "暂停" else "播放",
                    if (vm.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    prominent = true,
                    focusRequester = menuFocus,
                ) { vm.togglePlayPause() }
                ControlBtn(
                    when {
                        song?.canToggleVocal != true && song?.hasAccompaniment == true -> "仅伴奏"
                        song?.canToggleVocal != true && song?.hasOriginal == true -> "仅原曲"
                        vm.vocalMode == VocalMode.Accompaniment -> "伴唱"
                        else -> "原唱"
                    },
                    Icons.Outlined.RecordVoiceOver,
                ) { vm.toggleVocal() }
                ControlBtn("切歌", Icons.Outlined.SkipNext) { vm.nextTrack() }
                ControlBtn(
                    "收藏",
                    if (vm.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                ) { vm.toggleFavorite() }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "左右选择    确定执行    5 秒后自动隐藏",
                color = KaraokeColors.textSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun handlePlaybackKey(
    vm: AppViewModel,
    event: androidx.compose.ui.input.key.KeyEvent,
    chromeVisible: Boolean,
    onReveal: () -> Unit,
    onInteract: () -> Unit,
): Boolean {
    val native = event.nativeKeyEvent
    val code = native.keyCode
    val dpad = code == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
        code == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
        code == AndroidKeyEvent.KEYCODE_DPAD_UP ||
        code == AndroidKeyEvent.KEYCODE_DPAD_DOWN ||
        code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        code == AndroidKeyEvent.KEYCODE_ENTER ||
        code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_SELECT ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_A
    val media = code == AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_PLAY ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_PAUSE ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_NEXT ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_REWIND ||
        code == AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD
    if (!dpad && !media) return false
    val down = event.type == KeyEventType.KeyDown && native.repeatCount == 0
    if (!chromeVisible) {
        // 菜单收起时，方向键只把它叫出来，不切歌、不快进。
        if (down) {
            if (media) performMediaKey(vm, code)
            onReveal()
        }
        return true
    }
    if (down) onInteract()
    if (!media) return false
    if (down) performMediaKey(vm, code)
    return true
}

private fun performMediaKey(vm: AppViewModel, code: Int) {
    when (code) {
        AndroidKeyEvent.KEYCODE_MEDIA_REWIND,
        AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS,
        -> vm.seekBy(-10_000)
        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        -> vm.seekBy(10_000)
        AndroidKeyEvent.KEYCODE_MEDIA_NEXT,
        -> vm.nextTrack()
        else -> vm.togglePlayPause()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlBtn(
    label: String,
    icon: ImageVector,
    prominent: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        corner = if (prominent) 28.dp else 14.dp,
        focusRequester = focusRequester,
    ) { focused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (prominent) 56.dp else 44.dp)
                    .background(
                        when {
                            focused -> Color.White.copy(alpha = 0.22f)
                            prominent -> Color.White.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = KaraokeColors.textPrimary,
                    modifier = Modifier.size(if (prominent) 28.dp else 22.dp),
                )
            }
            Text(label, color = KaraokeColors.textSecondary, fontSize = 12.sp)
        }
    }
}
