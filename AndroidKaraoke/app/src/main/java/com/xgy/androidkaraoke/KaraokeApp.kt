package com.xgy.androidkaraoke

import android.app.Application
import android.content.Context
import com.xgy.androidkaraoke.library.LibraryStore
import com.xgy.androidkaraoke.library.PlayHistory
import com.xgy.androidkaraoke.nas.HomeNasTls
import com.xgy.androidkaraoke.nas.SecureCredentialStore
import okhttp3.OkHttpClient

class KaraokeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val appContext: Context = app.applicationContext
    val libraryStore = LibraryStore(app)
    val history = PlayHistory(app)
    val credentials = SecureCredentialStore(app)
    val http: OkHttpClient = HomeNasTls.client
}
