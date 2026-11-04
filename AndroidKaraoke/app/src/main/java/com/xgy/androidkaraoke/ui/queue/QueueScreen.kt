package com.xgy.androidkaraoke.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.BlockingOverlay
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ModalPanel
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QueueScreen(vm: AppViewModel) {
    BlockingOverlay(contentAlignment = Alignment.CenterEnd) {
        ModalPanel(
            modifier = Modifier
                .fillMaxHeight()
                .width(560.dp)
                .background(
                    KaraokeColors.backgroundElevated,
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                )
                .padding(28.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ShelfHeader("已点队列", "共 ${vm.queue.size} 首")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                ) {
                    FocusSurface(onClick = { vm.clearQueue() }, corner = 10.dp) {
                        Text(
                            "清空",
                            color = KaraokeColors.danger,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                        )
                    }
                    FocusSurface(onClick = { vm.dismissOverlay() }, corner = 10.dp) {
                        Text(
                            "关闭",
                            color = KaraokeColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                        )
                    }
                }

                vm.nowPlaying?.let { playing ->
                    Text(
                        "正在播放：${playing.displayTitle}",
                        color = KaraokeColors.accentSoft,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (vm.queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("还没有点歌", color = KaraokeColors.textSecondary, fontSize = 18.sp)
                            FocusSurface(onClick = { vm.openSearch() }, corner = 10.dp) {
                                Text(
                                    "去搜索点歌",
                                    color = KaraokeColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(vm.queue, key = { _, s -> s.id }) { index, song ->
                            val isNext = index == 0
                            val isPlayingNow = vm.nowPlaying?.id == song.id
                            FocusSurface(
                                onClick = { vm.openPlayback(song) },
                                corner = 12.dp,
                            ) { focused ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .background(
                                            when {
                                                focused -> KaraokeColors.surfaceStrong
                                                isPlayingNow -> KaraokeColors.accentSoft.copy(alpha = 0.18f)
                                                else -> KaraokeColors.surface
                                            },
                                            RoundedCornerShape(12.dp),
                                        )
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            buildString {
                                                append("${index + 1}. ${song.displayTitle}")
                                                if (isPlayingNow) append(" · 正在播")
                                                else if (isNext) append(" · 下一首")
                                            },
                                            color = KaraokeColors.textPrimary,
                                            fontSize = 17.sp,
                                        )
                                        Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 13.sp)
                                    }
                                    FocusSurface(onClick = { vm.moveQueueToTop(song) }, corner = 8.dp) {
                                        Text(
                                            "置顶",
                                            color = KaraokeColors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 13.sp,
                                        )
                                    }
                                    FocusSurface(onClick = { vm.removeFromQueue(song) }, corner = 8.dp) {
                                        Text(
                                            "删除",
                                            color = KaraokeColors.danger,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
