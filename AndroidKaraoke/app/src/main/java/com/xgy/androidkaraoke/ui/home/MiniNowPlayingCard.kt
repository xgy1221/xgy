package com.xgy.androidkaraoke.ui.home

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.KaraokeApp
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.theme.KaraokeDimens
import com.xgy.androidkaraoke.ui.components.FocusSurface

/**
 * 首页迷你唱页：播最近唱过的曲库 MV 预览；点击进入全屏演唱（同 Player 无缝续播）。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MiniNowPlayingCard(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val song = vm.continueSingSong
    val app = LocalContext.current.applicationContext as KaraokeApp
    val player = remember { app.container.player }
    val shape = RoundedCornerShape(KaraokeDimens.cardRadius)

    FocusSurface(
        onClick = { vm.openContinueSinging() },
        modifier = modifier,
        corner = KaraokeDimens.cardRadius,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFF1A2420)),
        ) {
            when {
                song != null && song.hasMv -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
                        modifier = Modifier.fillMaxSize(),
                    )
                    DisposableEffect(song.id) {
                        onDispose {
                            // 全屏 PlaybackScreen 接管同一 ExoPlayer 时释放本 Surface
                        }
                    }
                }
                song != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        song.coverColor.copy(alpha = 0.85f),
                                        song.coverColor.copy(alpha = 0.35f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.28f),
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                        )
                        Text(
                            "还没有唱过",
                            color = KaraokeColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            if (vm.libraryCatalog.isEmpty()) "添加曲库后，这里会预览最近唱的 MV"
                            else "去搜索点一首，唱过就会出现在这里",
                            color = KaraokeColors.textSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (song != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                1f to Color.Black.copy(alpha = if (focused) 0.72f else 0.58f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Text(
                        text = if (vm.nowPlaying?.id == song.id && vm.isPlaying) "正在唱" else "继续唱",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = song.displayTitle,
                        color = KaraokeColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        color = KaraokeColors.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
