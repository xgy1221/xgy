package com.xgy.androidkaraoke.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
fun SearchScreen(vm: AppViewModel) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    BlockingOverlay {
        ModalPanel(
            modifier = Modifier
                .width(900.dp)
                .height(560.dp)
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .padding(28.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShelfHeader("搜索", "歌名 / 歌星 / 首拼 · 确认点歌入队")
                FocusSurface(onClick = {}, corner = 12.dp) { focused ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (vm.searchQuery.isEmpty()) {
                            Text("输入关键词，如 小半 / 刀郎 / xb", color = KaraokeColors.textTertiary, fontSize = 17.sp)
                        }
                        BasicTextField(
                            value = vm.searchQuery,
                            onValueChange = { vm.updateSearchQuery(it) },
                            singleLine = true,
                            textStyle = TextStyle(color = KaraokeColors.textPrimary, fontSize = 18.sp),
                            cursorBrush = SolidColor(KaraokeColors.textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusSurface(onClick = { vm.dismissOverlay() }, corner = 10.dp) {
                        Text(
                            "关闭",
                            color = KaraokeColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 15.sp,
                        )
                    }
                    if (vm.shouldShowLibraryGuide) {
                        FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                            Text(
                                "去添加曲库",
                                color = KaraokeColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                val results = vm.searchResults
                when {
                    results.isEmpty() && vm.searchQuery.isNotBlank() -> {
                        Text("没有匹配歌曲，换个词试试", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    }
                    results.isEmpty() -> {
                        Text("输入后显示结果；也可浏览下方热门", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            items(results, key = { it.id }) { song ->
                                FocusSurface(
                                    onClick = {
                                        vm.addToQueue(song)
                                        vm.openPlayback(song)
                                    },
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
                                            Text(song.displayTitle, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                            Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 14.sp)
                                        }
                                        FocusSurface(
                                            onClick = { vm.addToQueue(song) },
                                            corner = 8.dp,
                                        ) {
                                            Text(
                                                "点歌",
                                                color = KaraokeColors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
    }
}
