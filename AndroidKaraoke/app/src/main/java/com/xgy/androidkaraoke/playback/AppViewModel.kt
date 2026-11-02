package com.xgy.androidkaraoke.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.xgy.androidkaraoke.AppContainer
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.model.PlayOrderMode
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.nas.NasConfig
import com.xgy.androidkaraoke.nas.NasEntry
import com.xgy.androidkaraoke.nas.NasEntryKind
import com.xgy.androidkaraoke.nas.NasError
import com.xgy.androidkaraoke.recommend.RecommendationEngine
import com.xgy.androidkaraoke.remote.PhoneRequestServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val player get() = container.player
    private val listeningPrefs get() = container.listeningPrefs

    var selectedTab by mutableStateOf(MainTab.FreeSing)
        private set
    var overlay by mutableStateOf(Overlay.None)
        private set
    var toastMessage by mutableStateOf<String?>(null)
        private set

    var nowPlaying by mutableStateOf<Song?>(null)
        private set
    var nextSong by mutableStateOf<Song?>(MockData.nextSong)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentTimeSec by mutableLongStateOf(0L)
        private set
    var durationSec by mutableLongStateOf(240L)
        private set
    var vocalMode by mutableStateOf(VocalMode.Accompaniment)
        private set
    var orderMode by mutableStateOf(PlayOrderMode.Sequential)
        private set
    var isFavorite by mutableStateOf(false)
        private set
    val queue = mutableStateListOf<Song>()

    /** 偏好变更计数，驱动推荐列表重组。 */
    var prefsEpoch by mutableStateOf(0)
        private set

    /** 手机点歌与推荐可用的曲目目录（mock ∪ NAS 已加载）。 */
    val catalog = mutableStateListOf<Song>().also { it.addAll(MockData.catalog) }

    /** 「为你推荐」：常唱偏好（歌手/曲风），当前播放不作为主信号。 */
    val forYouRecommendations: List<Song>
        get() {
            prefsEpoch // read for recomposition
            return RecommendationEngine.rank(
                candidates = catalog.toList(),
                artistWeights = listeningPrefs.artistWeights(),
                genreWeights = listeningPrefs.genreWeights(),
                excludeIds = emptySet(),
                nowPlayingId = nowPlaying?.id,
                limit = 8,
            )
        }

    /** 「猜你想唱」：同上，排除队列中已有；当前曲仅弱降权。 */
    val guessYouWantRecommendations: List<Song>
        get() {
            prefsEpoch
            val exclude = queue.map { it.id }.toSet() + setOfNotNull(nowPlaying?.id)
            return RecommendationEngine.rank(
                candidates = catalog.toList(),
                artistWeights = listeningPrefs.artistWeights(),
                genreWeights = listeningPrefs.genreWeights(),
                excludeIds = exclude,
                nowPlayingId = nowPlaying?.id,
                limit = 10,
            )
        }

    var nasPath by mutableStateOf(container.nasConfigStore.load().normalizedPath)
        private set
    var nasEntries = mutableStateListOf<NasEntry>()
        private set
    var nasLoading by mutableStateOf(false)
        private set
    var nasError by mutableStateOf<String?>(null)
        private set

    var settingsBaseUrl by mutableStateOf(container.nasConfigStore.load().normalizedBaseUrl)
    var settingsPath by mutableStateOf(container.nasConfigStore.load().normalizedPath)
    var settingsUsername by mutableStateOf(container.credentialStore.username())
    var settingsPassword by mutableStateOf("")
    var settingsBusy by mutableStateOf(false)
        private set
    var settingsMessage by mutableStateOf<String?>(null)
        private set
    var settingsIsError by mutableStateOf(false)
        private set

    var phoneOrderUrl by mutableStateOf<String?>(null)
        private set
    var phoneServerRunning by mutableStateOf(false)
        private set

    val progress: Float
        get() {
            val total = durationSec.coerceAtLeast(1)
            return (currentTimeSec.toFloat() / total).coerceIn(0f, 1f)
        }

    private var toastJob: Job? = null
    private var tickJob: Job? = null
    private var phoneServer: PhoneRequestServer? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = player.isPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                completeCurrentAndNext()
            }
        }
    }

    init {
        val seed = MockData.aiSongs.take(4)
        queue.addAll(seed)
        // 用已点种子建立初始「常唱」画像（演示：民谣/古风/民族等）
        seed.forEach { song ->
            listeningPrefs.recordQueued(song.artist, song.styleTags, song.id)
        }
        bumpPrefs()
        player.addListener(playerListener)
        startPhoneServer()
        startTicker()
    }

    fun selectTab(tab: MainTab) {
        if (!tab.implemented) {
            showToast("「${tab.title}」即将推出")
            return
        }
        selectedTab = tab
    }

    fun openSettings() {
        val cfg = container.nasConfigStore.load()
        settingsBaseUrl = cfg.normalizedBaseUrl
        settingsPath = cfg.normalizedPath
        settingsUsername = container.credentialStore.username()
        settingsPassword = ""
        settingsMessage = null
        overlay = Overlay.Settings
    }

    fun openNasLibrary() {
        overlay = Overlay.NasLibrary
        refreshNas(nasPath)
    }

    fun openQueue() {
        overlay = Overlay.Queue
    }

    fun openPhoneOrder() {
        startPhoneServer()
        phoneOrderUrl = phoneServer?.publicBaseUrl()
        overlay = Overlay.PhoneOrder
        if (phoneOrderUrl == null) {
            showToast("无法获取电视局域网 IP，请确认已连 Wi‑Fi")
        }
    }

    fun openPlayback(song: Song? = null) {
        if (song != null) {
            play(song)
        } else if (nowPlaying == null) {
            play(MockData.nowPlaying)
            nextSong = MockData.nextSong
        } else {
            player.resume()
        }
        overlay = Overlay.Playback
    }

    fun openGuess() {
        overlay = Overlay.Guess
    }

    fun dismissOverlay() {
        overlay = when (overlay) {
            Overlay.Guess -> Overlay.Playback
            Overlay.NasLibrary -> Overlay.Settings
            Overlay.Playback, Overlay.Queue, Overlay.Settings, Overlay.PhoneOrder -> Overlay.None
            Overlay.None -> Overlay.None
        }
    }

    fun addToQueue(song: Song, fromPhone: Boolean = false) {
        if (queue.none { it.id == song.id }) {
            queue.add(song)
        }
        mergeCatalog(listOf(song))
        listeningPrefs.recordQueued(song.artist, song.styleTags, song.id)
        bumpPrefs()
        if (nextSong == null) nextSong = song
        showToast(if (fromPhone) "手机点歌：${song.title}" else "已加入点唱：${song.title}")
    }

    fun removeFromQueue(song: Song) {
        queue.removeAll { it.id == song.id }
        if (nextSong?.id == song.id) nextSong = queue.firstOrNull()
    }

    fun play(song: Song) {
        nowPlaying = song
        isFavorite = listeningPrefs.isFavorite(song.id)
        player.play(song, vocalMode, autoPlay = true)
        isPlaying = true
        durationSec = song.durationSec
        currentTimeSec = 0
    }

    fun togglePlayPause() {
        player.togglePlayPause()
        isPlaying = player.isPlaying()
    }

    fun nextTrack() {
        // 主动切歌：若已听过大半，记为播完偏好；否则不记完成。
        val current = nowPlaying
        if (current != null && durationSec > 0 && currentTimeSec >= durationSec * 0.6) {
            listeningPrefs.recordPlayCompleted(current.artist, current.styleTags, current.id)
            bumpPrefs()
        }
        advanceToNext()
    }

    private fun completeCurrentAndNext() {
        nowPlaying?.let { song ->
            listeningPrefs.recordPlayCompleted(song.artist, song.styleTags, song.id)
            bumpPrefs()
        }
        advanceToNext()
    }

    private fun advanceToNext() {
        val upcoming = when {
            queue.isNotEmpty() -> queue.removeAt(0)
            nextSong != null -> nextSong
            else -> forYouRecommendations.firstOrNull { it.id != nowPlaying?.id }
                ?: catalog.randomOrNull()
        } ?: return
        nextSong = queue.firstOrNull()
            ?: forYouRecommendations.firstOrNull { it.id != upcoming.id }
            ?: catalog.filter { it.id != upcoming.id }.randomOrNull()
        play(upcoming)
    }

    fun replay() {
        player.seekTo(0)
        player.resume()
        currentTimeSec = 0
        isPlaying = true
    }

    fun skipPrelude() {
        player.skipForward(30_000)
        currentTimeSec = (player.currentPositionMs() / 1000)
    }

    fun toggleVocal() {
        vocalMode = if (vocalMode == VocalMode.Accompaniment) VocalMode.Original else VocalMode.Accompaniment
        player.switchVocalMode(vocalMode)
        showToast(if (vocalMode == VocalMode.Accompaniment) "伴唱" else "原唱")
    }

    fun cycleOrder() {
        orderMode = when (orderMode) {
            PlayOrderMode.Sequential -> PlayOrderMode.Shuffle
            PlayOrderMode.Shuffle -> PlayOrderMode.Loop
            PlayOrderMode.Loop -> PlayOrderMode.Sequential
        }
        showToast(
            when (orderMode) {
                PlayOrderMode.Sequential -> "顺序播放"
                PlayOrderMode.Shuffle -> "随机播放"
                PlayOrderMode.Loop -> "单曲循环"
            },
        )
    }

    fun toggleFavorite() {
        val song = nowPlaying ?: return
        isFavorite = !isFavorite
        listeningPrefs.recordFavorite(song.artist, song.styleTags, song.id, isFavorite)
        bumpPrefs()
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
    }

    private fun bumpPrefs() {
        prefsEpoch += 1
    }

    fun formatTime(sec: Long): String {
        val m = sec / 60
        val s = sec % 60
        return "%02d:%02d".format(m, s)
    }

    fun refreshNas(path: String = nasPath) {
        viewModelScope.launch {
            nasLoading = true
            nasError = null
            try {
                container.nasSource.reloadConfig()
                nasPath = path
                val entries = container.nasSource.listDirectory(path)
                nasEntries.clear()
                nasEntries.addAll(entries)
                runCatching {
                    val fromJson = container.nasSource.loadLibraryJson(path)
                    if (fromJson.isNotEmpty()) {
                        mergeCatalog(fromJson)
                    } else {
                        val songs = container.nasSource.listSongs(path)
                        if (songs.isNotEmpty()) mergeCatalog(songs)
                    }
                }
            } catch (e: Exception) {
                nasEntries.clear()
                nasError = (e as? NasError)?.message ?: (e.message ?: "无法连接 NAS")
            } finally {
                nasLoading = false
            }
        }
    }

    fun openNasEntry(entry: NasEntry) {
        viewModelScope.launch {
            when (entry.kind) {
                NasEntryKind.Folder -> {
                    val bundled = runCatching {
                        container.nasSource.songFromFolder(entry.path, entry.name)
                    }.getOrNull()
                    if (bundled != null && bundled.anyPlayableUrl() != null) {
                        mergeCatalog(listOf(bundled))
                        addToQueue(bundled)
                        openPlayback(bundled)
                    } else {
                        refreshNas(entry.path)
                    }
                }
                NasEntryKind.Audio, NasEntryKind.Video -> {
                    val song = Song(
                        id = entry.id,
                        title = entry.name.substringBeforeLast('.'),
                        artist = "NAS",
                        relativePath = entry.path,
                        mvUrl = if (entry.kind == NasEntryKind.Video) entry.absoluteUrl else null,
                        originalUrl = if (entry.kind == NasEntryKind.Audio) entry.absoluteUrl else null,
                        accompanimentUrl = null,
                    )
                    addToQueue(song)
                    openPlayback(song)
                }
                NasEntryKind.Manifest -> showToast("请在设置中配置 library.json 清单")
                else -> showToast("暂不支持打开：${entry.name}")
            }
        }
    }

    fun nasGoUp() {
        val parent = nasPath.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "/")
        refreshNas(if (parent.isEmpty()) "/" else parent)
    }

    fun saveSettingsAndTest(test: Boolean) {
        viewModelScope.launch {
            settingsBusy = true
            settingsMessage = null
            settingsIsError = false
            try {
                val cfg = NasConfig(
                    baseUrlString = settingsBaseUrl.trim(),
                    libraryPath = settingsPath.trim(),
                )
                container.nasConfigStore.save(cfg)
                val passToStore = settingsPassword.ifEmpty {
                    container.credentialStore.password()
                }
                container.credentialStore.save(settingsUsername.trim(), passToStore)
                settingsPassword = ""
                container.nasSource.reloadConfig()
                nasPath = cfg.normalizedPath
                if (test) {
                    val msg = container.nasSource.testConnection()
                    settingsMessage = msg
                    settingsIsError = false
                    showToast(msg)
                } else {
                    settingsMessage = "已保存"
                    showToast("设置已保存")
                }
            } catch (e: Exception) {
                settingsIsError = true
                settingsMessage = (e as? NasError)?.message ?: (e.message ?: "保存失败")
            } finally {
                settingsBusy = false
            }
        }
    }

    private fun mergeCatalog(songs: List<Song>) {
        songs.forEach { song ->
            val idx = catalog.indexOfFirst { it.id == song.id }
            if (idx >= 0) catalog[idx] = song else catalog.add(song)
        }
    }

    private fun startPhoneServer() {
        if (phoneServer?.isRunning == true) {
            phoneServerRunning = true
            phoneOrderUrl = phoneServer?.publicBaseUrl()
            return
        }
        val server = PhoneRequestServer(
            indexHtml = container.phoneOrderHtml,
            catalogProvider = { catalog.toList() },
            queueProvider = { queue.toList() },
            onAddSong = { song ->
                viewModelScope.launch {
                    addToQueue(song, fromPhone = true)
                    if (nowPlaying == null) openPlayback(song)
                }
            },
        )
        phoneServer = server
        server.start()
        phoneServerRunning = true
        phoneOrderUrl = server.publicBaseUrl()
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val dur = player.durationMs()
                if (dur > 0) durationSec = (dur / 1000).coerceAtLeast(1)
                currentTimeSec = (player.currentPositionMs() / 1000).coerceAtLeast(0)
                isPlaying = player.isPlaying()
            }
        }
    }

    fun showToast(message: String) {
        toastMessage = message
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(1800)
            if (toastMessage == message) toastMessage = null
        }
    }

    override fun onCleared() {
        phoneServer?.stop()
        player.removeListener(playerListener)
        tickJob?.cancel()
        super.onCleared()
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}
