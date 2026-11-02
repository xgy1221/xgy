package com.xgy.androidkaraoke.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.model.HomeTileKind
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

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
            HomeTileKind.FreeZone, HomeTileKind.MyQueue, HomeTileKind.Favorites,
            HomeTileKind.KRecommend, HomeTileKind.Ranking,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader("随心唱", "挑选今晚的第一首歌")
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
                    badge = tile.badgeCount?.let { "$it" },
                )
            }
        }
    }
}

private fun onTile(vm: AppViewModel, kind: HomeTileKind) {
    when (kind) {
        HomeTileKind.Daily -> vm.openPlayback(MockData.nowPlaying)
        HomeTileKind.SongOrder -> vm.openPlayback(MockData.aiSongs.first())
        HomeTileKind.SingerOrder -> vm.selectTab(com.xgy.androidkaraoke.model.MainTab.Singers)
        HomeTileKind.CategoryOrder -> vm.selectTab(com.xgy.androidkaraoke.model.MainTab.Hot)
        HomeTileKind.FreeZone -> vm.openPlayback(MockData.aiSongs.random())
        HomeTileKind.MyQueue -> vm.openQueue()
        HomeTileKind.Favorites -> vm.showToast("收藏夹即将推出")
        HomeTileKind.KRecommend -> vm.openGuess()
        HomeTileKind.Ranking -> vm.showToast("排行榜即将推出")
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
