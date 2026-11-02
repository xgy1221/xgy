package com.xgy.androidkaraoke

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.playback.AppViewModelFactory
import com.xgy.androidkaraoke.theme.KaraokeTheme
import com.xgy.androidkaraoke.ui.root.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaraokeTheme {
                KaraokeRoot()
            }
        }
    }
}

@Composable
private fun KaraokeRoot() {
    val app = LocalContext.current.applicationContext as KaraokeApp
    val factory = remember(app) { AppViewModelFactory(app.container) }
    val vm: AppViewModel = viewModel(factory = factory)
    RootScreen(vm = vm)
}
