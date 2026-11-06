package com.xgy.androidkaraoke.ui.home

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
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
 * 首页迷你唱页（英雄位）：ExoPlayer 实时 MV 预览。
 * 禁止做成绿色星标「每日推荐」瓷砖。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MiniNowPlayingCard(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
) {
    val song = vm.continueSingSong
    val app = LocalContext.current.applicationContext as KaraokeApp
    val player = remember { app.container.player }
    val shape = RoundedCornerShape(KaraokeDimens.cardRadius)

    FocusSurface(
        onClick = { vm.openContinueSinging() },
        modifier = modifier,
        corner = KaraokeDimens.cardRadius,
        requestFocus = requestFocus,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFF0E1218))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
                    shape = shape,
                ),
        ) {
            when {
                song != null && song.hasMv -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                isFocusable = false
                                isFocusableInTouchMode = false
                                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
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
                        modifier = Modifier
                            .fillMaxSize()
                            .focusProperties { canFocus = false },
                    )
                }
                song != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        song.coverColor.copy(alpha = 0.85f),
                                        Color(0xFF0E1218),
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
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = KaraokeColors.accentSoft.copy(alpha = 0.9f),
                        )
                        Text(
                            when {
                                vm.libraryIndexing -> "正在加载曲库…"
                                vm.libraryCatalog.isEmpty() -> "迷你唱页"
                                else -> "准备播放"
                            },
                            color = KaraokeColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            when {
                                vm.libraryIndexing -> "NAS / U 盘索引完成后会自动预览 MV"
                                vm.libraryCatalog.isEmpty() -> "去设置添加曲库后，这里会自动开唱预览"
                                else -> "点这里进入演唱页"
                            },
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
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = if (focused) 0.75f else 0.6f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Text(
                        text = when {
                            vm.nowPlaying?.id == song.id && vm.isPlaying -> "正在唱 · MV 预览"
                            vm.nowPlaying?.id == song.id -> "继续唱"
                            else -> "迷你唱页"
                        },
                        color = KaraokeColors.accentSoft,
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
