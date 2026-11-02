package com.xgy.androidkaraoke

import android.app.Application

class KaraokeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val nasConfigStore = com.xgy.androidkaraoke.nas.NasConfigStore(app)
    val credentialStore = com.xgy.androidkaraoke.nas.SecureCredentialStore(app)
    val nasSource = com.xgy.androidkaraoke.nas.HttpsNasMediaSource(nasConfigStore, credentialStore)
    val microphone = com.xgy.androidkaraoke.audio.StubMicrophoneInput()
}
