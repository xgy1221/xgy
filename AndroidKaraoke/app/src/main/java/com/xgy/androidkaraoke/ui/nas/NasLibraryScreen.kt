package com.xgy.androidkaraoke.ui.nas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.nas.NasEntryKind
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NasLibraryScreen(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 8.dp),
    ) {
        ShelfHeader("曲库（设置）", vm.nasPath)
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusSurface(onClick = { vm.nasGoUp() }, corner = 10.dp) {
                Text(
                    "上级目录",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
            FocusSurface(onClick = { vm.refreshNas() }, corner = 10.dp) {
                Text(
                    "刷新",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
            FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                Text(
                    "设置 NAS",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
        }

        when {
            vm.nasLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("正在连接 NAS…", color = KaraokeColors.textSecondary, fontSize = 20.sp)
                }
            }
            vm.nasError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaraokeColors.danger.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("无法浏览曲库", color = KaraokeColors.textPrimary, fontSize = 22.sp)
                        Text(vm.nasError.orEmpty(), color = KaraokeColors.danger, fontSize = 18.sp)
                        Text(
                            "请检查网络、地址，或在设置中填写账号密码后重试。",
                            color = KaraokeColors.textSecondary,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(vm.nasEntries, key = { it.id }) { entry ->
                        FocusSurface(onClick = { vm.openNasEntry(entry) }, corner = 12.dp) { focused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .background(
                                        if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(
                                    imageVector = when (entry.kind) {
                                        NasEntryKind.Folder -> Icons.Outlined.Folder
                                        NasEntryKind.Audio -> Icons.Outlined.AudioFile
                                        NasEntryKind.Video -> Icons.Outlined.Movie
                                        else -> Icons.Outlined.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = KaraokeColors.textSecondary,
                                )
                                Column {
                                    Text(entry.name, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                    Text(
                                        when (entry.kind) {
                                            NasEntryKind.Folder -> "文件夹"
                                            NasEntryKind.Audio -> "音频"
                                            NasEntryKind.Video -> "视频"
                                            NasEntryKind.Manifest -> "清单"
                                            NasEntryKind.Other -> "文件"
                                        },
                                        color = KaraokeColors.textTertiary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
