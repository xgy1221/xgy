package com.xgy.androidkaraoke.ui.playback

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaybackScreen(vm: AppViewModel) {
    val song = vm.nowPlaying
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (song?.hasMv == true) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player = vm.videoPlayer
                    }
                },
                update = { it.player = vm.videoPlayer },
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(song?.title ?: "没有正在播放的歌", color = KaraokeColors.textPrimary, fontSize = 40.sp)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 36.dp, vertical = 20.dp),
        ) {
            Text(
                text = "${song?.title ?: "—"}    ${song?.artist ?: ""}",
                color = KaraokeColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.formatTime(vm.currentTimeSec), color = KaraokeColors.textSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(vm.progress)
                            .height(4.dp)
                            .background(Color.White),
                    )
                }
                Text(vm.formatTime(vm.durationSec), color = KaraokeColors.textSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Control("跳过前奏", Icons.Outlined.FastForward) { vm.skipPrelude() }
                Control("重唱", Icons.Outlined.Replay) { vm.replay() }
                Control(
                    if (vm.isPlaying) "暂停" else "播放",
                    if (vm.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    prominent = true,
                ) { vm.togglePlayPause() }
                Control(
                    if (vm.vocalMode == VocalMode.Accompaniment) "伴唱" else "原唱",
                    Icons.Outlined.SkipNext,
                ) { vm.toggleVocal() }
                Control("切歌", Icons.Outlined.SkipNext) { vm.nextTrack() }
                Control(
                    "收藏",
                    if (vm.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                ) { vm.toggleFavorite() }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Control(
    label: String,
    icon: ImageVector,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(onClick = onClick, corner = 16.dp) { focused ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(if (prominent) 56.dp else 42.dp)
                    .clip(CircleShape)
                    .background(if (focused) Color.White.copy(alpha = 0.22f) else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = KaraokeColors.textPrimary)
            }
            Text(label, color = KaraokeColors.textSecondary, fontSize = 12.sp)
        }
    }
}
