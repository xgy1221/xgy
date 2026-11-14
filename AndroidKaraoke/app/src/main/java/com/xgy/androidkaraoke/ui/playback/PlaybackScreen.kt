package com.xgy.androidkaraoke.ui.playback

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
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
import androidx.compose.ui.text.style.TextOverflow
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
    var chromeVisible by remember { mutableStateOf(true) }
    var idleTick by remember { mutableIntStateOf(0) }
    var menuIndex by remember { mutableIntStateOf(PAUSE_MENU_INDEX) }
    val menuActions = listOf<() -> Unit>(
        { vm.skipPrelude() },
        { vm.cycleOrder() },
        { vm.replay() },
        { vm.togglePlayPause() },
        { vm.toggleVocal() },
        { vm.nextTrack() },
        { vm.toggleFavorite() },
    )
    LaunchedEffect(chromeVisible) {
        delay(48)
        runCatching { playbackKeys.requestFocus() }
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
            .focusProperties { canFocus = true }
            .focusable()
            .onPreviewKeyEvent { event ->
                handlePlaybackKey(
                    vm = vm,
                    event = event,
                    chromeVisible = chromeVisible,
                    onReveal = {
                        menuIndex = PAUSE_MENU_INDEX
                        chromeVisible = true
                        idleTick++
                    },
                    onInteract = { idleTick++ },
                    onLeft = { menuIndex = (menuIndex - 1).coerceAtLeast(0) },
                    onRight = { menuIndex = (menuIndex + 1).coerceAtMost(menuActions.lastIndex) },
                    onActivate = { menuActions[menuIndex].invoke() },
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
                    subtitleView?.visibility = android.view.View.GONE
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

        if (song?.hasMv != true && vm.durationSec <= 0) {
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
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KtvTicker(songTitle = song?.displayTitle, songArtist = song?.artist, next = vm.nextSong, waiting = vm.queue.size)
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
                modifier = Modifier.padding(top = 8.dp),
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
                ControlBtn("跳过前奏", Icons.Outlined.FastForward, armed = menuIndex == 0) { vm.skipPrelude() }
                ControlBtn("顺序", Icons.Outlined.Shuffle, armed = menuIndex == 1) { vm.cycleOrder() }
                ControlBtn("重唱", Icons.Outlined.Replay, armed = menuIndex == 2) { vm.replay() }
                ControlBtn(
                    if (vm.isPlaying) "暂停" else "播放",
                    if (vm.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    prominent = true,
                    armed = menuIndex == PAUSE_MENU_INDEX,
                ) { vm.togglePlayPause() }
                ControlBtn(
                    when {
                        song?.canToggleVocal != true && song?.hasAccompaniment == true -> "仅伴奏"
                        song?.canToggleVocal != true && song?.hasOriginal == true -> "仅原曲"
                        vm.vocalMode == VocalMode.Accompaniment -> "伴唱"
                        else -> "原唱"
                    },
                    Icons.Outlined.RecordVoiceOver,
                    armed = menuIndex == 4,
                ) { vm.toggleVocal() }
                ControlBtn("切歌", Icons.Outlined.SkipNext, armed = menuIndex == 5) { vm.nextTrack() }
                ControlBtn(
                    "收藏",
                    if (vm.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    armed = menuIndex == 6,
                ) { vm.toggleFavorite() }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "左右选择    确定执行    5 秒后收起。收起后左后退、右快进，上或下再打开",
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
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onActivate: () -> Unit,
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
        if (!down) return true
        when (code) {
            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> vm.seekBy(-10_000)
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> vm.seekBy(10_000)
            AndroidKeyEvent.KEYCODE_DPAD_UP,
            AndroidKeyEvent.KEYCODE_DPAD_DOWN,
            -> onReveal()
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_ENTER,
            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
            AndroidKeyEvent.KEYCODE_BUTTON_SELECT,
            AndroidKeyEvent.KEYCODE_BUTTON_A,
            -> vm.togglePlayPause()
            else -> if (media) performMediaKey(vm, code)
        }
        return true
    }
    if (!down) return true
    onInteract()
    when (code) {
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> onLeft()
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> onRight()
        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
        AndroidKeyEvent.KEYCODE_BUTTON_SELECT,
        AndroidKeyEvent.KEYCODE_BUTTON_A,
        -> onActivate()
        AndroidKeyEvent.KEYCODE_DPAD_UP,
        AndroidKeyEvent.KEYCODE_DPAD_DOWN,
        -> Unit
        else -> if (media) performMediaKey(vm, code)
    }
    return true
}

private const val PAUSE_MENU_INDEX = 3

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun KtvTicker(songTitle: String?, songArtist: String?, next: com.xgy.androidkaraoke.model.Song?, waiting: Int) {
    val line = buildString {
        append("正在演唱：")
        append(songTitle ?: "暂无")
        if (!songArtist.isNullOrBlank()) append(" · ").append(songArtist)
        append("　　下一首：")
        if (next != null) {
            append(next.displayTitle)
            if (next.artist.isNotBlank()) append(" · ").append(next.artist)
        } else {
            append("暂无")
        }
        if (waiting > 0) append("　　已点 ").append(waiting).append(" 首")
        append("　　　")
    }
    val scrolling = remember(line) { line.repeat(4) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = scrolling,
            color = Color(0xFFFFE08A),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                initialDelayMillis = 400,
                velocity = 46.dp,
            ),
        )
    }
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
    armed: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        corner = if (prominent) 28.dp else 14.dp,
        highlighted = armed,
        focusable = false,
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
