package com.xgy.androidkaraoke.ui.root

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
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
import com.xgy.androidkaraoke.ui.components.LocalTvFocusEnabled
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RootScreen(vm: AppViewModel) {
    val activity = LocalContext.current.findActivity()
    BackHandler {
        when {
            vm.overlay != Overlay.None -> vm.dismissOverlay()
            vm.selectedTab != MainTab.FreeSing -> vm.selectTab(MainTab.FreeSing)
            else -> {
                vm.shutdownPlayback()
                activity?.finishAndRemoveTask()
            }
        }
    }

    val overlay = vm.overlay
    // 整屏目的地：完全替换主壳，禁止与首页叠层
    val fullScreenReplace = overlay == Overlay.NasLibrary ||
        overlay == Overlay.Playback ||
        overlay == Overlay.Guess
    val tabFocus = remember { FocusRequester() }
    val singerTabFocus = remember { FocusRequester() }
    val topFocus = remember { FocusRequester() }
    val entryFocus = remember { FocusRequester() }
    LaunchedEffect(overlay, vm.selectedTab, vm.shouldShowLibraryGuide) {
        if (overlay != Overlay.None) return@LaunchedEffect
        repeat(4) {
            val landed = runCatching { entryFocus.requestFocus(); true }.getOrDefault(false)
            if (landed) return@LaunchedEffect
            delay(20)
        }
    }

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
            // canFocus 不会传给子节点，所以用 LocalTvFocusEnabled 把首页每个按钮关掉。
            CompositionLocalProvider(LocalTvFocusEnabled provides !modalOpen) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        queueCount = vm.queue.size,
                        onSearch = { vm.openSearch() },
                        onQueue = { vm.openQueue() },
                        onWechat = { vm.openPhoneOrder() },
                        onSettings = { vm.openSettings() },
                        topFocus = topFocus,
                        navigateDown = tabFocus,
                    )
                    CategoryTabs(
                        selected = vm.selectedTab,
                        onSelect = vm::selectTab,
                        tabFocus = tabFocus,
                        singerTabFocus = singerTabFocus,
                        navigateUp = topFocus,
                        navigateDown = entryFocus,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        when (vm.selectedTab) {
                            MainTab.FreeSing -> HomeScreen(vm, entryFocus, tabFocus)
                            MainTab.Singers -> SingersScreen(vm, entryFocus, tabFocus)
                            else -> HomeScreen(vm, entryFocus, tabFocus)
                        }
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
        vm.phoneHint?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(30f)
                    .padding(top = 64.dp, end = 28.dp)
                    .background(KaraokeColors.surfaceStrong, RoundedCornerShape(14.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Text(text = msg, color = KaraokeColors.textPrimary, fontSize = 20.sp)
            }
        }

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

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
