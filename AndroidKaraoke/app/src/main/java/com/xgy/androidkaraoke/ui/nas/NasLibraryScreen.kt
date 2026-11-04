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
import com.xgy.androidkaraoke.library.LibraryEntryKind
import com.xgy.androidkaraoke.library.LibrarySourceKind
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

/**
 * 曲库浏览：整屏不透明替换主界面（禁止半透明叠在首页上）。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NasLibraryScreen(vm: AppViewModel) {
    val source = vm.activeSource
    val title = when (source?.kind) {
        LibrarySourceKind.Usb -> "曲库 · U 盘"
        LibrarySourceKind.Nas -> "曲库 · NAS"
        null -> "曲库"
    }
    val subtitle = buildString {
        append(source?.displayName ?: "未选择来源")
        append("  ·  ")
        append(vm.libraryPath.ifBlank { "—" })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background)
            .padding(horizontal = 40.dp, vertical = 20.dp),
    ) {
        ShelfHeader(title, subtitle)
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusSurface(onClick = { vm.dismissOverlay() }, corner = 10.dp) {
                Text(
                    "返回设置",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
            FocusSurface(onClick = { vm.libraryGoUp() }, corner = 10.dp) {
                Text(
                    "上级目录",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
            FocusSurface(onClick = { vm.refreshLibrary() }, corner = 10.dp) {
                Text(
                    "刷新",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
            FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                Text(
                    "曲库来源",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 16.sp,
                )
            }
        }

        when {
            vm.libraryLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KaraokeColors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("正在加载曲库…", color = KaraokeColors.textSecondary, fontSize = 22.sp)
                }
            }
            vm.libraryError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KaraokeColors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .background(KaraokeColors.danger.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("无法浏览曲库", color = KaraokeColors.textPrimary, fontSize = 26.sp)
                        Text(vm.libraryError.orEmpty(), color = KaraokeColors.danger, fontSize = 18.sp)
                        Text(
                            when (source?.kind) {
                                LibrarySourceKind.Usb ->
                                    "请确认 U 盘已插入、已授予存储权限，或返回设置重新扫描。"
                                else ->
                                    "群晖请用 DSM 地址（含端口）+ File Station 路径（如 /volume1/music）并填写账号密码；" +
                                        "若是 WebDAV/直链目录，请把 Base URL 改成文件服务地址。"
                            },
                            color = KaraokeColors.textSecondary,
                            fontSize = 15.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FocusSurface(onClick = { vm.refreshLibrary() }, corner = 10.dp) {
                                Text(
                                    "重试",
                                    color = KaraokeColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    fontSize = 16.sp,
                                )
                            }
                            FocusSurface(onClick = { vm.openSettings() }, corner = 10.dp) {
                                Text(
                                    "去设置",
                                    color = KaraokeColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KaraokeColors.background),
                ) {
                    items(vm.libraryEntries, key = { it.id }) { entry ->
                        FocusSurface(onClick = { vm.openLibraryEntry(entry) }, corner = 12.dp) { focused ->
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
                                        LibraryEntryKind.Folder -> Icons.Outlined.Folder
                                        LibraryEntryKind.Audio -> Icons.Outlined.AudioFile
                                        LibraryEntryKind.Video -> Icons.Outlined.Movie
                                        else -> Icons.Outlined.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = KaraokeColors.textSecondary,
                                )
                                Column {
                                    Text(entry.name, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                                    Text(
                                        when (entry.kind) {
                                            LibraryEntryKind.Folder -> "文件夹（可含 MV/原唱/伴唱/歌词）"
                                            LibraryEntryKind.Audio -> "音频"
                                            LibraryEntryKind.Video -> "视频"
                                            LibraryEntryKind.Manifest -> "清单"
                                            LibraryEntryKind.Lyrics -> "歌词 LRC"
                                            LibraryEntryKind.Other -> "文件"
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
