package com.xgy.androidkaraoke.ui.singers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
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
fun SingersScreen(vm: AppViewModel) {
    val artists = vm.libraryArtists
    val recommended = vm.recommendedArtists
    val hasLibrary = vm.libraryCatalog.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader(
                "歌星",
                if (hasLibrary) "来自你的曲库 · 按常唱习惯排序"
                else "添加曲库后，这里会列出真实歌星",
            )
        }

        if (!hasLibrary) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(KaraokeColors.surface, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("还没有曲库歌星", color = KaraokeColors.textPrimary, fontSize = 18.sp)
                    Text(
                        "去设置添加 NAS 或 U 盘曲库；扁平原文件名「歌名-歌手」也会自动识别。",
                        color = KaraokeColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
                FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                    Text(
                        "去添加曲库",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                    )
                }
            }
            return
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PosterCard(
                    title = "搜索歌星",
                    subtitle = "姓名 / 首拼",
                    tint = Color(0xFF2E5BB8),
                    onClick = { vm.openArtistSearch() },
                    modifier = Modifier.width(220.dp).height(180.dp),
                    icon = Icons.Outlined.PersonSearch,
                )
            }
            items(recommended.take(6), key = { "rec-${it.id}" }) { artist ->
                PosterCard(
                    title = artist.name,
                    subtitle = "${artist.songCount} 首",
                    tint = artist.coverColor,
                    onClick = { vm.openArtistSongs(artist.name) },
                    modifier = Modifier.width(280.dp).height(180.dp),
                    badge = if (artist.habitWeight > 0f) "常唱" else null,
                )
            }
        }

        if (artists.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 40.dp)) {
                ShelfHeader("全部歌星", "共 ${artists.size} 位 · 按歌曲数")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    artists.sortedWith(
                        compareByDescending<com.xgy.androidkaraoke.model.LibraryArtist> { it.songCount }
                            .thenBy { it.name },
                    ),
                    key = { it.id },
                ) { artist ->
                    PosterCard(
                        title = artist.name,
                        subtitle = "${artist.songCount} 首",
                        tint = artist.coverColor,
                        onClick = { vm.openArtistSongs(artist.name) },
                        modifier = Modifier.size(140.dp),
                    )
                }
            }
        }
    }
}
