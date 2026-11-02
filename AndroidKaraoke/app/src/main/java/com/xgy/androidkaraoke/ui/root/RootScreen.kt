package com.xgy.androidkaraoke.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.xgy.androidkaraoke.ui.home.HomeScreen
import com.xgy.androidkaraoke.ui.hot.HotScreen
import com.xgy.androidkaraoke.ui.kids.KidsScreen
import com.xgy.androidkaraoke.ui.nas.NasLibraryScreen
import com.xgy.androidkaraoke.ui.playback.GuessYouWantScreen
import com.xgy.androidkaraoke.ui.playback.PlaybackScreen
import com.xgy.androidkaraoke.ui.queue.QueueScreen
import com.xgy.androidkaraoke.ui.settings.SettingsScreen
import com.xgy.androidkaraoke.ui.singers.SingersScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RootScreen(vm: AppViewModel) {
    BackHandler(enabled = vm.overlay != Overlay.None) {
        vm.dismissOverlay()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                queueCount = vm.queue.size,
                onSearch = { vm.showToast("搜索即将推出") },
                onQueue = { vm.openQueue() },
                onWechat = { vm.showToast("微信点歌即将推出") },
                onProfile = { vm.showToast("「我的」即将推出") },
                onSettings = { vm.openSettings() },
            )
            CategoryTabs(
                selected = vm.selectedTab,
                onSelect = vm::selectTab,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (vm.selectedTab) {
                    MainTab.FreeSing -> HomeScreen(vm)
                    MainTab.Singers -> SingersScreen(vm)
                    MainTab.Hot -> HotScreen(vm)
                    MainTab.Kids -> KidsScreen(vm)
                    MainTab.NasLibrary -> NasLibraryScreen(vm)
                    else -> HomeScreen(vm)
                }
            }
        }

        when (vm.overlay) {
            Overlay.Playback -> PlaybackScreen(vm)
            Overlay.Guess -> GuessYouWantScreen(vm)
            Overlay.Queue -> QueueScreen(vm)
            Overlay.Settings -> SettingsScreen(vm)
            Overlay.None -> Unit
        }

        vm.toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(KaraokeColors.surfaceStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(text = msg, color = KaraokeColors.textPrimary, fontSize = 18.sp)
            }
        }
    }
}
