package com.xgy.androidkaraoke.ui.singers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@Composable
fun SingersScreen(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader("歌星", "按歌手找歌")
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PosterCard(
                    title = "搜索歌星",
                    subtitle = "输入姓名",
                    tint = Color(0xFF2E5BB8),
                    onClick = { vm.showToast("歌星搜索即将推出") },
                    modifier = Modifier.width(220.dp).height(180.dp),
                    icon = Icons.Outlined.PersonSearch,
                )
            }
            items(MockData.featuredSingers, key = { it.id }) { singer ->
                PosterCard(
                    title = singer.name,
                    tint = singer.coverColor,
                    onClick = { vm.showToast("进入 ${singer.name}") },
                    modifier = Modifier.width(280.dp).height(180.dp),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(MockData.singerGrid, key = { it.id }) { singer ->
                PosterCard(
                    title = singer.name,
                    tint = singer.coverColor,
                    onClick = { vm.showToast(singer.name) },
                    modifier = Modifier.size(140.dp),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(MockData.singerRegions, key = { it.id }) { region ->
                PosterCard(
                    title = region.title,
                    tint = region.tint.copy(alpha = 0.55f),
                    onClick = { vm.showToast(region.title) },
                    modifier = Modifier.width(160.dp).height(72.dp),
                )
            }
        }
    }
}
