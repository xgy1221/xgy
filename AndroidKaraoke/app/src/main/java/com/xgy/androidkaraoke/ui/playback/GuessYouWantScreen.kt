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
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuessYouWantScreen(vm: AppViewModel) {
    val recs = vm.guessYouWantRecommendations
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
                ShelfHeader("猜你想唱", "按你常唱的歌星与曲风 · 仅曲库真实歌曲")
            }
            if (recs.isEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        if (vm.libraryCatalog.isEmpty()) "曲库还没有歌曲，无法推荐"
                        else "暂时没有更多可推荐曲目",
                        color = KaraokeColors.textSecondary,
                        fontSize = 16.sp,
                    )
                    if (vm.libraryCatalog.isEmpty()) {
                        FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                            Text(
                                "去添加曲库",
                                color = KaraokeColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(recs, key = { it.id }) { song ->
                        PosterCard(
                            title = song.title,
                            subtitle = buildString {
                                append(song.artist)
                                if (song.styleTags.isNotEmpty()) {
                                    append(" · ")
                                    append(song.styleTags.take(2).joinToString("/"))
                                }
                            },
                            tint = song.coverColor,
                            badge = song.tag ?: song.genres.firstOrNull(),
                            onClick = { vm.addToQueue(song) },
                            modifier = Modifier.width(260.dp).height(120.dp),
                        )
                    }
                }
            }
        }
    }
}
