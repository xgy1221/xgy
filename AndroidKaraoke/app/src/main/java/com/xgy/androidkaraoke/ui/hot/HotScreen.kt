package com.xgy.androidkaraoke.ui.hot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HotScreen(vm: AppViewModel) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 40.dp, end = 24.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(KaraokeColors.surface)
                .padding(16.dp),
        ) {
            ShelfHeader("为你推荐")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                items(MockData.aiSongs, key = { it.id }) { song ->
                    FocusSurface(onClick = {
                        vm.addToQueue(song)
                        vm.openPlayback(song)
                    }, corner = 12.dp) { focused ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (focused) KaraokeColors.surfaceStrong
                                    else KaraokeColors.surface.copy(alpha = 0f),
                                )
                                .padding(12.dp),
                        ) {
                            Text(song.displayTitle, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                            Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(MockData.hotPosters, key = { it.id }) { card ->
                    PosterCard(
                        title = card.title,
                        subtitle = card.subtitle,
                        tint = card.tint,
                        badge = card.badge,
                        onClick = { vm.showToast(card.title) },
                        modifier = Modifier.width(180.dp).height(240.dp),
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(MockData.hotBottom, key = { it.id }) { card ->
                    PosterCard(
                        title = card.title,
                        tint = card.tint,
                        onClick = { vm.showToast(card.title) },
                        modifier = Modifier.width(180.dp).height(90.dp),
                    )
                }
            }
        }
    }
}
