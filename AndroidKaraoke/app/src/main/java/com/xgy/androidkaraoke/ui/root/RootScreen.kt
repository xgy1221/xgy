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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.AmbientBackground
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.home.HomeScreen
import com.xgy.androidkaraoke.ui.nas.NasLibraryScreen
import com.xgy.androidkaraoke.ui.playback.GuessYouWantScreen
import com.xgy.androidkaraoke.ui.playback.PlaybackScreen
import com.xgy.androidkaraoke.ui.queue.QueueScreen
import com.xgy.androidkaraoke.ui.remote.PhoneOrderScreen
import com.xgy.androidkaraoke.ui.search.SearchScreen
import com.xgy.androidkaraoke.ui.settings.SettingsScreen
import com.xgy.androidkaraoke.ui.singers.ArtistSongsScreen
import com.xgy.androidkaraoke.ui.singers.SingersScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RootScreen(vm: AppViewModel) {
    BackHandler(enabled = vm.overlay != Overlay.None) {
        vm.dismissOverlay()
    }

    val overlay = vm.overlay
    // 整屏目的地：完全替换主壳，禁止与首页叠层
    val fullScreenReplace = overlay == Overlay.NasLibrary ||
        overlay == Overlay.Playback ||
        overlay == Overlay.Guess

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()

        if (fullScreenReplace) {
            when (overlay) {
                Overlay.NasLibrary -> NasLibraryScreen(vm)
                Overlay.Playback -> PlaybackScreen(vm)
                Overlay.Guess -> GuessYouWantScreen(vm)
                else -> Unit
            }
        } else {
            val modalOpen = overlay != Overlay.None
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = !modalOpen },
            ) {
                TopBar(
                    queueCount = vm.queue.size,
                    onSearch = { vm.openSearch() },
                    onQueue = { vm.openQueue() },
                    onWechat = { vm.openPhoneOrder() },
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
                        else -> HomeScreen(vm)
                    }
                }
            }

            when (overlay) {
                Overlay.Queue -> QueueScreen(vm)
                Overlay.Settings -> SettingsScreen(vm)
                Overlay.PhoneOrder -> PhoneOrderScreen(vm)
                Overlay.Search -> SearchScreen(vm)
                Overlay.ArtistSongs -> ArtistSongsScreen(vm)
                else -> Unit
            }
        }

        // zIndex 须高于 BlockingOverlay(10)，否则搜索/已点里的 toast 被挡住
        vm.toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
                    .padding(bottom = 48.dp)
                    .background(KaraokeColors.surfaceStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(text = msg, color = KaraokeColors.textPrimary, fontSize = 18.sp)
            }
        }
    }
}
