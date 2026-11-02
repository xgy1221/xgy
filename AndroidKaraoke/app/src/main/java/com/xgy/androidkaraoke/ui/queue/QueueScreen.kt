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
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QueueScreen(vm: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.72f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(520.dp)
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .padding(28.dp),
        ) {
            ShelfHeader("已点队列", "共 ${vm.queue.size} 首")
            if (vm.queue.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有点歌，去曲库挑几首吧", color = KaraokeColors.textSecondary, fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                ) {
                    itemsIndexed(vm.queue, key = { _, s -> s.id }) { index, song ->
                        FocusSurface(
                            onClick = { vm.singNow(song) },
                            corner = 12.dp,
                        ) { focused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .background(
                                        if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("${index + 1}. ${song.title}", color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                    Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 14.sp)
                                }
                                FocusSurface(onClick = { vm.removeFromQueue(song) }, corner = 8.dp) {
                                    Text(
                                        "移除",
                                        color = KaraokeColors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 14.sp,
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
