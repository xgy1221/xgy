package com.xgy.androidkaraoke.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.model.HomeTileKind
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val top = MockData.homeTiles.filter {
        it.kind in setOf(
            HomeTileKind.Daily, HomeTileKind.SongOrder,
            HomeTileKind.SingerOrder, HomeTileKind.CategoryOrder,
        )
    }
    val bottom = MockData.homeTiles.filter {
        it.kind in setOf(
            HomeTileKind.MyQueue, HomeTileKind.Favorites,
            HomeTileKind.KRecommend,
        )
    }
    val recent = vm.recentSongs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader("随心唱", "挑选今晚的第一首歌")
        }

        if (vm.shouldShowLibraryGuide) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(KaraokeColors.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("还没有可用曲库", color = KaraokeColors.textPrimary, fontSize = 18.sp)
                    Text(
                        "去设置添加 NAS（先连接再选目录）或扫描 U 盘，唱自己的歌。",
                        color = KaraokeColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
                FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                    Text(
                        "添加曲库",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                    )
                }
                FocusSurface(onClick = { vm.dismissLibraryGuide() }, corner = 10.dp) {
                    Text(
                        "稍后",
                        color = KaraokeColors.textTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(top, key = { it.id }) { tile ->
                PosterCard(
                    title = tile.title,
                    tint = tile.tint,
                    onClick = { onTile(vm, tile.kind) },
                    modifier = Modifier
                        .width(if (tile.isWide) 360.dp else 200.dp)
                        .height(200.dp),
                    icon = iconFor(tile.kind),
                    badge = tile.badgeCount?.let { "已点 $it" },
                )
            }
        }

        if (recent.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 40.dp)) {
                ShelfHeader("最近唱过", "一键再点")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recent, key = { it.id }) { song ->
                    FocusSurface(
                        onClick = {
                            vm.addToQueue(song)
                            vm.openPlayback(song)
                        },
                        corner = 12.dp,
                    ) { focused ->
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .height(88.dp)
                                .background(
                                    if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(14.dp),
                        ) {
                            Text(song.displayTitle, color = KaraokeColors.textPrimary, fontSize = 16.sp, maxLines = 1)
                            Text(song.artist, color = KaraokeColors.textSecondary, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(bottom, key = { it.id }) { tile ->
                PosterCard(
                    title = tile.title,
                    tint = tile.tint,
                    onClick = { onTile(vm, tile.kind) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp),
                    icon = iconFor(tile.kind),
                    badge = when (tile.kind) {
                        HomeTileKind.MyQueue -> vm.queue.size.takeIf { it > 0 }?.toString()
                        else -> tile.badgeCount?.toString()
                    },
                )
            }
        }
    }
}

private fun onTile(vm: AppViewModel, kind: HomeTileKind) {
    when (kind) {
        HomeTileKind.Daily -> {
            val song = vm.forYouRecommendations.firstOrNull() ?: MockData.nowPlaying
            vm.openPlayback(song)
        }
        HomeTileKind.SongOrder -> vm.openSearch()
        HomeTileKind.SingerOrder -> vm.selectTab(com.xgy.androidkaraoke.model.MainTab.Singers)
        HomeTileKind.CategoryOrder -> vm.selectTab(com.xgy.androidkaraoke.model.MainTab.Hot)
        HomeTileKind.FreeZone -> vm.openSearch()
        HomeTileKind.MyQueue -> vm.openQueue()
        HomeTileKind.Favorites -> {
            val favs = vm.favoriteSongs
            if (favs.isEmpty()) vm.showToast("还没有收藏，播放页可收藏")
            else vm.openSearch()
        }
        HomeTileKind.KRecommend -> vm.openGuess()
        HomeTileKind.Ranking -> vm.showToast("排行榜已从顶栏移除")
    }
}

private fun iconFor(kind: HomeTileKind): ImageVector = when (kind) {
    HomeTileKind.Daily -> Icons.Outlined.Star
    HomeTileKind.SongOrder -> Icons.Outlined.MusicNote
    HomeTileKind.SingerOrder -> Icons.Outlined.PersonSearch
    HomeTileKind.CategoryOrder -> Icons.Outlined.Category
    HomeTileKind.FreeZone -> Icons.Outlined.Mic
    HomeTileKind.MyQueue -> Icons.Outlined.QueueMusic
    HomeTileKind.Favorites -> Icons.Outlined.FavoriteBorder
    HomeTileKind.KRecommend -> Icons.Outlined.Recommend
    HomeTileKind.Ranking -> Icons.Outlined.Album
}
