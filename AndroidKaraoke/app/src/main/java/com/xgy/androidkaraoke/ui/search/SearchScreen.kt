package com.xgy.androidkaraoke.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.songs.SongRow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel) {
    val results = vm.searchResults()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.94f))
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Column {
            Text("搜索歌曲 / 歌星", color = KaraokeColors.textPrimary, fontSize = 28.sp)
            BasicTextField(
                value = vm.searchQuery,
                onValueChange = { vm.searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = KaraokeColors.textPrimary, fontSize = 22.sp),
                cursorBrush = SolidColor(KaraokeColors.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(KaraokeColors.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(results, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        index = index + 1,
                        song = song,
                        onSingNow = { vm.singNow(song) },
                        onEnqueue = { vm.enqueue(song) },
                    )
                }
            }
            FocusSurface(onClick = { vm.dismissOverlay() }, corner = 12.dp) {
                Text(
                    "关闭",
                    color = KaraokeColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
        }
    }
}
