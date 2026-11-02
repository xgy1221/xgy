package com.xgy.androidkaraoke

import android.app.Application
import com.xgy.androidkaraoke.playback.KaraokePlayer

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
    val player = KaraokePlayer(app)
    val listeningPrefs = com.xgy.androidkaraoke.recommend.ListeningPreferenceStore(app)
    val phoneOrderHtml: String = runCatching {
        app.assets.open("phone_order.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrElse {
        "<html><body><h1>手机点歌页缺失</h1></body></html>"
    }
}
