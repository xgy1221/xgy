package com.xgy.androidkaraoke.ui.singers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.songs.SongBrowserScreen
import androidx.compose.ui.graphics.Color

@Composable
fun SingersScreen(vm: AppViewModel) {
    if (vm.drillTitle != null) {
        SongBrowserScreen(vm)
        return
    }
    val singers = vm.singers()
    if (singers.isEmpty()) {
        Text(
            text = "扫到曲库后，这里按歌手列出。",
            color = KaraokeColors.textSecondary,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(singers, key = { it }) { name ->
            PosterCard(
                title = name,
                tint = Color(0xFF3A4A68),
                onClick = { vm.openSinger(name) },
                modifier = Modifier.height(120.dp),
            )
        }
    }
}
