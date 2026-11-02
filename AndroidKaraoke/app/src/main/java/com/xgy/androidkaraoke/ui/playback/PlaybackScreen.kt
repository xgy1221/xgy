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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            (song?.coverColor ?: Color(0xFF334455)).copy(alpha = 0.45f),
                            Color.Black,
                        ),
                    ),
                ),
        )
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
        }

        Column(
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
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(vm.progress)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.85f)),
                    )
                }
                Text(
                    vm.formatTime(song?.durationSec ?: 0),
                    color = KaraokeColors.textSecondary,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlBtn("跳过前奏", Icons.Outlined.FastForward) { vm.skipPrelude() }
                ControlBtn("顺序", Icons.Outlined.Shuffle) { vm.cycleOrder() }
                ControlBtn("重唱", Icons.Outlined.Replay) { vm.replay() }
                ControlBtn(
                    if (vm.isPlaying) "暂停" else "播放",
                    if (vm.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    prominent = true,
                ) { vm.togglePlayPause() }
                ControlBtn(
                    if (vm.vocalMode == VocalMode.Accompaniment) "伴唱" else "原唱",
                    Icons.Outlined.RecordVoiceOver,
                ) { vm.toggleVocal() }
                ControlBtn("切歌", Icons.Outlined.SkipNext) { vm.nextTrack() }
                ControlBtn(
                    "收藏",
                    if (vm.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                ) { vm.toggleFavorite() }
            }
            Spacer(modifier = Modifier.height(14.dp))
            FocusSurface(onClick = { vm.openGuess() }, corner = 10.dp) {
                Text(
                    text = "按【下键】或确认查看「猜你想唱」",
                    color = KaraokeColors.textSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlBtn(
    label: String,
    icon: ImageVector,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    FocusSurface(onClick = onClick, corner = if (prominent) 28.dp else 14.dp) { focused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (prominent) 56.dp else 44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            focused -> Color.White.copy(alpha = 0.22f)
                            prominent -> Color.White.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
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
