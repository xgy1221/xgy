package com.xgy.androidkaraoke.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.xgy.androidkaraoke.ui.components.TvSearchField
import com.xgy.androidkaraoke.ui.components.isRemoteVoiceKey
import com.xgy.androidkaraoke.ui.components.rememberVoiceInput

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel) {
    val startVoice = rememberVoiceInput(
        onResult = vm::updateSearchQuery,
        onUnavailable = { vm.showToast("这台电视没有语音识别，请用键盘右上角的「语音」") },
    )

    BlockingOverlay(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (!event.isRemoteVoiceKey()) return@onPreviewKeyEvent false
            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                event.nativeKeyEvent.repeatCount == 0
            ) {
                startVoice()
            }
            true
        },
    ) {
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
                ShelfHeader("搜索", "歌名 / 歌星 / 首拼 · 确定重新打开键盘 · 语音键说话")
                TvSearchField(
                    value = vm.searchQuery,
                    onValueChange = vm::updateSearchQuery,
                    hint = "搜索曲库歌名 / 歌星 / 首拼",
                    requestFocus = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusSurface(onClick = startVoice, corner = 10.dp) {
                        Text(
                            "语音",
                            color = KaraokeColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 15.sp,
                        )
                    }
                    FocusSurface(onClick = { vm.dismissOverlay() }, corner = 10.dp) {
                        Text(
                            "关闭",
                            color = KaraokeColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 15.sp,
                        )
                    }
                    if (vm.shouldShowLibraryGuide) {
                        FocusSurface(onClick = { vm.openSettingsLibrary() }, corner = 10.dp) {
                            Text(
                                "去添加媒体库",
                                color = KaraokeColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 15.sp,
                            )
                        }
                    }
                }

                val results = vm.searchResults
                when {
                    vm.libraryCatalog.isEmpty() -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "曲库还没有歌曲",
                                color = KaraokeColors.textSecondary,
                                fontSize = 16.sp,
                            )
                            Text(
                                "演示歌不会出现在这里。请先添加 NAS / U 盘曲库后再点歌。",
                                color = KaraokeColors.textTertiary,
                                fontSize = 14.sp,
                            )
                            FocusSurface(onClick = { vm.openSettingsLibrary() }, corner = 10.dp) {
                                Text(
                                    "去添加媒体库",
                                    color = KaraokeColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                    results.isEmpty() && vm.searchQuery.isNotBlank() -> {
                        Text("曲库里没有匹配歌曲，换个词试试", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    }
                    results.isEmpty() -> {
                        Text("曲库为空或暂无推荐", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            items(results, key = { it.id }) { song ->
                                // 行本身不可点；仅「点歌」可聚焦点击，避免嵌套 FocusSurface 吞事件
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(KaraokeColors.surface, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(song.displayTitle, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                        Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 14.sp)
                                    }
                                    FocusSurface(
                                        onClick = { vm.addToQueue(song) },
                                        corner = 8.dp,
                                    ) { focused ->
                                        Text(
                                            "点歌",
                                            color = if (focused) KaraokeColors.textPrimary else KaraokeColors.accentSoft,
                                            modifier = Modifier
                                                .background(
                                                    if (focused) KaraokeColors.surfaceStrong
                                                    else KaraokeColors.surface.copy(alpha = 0f),
                                                    RoundedCornerShape(8.dp),
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
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
