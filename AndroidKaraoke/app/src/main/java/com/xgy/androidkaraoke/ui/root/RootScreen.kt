package com.xgy.androidkaraoke.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.AmbientBackground
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.playback.PlaybackScreen
import com.xgy.androidkaraoke.ui.playlists.PlaylistScreen
import com.xgy.androidkaraoke.ui.profile.ProfileScreen
import com.xgy.androidkaraoke.ui.queue.QueueScreen
import com.xgy.androidkaraoke.ui.remote.RemoteScreen
import com.xgy.androidkaraoke.ui.search.SearchScreen
import com.xgy.androidkaraoke.ui.settings.SettingsScreen
import com.xgy.androidkaraoke.ui.singers.SingersScreen
import com.xgy.androidkaraoke.ui.songs.SongBrowserScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RootScreen(vm: AppViewModel) {
    BackHandler(enabled = vm.overlay != Overlay.None || vm.drillTitle != null) {
        if (vm.overlay != Overlay.None) vm.dismissOverlay() else vm.clearDrill()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                queueCount = vm.queue.size,
                onSearch = { vm.openSearch() },
                onQueue = { vm.openQueue() },
                onWechat = { vm.openRemote() },
                onProfile = { vm.openProfile() },
                onSettings = { vm.openSettings() },
            )
            CategoryTabs(selected = vm.selectedTab, onSelect = vm::selectTab)
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (vm.selectedTab) {
                    MainTab.Songs, MainTab.Charts -> SongBrowserScreen(vm)
                    MainTab.Singers -> SingersScreen(vm)
                    MainTab.Playlists -> PlaylistScreen(vm)
                }
            }
            if (vm.overlay == Overlay.None && vm.nowPlaying != null) {
                NowPlayingBar(vm)
            }
        }
        when (vm.overlay) {
            Overlay.Playback -> PlaybackScreen(vm)
            Overlay.Queue -> QueueScreen(vm)
            Overlay.Settings -> SettingsScreen(vm)
            Overlay.Search -> SearchScreen(vm)
            Overlay.Remote -> RemoteScreen(vm)
            Overlay.Profile -> ProfileScreen(vm)
            Overlay.None -> Unit
        }
        vm.toastMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .background(KaraokeColors.surfaceStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(message, color = KaraokeColors.textPrimary, fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NowPlayingBar(vm: AppViewModel) {
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBar)
                .padding(horizontal = 28.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vm.nowPlaying?.title ?: "", color = KaraokeColors.textPrimary, fontSize = 16.sp)
                Text(
                    if (vm.vocalMode == com.xgy.androidkaraoke.model.VocalMode.Accompaniment) "伴唱" else "原唱",
                    color = KaraokeColors.textSecondary,
                    fontSize = 13.sp,
                )
            }
            com.xgy.androidkaraoke.ui.components.FocusSurface(onClick = { vm.openPlayback() }, corner = 10.dp) {
                Text("回到播放", color = KaraokeColors.textPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
    }
}

private val ColorBar = androidx.compose.ui.graphics.Color(0xF0141414)
