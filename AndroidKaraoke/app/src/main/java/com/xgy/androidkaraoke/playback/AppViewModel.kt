package com.xgy.androidkaraoke.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xgy.androidkaraoke.AppContainer
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.model.PlayOrderMode
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.nas.NasConfig
import com.xgy.androidkaraoke.nas.NasEntry
import com.xgy.androidkaraoke.nas.NasError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppViewModel(private val container: AppContainer) : ViewModel() {
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
    var vocalMode by mutableStateOf(VocalMode.Accompaniment)
        private set
    var orderMode by mutableStateOf(PlayOrderMode.Sequential)
        private set
    var isFavorite by mutableStateOf(false)
        private set
    val queue = mutableStateListOf<Song>().also { it.addAll(MockData.aiSongs.take(4)) }

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

    val progress: Float
        get() {
            val total = (nowPlaying?.durationSec ?: 1).coerceAtLeast(1)
            return (currentTimeSec.toFloat() / total).coerceIn(0f, 1f)
        }

    private var toastJob: Job? = null
    private var tickJob: Job? = null

    fun selectTab(tab: MainTab) {
        if (!tab.implemented) {
            showToast("「${tab.title}」即将推出")
            return
        }
        selectedTab = tab
        if (tab == MainTab.NasLibrary) {
            refreshNas(nasPath)
        }
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

    fun openQueue() {
        overlay = Overlay.Queue
    }

    fun openPlayback(song: Song? = null) {
        if (song != null) {
            play(song)
        } else if (nowPlaying == null) {
            play(MockData.nowPlaying)
            nextSong = MockData.nextSong
        }
        overlay = Overlay.Playback
    }

    fun openGuess() {
        overlay = Overlay.Guess
    }

    fun dismissOverlay() {
        overlay = when (overlay) {
            Overlay.Guess -> Overlay.Playback
            Overlay.Playback, Overlay.Queue, Overlay.Settings -> Overlay.None
            Overlay.None -> Overlay.None
        }
    }

    fun addToQueue(song: Song) {
        queue.add(song)
        if (nextSong == null) nextSong = song
        showToast("已加入点唱：${song.title}")
    }

    fun removeFromQueue(song: Song) {
        queue.removeAll { it.id == song.id }
        if (nextSong?.id == song.id) nextSong = queue.firstOrNull()
    }

    fun play(song: Song) {
        nowPlaying = song
        isPlaying = true
        currentTimeSec = (song.durationSec * 0.1).toLong().coerceAtMost(30)
        startTicker()
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        if (isPlaying) startTicker() else tickJob?.cancel()
    }

    fun nextTrack() {
        val upcoming = nextSong ?: queue.firstOrNull() ?: return
        if (queue.isNotEmpty() && queue.first().id == upcoming.id) {
            queue.removeAt(0)
        }
        nowPlaying = upcoming
        nextSong = queue.firstOrNull() ?: MockData.aiSongs.random()
        currentTimeSec = 0
        isPlaying = true
        startTicker()
    }

    fun replay() {
        currentTimeSec = 0
        isPlaying = true
        startTicker()
    }

    fun skipPrelude() {
        val total = nowPlaying?.durationSec ?: 300
        currentTimeSec = (currentTimeSec + 30).coerceAtMost(total - 1)
    }

    fun toggleVocal() {
        vocalMode = if (vocalMode == VocalMode.Accompaniment) VocalMode.Original else VocalMode.Accompaniment
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
        isFavorite = !isFavorite
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
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
            } catch (e: Exception) {
                nasEntries.clear()
                nasError = (e as? NasError)?.message ?: (e.message ?: "无法连接 NAS")
            } finally {
                nasLoading = false
            }
        }
    }

    fun openNasEntry(entry: NasEntry) {
        when (entry.kind) {
            com.xgy.androidkaraoke.nas.NasEntryKind.Folder -> refreshNas(entry.path)
            com.xgy.androidkaraoke.nas.NasEntryKind.Audio,
            com.xgy.androidkaraoke.nas.NasEntryKind.Video,
            -> {
                val song = Song(
                    id = entry.id,
                    title = entry.name.substringBeforeLast('.'),
                    artist = "NAS",
                    mediaUrl = entry.absoluteUrl,
                    relativePath = entry.path,
                )
                addToQueue(song)
                openPlayback(song)
            }
            else -> showToast("暂不支持打开：${entry.name}")
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
                    // 留空表示不改密；重新填密码才会覆盖。
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

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isPlaying) {
                delay(1000)
                val total = nowPlaying?.durationSec ?: return@launch
                if (currentTimeSec < total) {
                    currentTimeSec += 1
                } else {
                    nextTrack()
                }
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
