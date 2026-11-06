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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
fun HomeScreen(
    vm: AppViewModel,
    entryFocus: FocusRequester,
    tabFocus: FocusRequester,
) {
    // 明确排除 Daily：首页英雄位只能是迷你唱页，绝不再渲染「每日推荐」星标
    val top = MockData.homeTiles.filter {
        it.kind in setOf(
            HomeTileKind.SongOrder,
            HomeTileKind.SingerOrder,
        )
    }
    val bottom = MockData.homeTiles.filter {
        it.kind in setOf(
            HomeTileKind.MyQueue,
            HomeTileKind.Favorites,
            HomeTileKind.KRecommend,
        )
    }
    val recent = vm.recentSongs
    val guide = vm.shouldShowLibraryGuide
    val heroFocus = remember { FocusRequester() }
    val songFocus = remember { FocusRequester() }
    val singerFocus = remember { FocusRequester() }
    val recentFocus = remember { FocusRequester() }
    val bottomFocus = remember { FocusRequester() }
    val miniKey = if (guide) heroFocus else entryFocus
    val upFromHero = if (guide) entryFocus else tabFocus
    val downFromHero = if (recent.isNotEmpty()) recentFocus else bottomFocus
    val upFromBottom = if (recent.isNotEmpty()) recentFocus else miniKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader("随心唱", "挑选今晚的第一首歌")
        }

        if (guide) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(KaraokeColors.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FocusSurface(
                    onClick = { vm.openSettingsLibrary() },
                    corner = 10.dp,
                    requestFocus = true,
                    focusRequester = entryFocus,
                    navigateDown = miniKey,
                ) {
                    Text(
                        "添加媒体库",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                    )
                }
                FocusSurface(
                    onClick = { vm.dismissLibraryGuide() },
                    corner = 10.dp,
                    navigateDown = miniKey,
                ) {
                    Text(
                        "稍后",
                        color = KaraokeColors.textTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("还没有可用曲库", color = KaraokeColors.textPrimary, fontSize = 18.sp)
                    Text(
                        "去设置添加 NAS（先连接再选目录）或扫描 U 盘，唱自己的歌。",
                        color = KaraokeColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "mini-now-playing") {
                MiniNowPlayingCard(
                    vm = vm,
                    modifier = Modifier
                        .width(400.dp)
                        .height(220.dp),
                    requestFocus = !guide,
                    focusRequester = miniKey,
                    navigateUp = upFromHero,
                    navigateDown = downFromHero,
                    navigateRight = songFocus,
                )
            }
            itemsIndexed(top, key = { _, tile -> tile.id }) { index, tile ->
                val mine = if (index == 0) songFocus else singerFocus
                PosterCard(
                    title = tile.title,
                    tint = tile.tint,
                    onClick = { onTile(vm, tile.kind) },
                    modifier = Modifier
                        .width(200.dp)
                        .height(220.dp),
                    icon = iconFor(tile.kind),
                    focusRequester = mine,
                    navigateUp = upFromHero,
                    navigateDown = downFromHero,
                    navigateLeft = if (index == 0) miniKey else songFocus,
                    navigateRight = if (index == 0 && top.size > 1) singerFocus else null,
                )
            }
        }

        if (recent.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 40.dp)) {
                ShelfHeader("最近唱过", "仅曲库真实歌曲 · 一键再点")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(recent, key = { _, song -> song.id }) { index, song ->
                    FocusSurface(
                        onClick = {
                            vm.addToQueue(song)
                            vm.openPlayback(song)
                        },
                        corner = 12.dp,
                        focusRequester = if (index == 0) recentFocus else null,
                        navigateUp = miniKey,
                        navigateDown = bottomFocus,
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
            itemsIndexed(bottom, key = { _, tile -> tile.id }) { index, tile ->
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
                        else -> null
                    },
                    focusRequester = if (index == 0) bottomFocus else null,
                    navigateUp = upFromBottom,
                )
            }
        }
    }
}

private fun onTile(vm: AppViewModel, kind: HomeTileKind) {
    when (kind) {
        HomeTileKind.Daily -> vm.openContinueSinging()
        HomeTileKind.SongOrder -> vm.openSearch()
        HomeTileKind.SingerOrder -> vm.selectTab(com.xgy.androidkaraoke.model.MainTab.Singers)
        HomeTileKind.CategoryOrder -> vm.openSearch()
        HomeTileKind.FreeZone -> vm.openSearch()
        HomeTileKind.MyQueue -> vm.openQueue()
        HomeTileKind.Favorites -> {
            val favs = vm.favoriteSongs
            if (favs.isEmpty()) vm.showToast("还没有收藏，播放页可收藏")
            else vm.openSearch()
        }
        HomeTileKind.KRecommend -> vm.openGuess()
        HomeTileKind.Ranking -> Unit
    }
}

private fun iconFor(kind: HomeTileKind): ImageVector = when (kind) {
    HomeTileKind.SongOrder -> Icons.Outlined.MusicNote
    HomeTileKind.SingerOrder -> Icons.Outlined.PersonSearch
    HomeTileKind.MyQueue -> Icons.Outlined.QueueMusic
    HomeTileKind.Favorites -> Icons.Outlined.FavoriteBorder
    HomeTileKind.KRecommend -> Icons.Outlined.Recommend
    else -> Icons.Outlined.MusicNote
}
