package com.xgy.androidkaraoke.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.songs.SongBrowserScreen

@Composable
fun PlaylistScreen(vm: AppViewModel) {
    if (vm.drillTitle != null) {
        SongBrowserScreen(vm)
        return
    }
    val names = vm.playlists()
    if (names.isEmpty()) {
        Text(
            text = "歌单来自曲库里的文件夹。例如「欢唱金曲/歌手/歌名」。",
            color = KaraokeColors.textSecondary,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(names, key = { it }) { name ->
            PosterCard(
                title = name,
                subtitle = "文件夹",
                tint = Color(0xFF6A3A3A),
                onClick = { vm.openPlaylist(name) },
                modifier = Modifier
                    .width(220.dp)
                    .height(120.dp),
            )
        }
    }
}
