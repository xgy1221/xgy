package com.xgy.androidkaraoke.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.SongListFilter
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SongBrowserScreen(vm: AppViewModel) {
    val songs = vm.visibleSongs()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
    ) {
        ShelfHeader(
            title = vm.drillTitle ?: if (vm.selectedTab == com.xgy.androidkaraoke.model.MainTab.Charts) "榜单" else "点歌",
            subtitle = vm.statusMessage,
        )
        if (vm.drillTitle == null) {
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SongListFilter.entries.forEach { filter ->
                    val active = filter == vm.songFilter
                    FocusSurface(onClick = { vm.selectFilter(filter) }, corner = 10.dp) { focused ->
                        Text(
                            text = filter.title,
                            color = if (active || focused) KaraokeColors.textPrimary else KaraokeColors.textSecondary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
        if (songs.isEmpty()) {
            Text(
                text = if (vm.catalog.isEmpty()) {
                    "曲库还是空的。打开右上角设置，填写 NAS 账号并扫描。歌曲按「歌手/歌名」文件夹放：MV、原唱、伴唱三个文件。"
                } else {
                    "这个列表里还没有歌。"
                },
                color = KaraokeColors.textSecondary,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        index = index + 1,
                        song = song,
                        onSingNow = { vm.singNow(song) },
                        onEnqueue = { vm.enqueue(song) },
                    )
                }
            }
        }
    }
}
