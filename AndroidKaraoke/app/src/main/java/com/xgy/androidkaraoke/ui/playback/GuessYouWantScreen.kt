package com.xgy.androidkaraoke.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuessYouWantScreen(vm: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 48.dp)) {
                ShelfHeader("猜你想唱", "正在播放：${vm.nowPlaying?.title ?: "—"}")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(MockData.guessSongs, key = { it.id }) { song ->
                    PosterCard(
                        title = song.title,
                        subtitle = song.artist,
                        tint = song.coverColor,
                        badge = song.tag,
                        onClick = {
                            vm.addToQueue(song)
                        },
                        modifier = Modifier.width(260.dp).height(120.dp),
                    )
                }
            }
            Text(
                "歌单推荐",
                color = KaraokeColors.textPrimary,
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 48.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(MockData.playlists, key = { it.id }) { card ->
                    PosterCard(
                        title = card.title,
                        tint = card.tint,
                        badge = card.badge,
                        onClick = { vm.showToast(card.title) },
                        modifier = Modifier.width(180.dp).height(100.dp),
                    )
                }
            }
        }
    }
}
