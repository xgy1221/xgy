package com.xgy.androidkaraoke.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.songs.SongRow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.9f))
            .padding(40.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("我的", color = KaraokeColors.textPrimary, fontSize = 28.sp, modifier = Modifier.weight(1f))
                FocusSurface(onClick = { vm.dismissOverlay() }, corner = 10.dp) {
                    Text("关闭", color = KaraokeColors.textSecondary, modifier = Modifier.padding(12.dp))
                }
            }
            Row(modifier = Modifier.padding(top = 12.dp)) {
                SongColumn("收藏", vm.favoriteSongs(), vm, Modifier.weight(1f))
                SongColumn("最近唱过", vm.recentSongs(), vm, Modifier.weight(1f).padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun SongColumn(title: String, songs: List<Song>, vm: AppViewModel, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(title, color = KaraokeColors.textPrimary, fontSize = 20.sp)
        if (songs.isEmpty()) {
            Text("还没有", color = KaraokeColors.textTertiary, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn {
                itemsIndexed(songs.take(40), key = { _, song -> "$title-${song.id}" }) { index, song ->
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
