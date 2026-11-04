package com.xgy.androidkaraoke

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.playback.AppViewModelFactory
import com.xgy.androidkaraoke.theme.KaraokeTheme
import com.xgy.androidkaraoke.ui.root.RootScreen

class MainActivity : ComponentActivity() {
    private var storageVm: AppViewModel? = null

    private val requestStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            storageVm?.onStorageChanged()
            storageVm?.showToast("已获得存储权限，可扫描 U 盘")
        } else {
            storageVm?.showToast("未授予存储权限，U 盘曲库可能无法读取")
        }
    }

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED,
                Intent.ACTION_MEDIA_UNMOUNTED,
                Intent.ACTION_MEDIA_REMOVED,
                Intent.ACTION_MEDIA_EJECT,
                Intent.ACTION_MEDIA_BAD_REMOVAL,
                -> storageVm?.onStorageChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerMediaReceiver()
        setContent {
            KaraokeTheme {
                KaraokeRoot(
                    onVmReady = { vm ->
                        storageVm = vm
                        maybeRequestStorage(vm)
                    },
                )
            }
        }
    }

    private fun maybeRequestStorage(vm: AppViewModel) {
        // API 28–32：READ_EXTERNAL_STORAGE；API 33+ 媒体权限（电视目标仍以 29 为主）
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                vm.onStorageChanged()
            }
            else -> requestStorage.launch(permission)
        }
    }

    private fun registerMediaReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(
            this,
            mediaReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(mediaReceiver) }
        storageVm = null
        super.onDestroy()
    }
}

@Composable
private fun KaraokeRoot(onVmReady: (AppViewModel) -> Unit) {
    val app = LocalContext.current.applicationContext as KaraokeApp
    val factory = remember(app) { AppViewModelFactory(app.container) }
    val vm: AppViewModel = viewModel(factory = factory)
    LaunchedEffect(vm) { onVmReady(vm) }
    DisposableEffect(Unit) {
        onDispose { }
    }
    RootScreen(vm = vm)
}
