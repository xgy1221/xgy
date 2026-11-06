package com.xgy.androidkaraoke.ui.singers

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
fun ArtistSongsScreen(vm: AppViewModel) {
    val artist = vm.browsingArtist
    val startVoice = rememberVoiceInput(
        onResult = vm::updateArtistSearchQuery,
        onUnavailable = { vm.showToast("这台电视没有语音识别，请用键盘右上角的「语音」") },
    )

    BlockingOverlay(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (artist != null || !event.isRemoteVoiceKey()) return@onPreviewKeyEvent false
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
                if (artist == null) {
                    ShelfHeader("搜索歌星", "姓名 / 首拼 · 确定重新打开键盘")
                    TvSearchField(
                        value = vm.artistSearchQuery,
                        onValueChange = vm::updateArtistSearchQuery,
                        hint = "输入歌星名，如 刀郎 / dl",
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
                    }
                    val results = vm.artistSearchResults
                    if (results.isEmpty()) {
                        Text("没有匹配歌星", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            items(results, key = { it.id }) { item ->
                                FocusSurface(
                                    onClick = { vm.openArtistSongs(item.name) },
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
                                        Text(item.name, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                        Text(
                                            "${item.songCount} 首",
                                            color = KaraokeColors.textSecondary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    ShelfHeader(artist, "曲库共 ${vm.browsingArtistSongs.size} 首 · 点歌入队")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocusSurface(
                            onClick = { vm.clearBrowsingArtist() },
                            corner = 10.dp,
                        ) {
                            Text(
                                "返回",
                                color = KaraokeColors.textSecondary,
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
                    }
                    val songs = vm.browsingArtistSongs
                    if (songs.isEmpty()) {
                        Text("该歌星暂无歌曲", color = KaraokeColors.textSecondary, fontSize = 16.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            items(songs, key = { it.id }) { song ->
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
