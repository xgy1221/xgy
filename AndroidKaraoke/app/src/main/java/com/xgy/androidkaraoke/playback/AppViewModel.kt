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
import com.xgy.androidkaraoke.BrandConfig
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.library.LibraryEntry
import com.xgy.androidkaraoke.library.LibraryEntryKind
import com.xgy.androidkaraoke.library.LibrarySource
import com.xgy.androidkaraoke.library.LibrarySourceKind
import com.xgy.androidkaraoke.library.LocalLibraryError
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.model.PlayOrderMode
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.nas.NasConfig
import com.xgy.androidkaraoke.nas.NasEntryKind
import com.xgy.androidkaraoke.nas.NasError
import com.xgy.androidkaraoke.recommend.RecommendationEngine
import com.xgy.androidkaraoke.remote.PhoneRequestServer
import com.xgy.androidkaraoke.search.SongSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val player get() = container.player
    private val listeningPrefs get() = container.listeningPrefs
    private val sourceStore get() = container.librarySourceStore

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

    /** 手机点歌与推荐可用的曲目目录（mock ∪ 已加载曲库）。 */
    val catalog = mutableStateListOf<Song>().also { it.addAll(MockData.catalog) }

    val forYouRecommendations: List<Song>
        get() {
            prefsEpoch
            return RecommendationEngine.rank(
                candidates = catalog.toList(),
                artistWeights = listeningPrefs.artistWeights(),
                genreWeights = listeningPrefs.genreWeights(),
                excludeIds = emptySet(),
                nowPlayingId = nowPlaying?.id,
                limit = 8,
            )
        }

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

    // ——— 曲库多来源 ———
    val librarySources = mutableStateListOf<LibrarySource>()
    var activeSourceId by mutableStateOf<String?>(null)
        private set
    var libraryPath by mutableStateOf("")
        private set
    var libraryEntries = mutableStateListOf<LibraryEntry>()
        private set
    var libraryLoading by mutableStateOf(false)
        private set
    var libraryError by mutableStateOf<String?>(null)
        private set

    val activeSource: LibrarySource?
        get() = librarySources.firstOrNull { it.id == activeSourceId }
            ?: librarySources.firstOrNull { it.enabled }

    // 兼容旧 UI 命名
    val nasPath: String get() = libraryPath
    val nasEntries: List<LibraryEntry> get() = libraryEntries
    val nasLoading: Boolean get() = libraryLoading
    val nasError: String? get() = libraryError

    // ——— 设置编辑（NAS 添加/编辑） ———
    var editingSourceId by mutableStateOf<String?>(null)
    var settingsDisplayName by mutableStateOf("NAS")
    var settingsBaseUrl by mutableStateOf(BrandConfig.defaultNasBaseUrl)
    var settingsPath by mutableStateOf(BrandConfig.defaultNasLibraryPath)
    var settingsUsername by mutableStateOf("")
    var settingsPassword by mutableStateOf("")
    var settingsRelativeMusicPath by mutableStateOf("")
    var settingsBusy by mutableStateOf(false)
        private set
    var settingsMessage by mutableStateOf<String?>(null)
        private set
    var settingsIsError by mutableStateOf(false)
        private set
    /** null = 列表；Nas = 编辑 NAS；Usb = 编辑 U 盘子路径 */
    var settingsEditorKind by mutableStateOf<LibrarySourceKind?>(null)
        private set

    /** NAS 添加向导：先连接，再选目录 */
    enum class NasWizardStep { Connect, PickDirectory }
    var nasWizardStep by mutableStateOf(NasWizardStep.Connect)
        private set
    var dirPickPath by mutableStateOf("/")
        private set
    val dirPickFolders = mutableStateListOf<com.xgy.androidkaraoke.nas.NasEntry>()
    var dirPickLoading by mutableStateOf(false)
        private set
    var dirPickError by mutableStateOf<String?>(null)
        private set

    var phoneOrderUrl by mutableStateOf<String?>(null)
        private set
    var phoneServerRunning by mutableStateOf(false)
        private set

    // ——— 搜索 ———
    var searchQuery by mutableStateOf("")
        private set
    val searchResults: List<Song>
        get() = SongSearch.filter(catalog.toList(), searchQuery)

    var playbackError by mutableStateOf<String?>(null)
        private set

    var libraryGuideDismissed by mutableStateOf(sourceStore.isLibraryGuideDismissed())
        private set

    /** 尚无非 Mock 曲库且用户未关掉引导 */
    val shouldShowLibraryGuide: Boolean
        get() = !libraryGuideDismissed && catalog.none { it.source != MediaSource.Mock }

    val recentSongs: List<Song>
        get() {
            prefsEpoch
            return listeningPrefs.historySongIds().mapNotNull { id ->
                catalog.firstOrNull { it.id == id }
            }.take(12)
        }

    val favoriteSongs: List<Song>
        get() {
            prefsEpoch
            return listeningPrefs.favoriteIds().mapNotNull { id ->
                catalog.firstOrNull { it.id == id }
            }
        }

    val progress: Float
        get() {
            val total = durationSec.coerceAtLeast(1)
            return (currentTimeSec.toFloat() / total).coerceIn(0f, 1f)
        }

    private var toastJob: Job? = null
    private var tickJob: Job? = null
    private var phoneServer: PhoneRequestServer? = null
    private var knownUsbIds: Set<String> = emptySet()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = player.isPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                completeCurrentAndNext()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            playbackError = "播放失败：${error.message?.take(80) ?: "媒体无法加载"}"
            showToast(playbackError!!)
        }
    }

    init {
        migrateLegacyNasIfNeeded()
        reloadSources(scanUsb = true, announceUsb = false)
        val seed = MockData.aiSongs.take(4)
        queue.addAll(seed)
        seed.forEach { song ->
            listeningPrefs.recordQueued(song.artist, song.styleTags, song.id)
        }
        bumpPrefs()
        player.addListener(playerListener)
        startPhoneServer()
        startTicker()
    }

    private fun migrateLegacyNasIfNeeded() {
        val existing = sourceStore.load()
        if (existing.any { it.kind == LibrarySourceKind.Nas }) return
        val cfg = container.nasConfigStore.load()
        sourceStore.upsert(
            LibrarySource(
                id = "nas-default",
                kind = LibrarySourceKind.Nas,
                displayName = "NAS 默认",
                enabled = true,
                nasBaseUrl = cfg.normalizedBaseUrl,
                nasLibraryPath = cfg.normalizedPath,
            ),
        )
    }

    fun reloadSources(scanUsb: Boolean = true, announceUsb: Boolean = false) {
        val beforeUsb = knownUsbIds
        if (scanUsb) {
            container.usbScanner.syncUsbSources(sourceStore)
        }
        val list = sourceStore.load()
        librarySources.clear()
        librarySources.addAll(list)
        val usbNow = list.filter { it.kind == LibrarySourceKind.Usb && it.localRootPath != null }
            .map { it.id }.toSet()
        if (announceUsb) {
            val added = usbNow - beforeUsb
            val removed = beforeUsb - usbNow
            when {
                added.isNotEmpty() -> {
                    val names = list.filter { it.id in added }.joinToString { it.displayName }
                    showToast("已检测到 U 盘：$names")
                }
                removed.isNotEmpty() -> showToast("U 盘已拔出，相关曲库暂不可用")
            }
        }
        knownUsbIds = usbNow
        if (activeSourceId == null || librarySources.none { it.id == activeSourceId }) {
            activeSourceId = sourceStore.activeSourceId()
                ?: librarySources.firstOrNull { it.enabled }?.id
        }
    }

    /** 媒体挂载广播 / 权限授予后调用。 */
    fun onStorageChanged() {
        reloadSources(scanUsb = true, announceUsb = true)
        val active = activeSource
        if (active?.kind == LibrarySourceKind.Usb && overlay == Overlay.NasLibrary) {
            if (active.localRootPath == null) {
                libraryError = "U 盘已拔出，请重新插入或切换到来源"
                libraryEntries.clear()
            } else {
                refreshLibrary(active.browseRoot)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        if (!tab.implemented) {
            showToast("「${tab.title}」即将推出")
            return
        }
        selectedTab = tab
    }

    fun openSettings() {
        reloadSources(scanUsb = true, announceUsb = false)
        settingsEditorKind = null
        editingSourceId = null
        settingsMessage = null
        nasWizardStep = NasWizardStep.Connect
        settingsUsername = container.credentialStore.username()
        settingsPassword = ""
        overlay = Overlay.Settings
    }

    fun beginAddNas() {
        editingSourceId = null
        settingsEditorKind = LibrarySourceKind.Nas
        nasWizardStep = NasWizardStep.Connect
        settingsDisplayName = "NAS"
        settingsBaseUrl = BrandConfig.defaultNasBaseUrl
        settingsPath = BrandConfig.defaultNasLibraryPath
        settingsUsername = container.credentialStore.username()
        settingsPassword = ""
        settingsMessage = null
        settingsIsError = false
        dirPickFolders.clear()
        dirPickError = null
        dirPickPath = "/"
    }

    fun beginEditSource(source: LibrarySource) {
        editingSourceId = source.id
        settingsEditorKind = source.kind
        settingsDisplayName = source.displayName
        settingsBaseUrl = source.nasBaseUrl ?: BrandConfig.defaultNasBaseUrl
        settingsPath = source.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath
        settingsRelativeMusicPath = source.relativeMusicPath
        settingsUsername = container.credentialStore.username()
        settingsPassword = ""
        settingsMessage = null
        settingsIsError = false
        if (source.kind == LibrarySourceKind.Nas) {
            nasWizardStep = NasWizardStep.Connect
            dirPickPath = source.nasLibraryPath ?: "/"
            dirPickFolders.clear()
            dirPickError = null
        }
    }

    fun cancelSourceEditor() {
        settingsEditorKind = null
        editingSourceId = null
        settingsMessage = null
        nasWizardStep = NasWizardStep.Connect
        dirPickFolders.clear()
        dirPickError = null
    }

    fun backToNasConnect() {
        nasWizardStep = NasWizardStep.Connect
        settingsMessage = null
        dirPickError = null
    }

    /** 步骤 1：保存认证并探测连接，成功后进入目录选择。 */
    fun connectNasThenPickDirectory() {
        viewModelScope.launch {
            settingsBusy = true
            settingsMessage = null
            settingsIsError = false
            try {
                val cfg = NasConfig(
                    baseUrlString = settingsBaseUrl.trim(),
                    libraryPath = "/",
                )
                container.nasConfigStore.save(cfg)
                val passToStore = settingsPassword.ifEmpty {
                    container.credentialStore.password()
                }
                container.credentialStore.save(settingsUsername.trim(), passToStore)
                container.nasSource.reloadConfig()
                val msg = container.nasSource.probeConnection("/")
                settingsMessage = msg
                settingsIsError = false
                showToast(msg)
                nasWizardStep = NasWizardStep.PickDirectory
                // 群晖：优先从 /volume1 或曲库默认父目录开始选
                val start = listOf(
                    BrandConfig.defaultNasLibraryPath.trimEnd('/')
                        .substringBeforeLast('/', missingDelimiterValue = "/"),
                    "/volume1",
                    "/",
                ).first { it.isNotBlank() }
                refreshDirPicker(start)
            } catch (e: Exception) {
                settingsIsError = true
                settingsMessage = (e as? NasError)?.message ?: (e.message ?: "连接失败")
            } finally {
                settingsBusy = false
            }
        }
    }

    fun refreshDirPicker(path: String = dirPickPath) {
        viewModelScope.launch {
            dirPickLoading = true
            dirPickError = null
            try {
                dirPickPath = path
                val folders = container.nasSource.listFolders(path)
                dirPickFolders.clear()
                dirPickFolders.addAll(folders)
            } catch (e: Exception) {
                dirPickFolders.clear()
                dirPickError = (e as? NasError)?.message ?: (e.message ?: "无法列出目录")
            } finally {
                dirPickLoading = false
            }
        }
    }

    fun dirPickerGoUp() {
        val parent = dirPickPath.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "/")
        refreshDirPicker(if (parent.isEmpty()) "/" else parent)
    }

    fun dirPickerEnter(folderPath: String) {
        refreshDirPicker(folderPath)
    }

    /** 步骤 2：确认当前浏览路径为曲库根目录并保存来源。 */
    fun confirmNasLibraryDirectory() {
        viewModelScope.launch {
            settingsBusy = true
            try {
                val path = dirPickPath.ifBlank { "/" }
                settingsPath = path
                val id = editingSourceId ?: ("nas-" + UUID.randomUUID().toString().take(8))
                val source = LibrarySource(
                    id = id,
                    kind = LibrarySourceKind.Nas,
                    displayName = settingsDisplayName.trim().ifBlank { "NAS" },
                    enabled = true,
                    nasBaseUrl = settingsBaseUrl.trim(),
                    nasLibraryPath = path,
                )
                sourceStore.upsert(source)
                container.nasConfigStore.save(
                    NasConfig(
                        baseUrlString = settingsBaseUrl.trim(),
                        libraryPath = path,
                    ),
                )
                container.nasSource.reloadConfig()
                activeSourceId = id
                sourceStore.setActiveSourceId(id)
                reloadSources(scanUsb = false)
                showToast("已添加曲库：$path")
                dismissLibraryGuide()
                settingsEditorKind = null
                nasWizardStep = NasWizardStep.Connect
                settingsMessage = null
            } catch (e: Exception) {
                settingsIsError = true
                settingsMessage = e.message ?: "保存失败"
            } finally {
                settingsBusy = false
            }
        }
    }

    fun scanUsbNow() {
        reloadSources(scanUsb = true, announceUsb = true)
        val usb = librarySources.filter { it.kind == LibrarySourceKind.Usb }
        if (usb.isEmpty()) {
            showToast("未发现可移除存储，请插入 U 盘后重试")
        } else {
            val mounted = usb.count { it.localRootPath != null }
            showToast("已扫描：${usb.size} 个 U 盘来源（已挂载 $mounted）")
        }
    }

    fun setSourceEnabled(id: String, enabled: Boolean) {
        sourceStore.setEnabled(id, enabled)
        reloadSources(scanUsb = false)
        showToast(if (enabled) "已启用" else "已禁用")
    }

    fun removeSource(id: String) {
        sourceStore.remove(id)
        if (activeSourceId == id) activeSourceId = null
        reloadSources(scanUsb = false)
        showToast("已删除来源")
    }

    fun openLibrary(source: LibrarySource? = null) {
        val target = source ?: activeSource
        if (target == null) {
            showToast("请先添加曲库来源")
            return
        }
        if (!target.enabled) {
            showToast("该来源已禁用，请先启用")
            return
        }
        if (target.kind == LibrarySourceKind.Usb && target.localRootPath == null) {
            showToast("U 盘未挂载，请插入后再浏览")
            return
        }
        activeSourceId = target.id
        sourceStore.setActiveSourceId(target.id)
        // 同步 NAS 配置给 HttpsNasMediaSource
        if (target.kind == LibrarySourceKind.Nas) {
            val cfg = NasConfig(
                baseUrlString = target.nasBaseUrl ?: BrandConfig.defaultNasBaseUrl,
                libraryPath = target.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath,
            )
            container.nasConfigStore.save(cfg)
            container.nasSource.reloadConfig()
        }
        overlay = Overlay.NasLibrary
        refreshLibrary(target.browseRoot)
    }

    fun openNasLibrary() = openLibrary(activeSource)

    fun openQueue() {
        overlay = Overlay.Queue
    }

    fun openSearch() {
        searchQuery = ""
        overlay = Overlay.Search
    }

    fun updateSearchQuery(q: String) {
        searchQuery = q
    }

    fun dismissLibraryGuide() {
        libraryGuideDismissed = true
        sourceStore.setLibraryGuideDismissed(true)
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
        when (overlay) {
            Overlay.Guess -> overlay = Overlay.Playback
            Overlay.NasLibrary -> overlay = Overlay.Settings
            Overlay.Settings -> {
                when {
                    settingsEditorKind == LibrarySourceKind.Nas &&
                        nasWizardStep == NasWizardStep.PickDirectory -> {
                        nasWizardStep = NasWizardStep.Connect
                        settingsMessage = null
                    }
                    settingsEditorKind != null -> cancelSourceEditor()
                    else -> overlay = Overlay.None
                }
            }
            Overlay.Playback, Overlay.Queue, Overlay.PhoneOrder, Overlay.Search -> overlay = Overlay.None
            Overlay.None -> Unit
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
        showToast("已移除：${song.title}")
    }

    fun moveQueueToTop(song: Song) {
        val existing = queue.firstOrNull { it.id == song.id } ?: return
        queue.removeAll { it.id == song.id }
        queue.add(0, existing)
        nextSong = existing
        showToast("已置顶：${song.title}")
    }

    fun clearQueue() {
        queue.clear()
        nextSong = null
        showToast("已清空已点队列")
    }

    fun play(song: Song) {
        if (song.anyPlayableUrl().isNullOrBlank()) {
            playbackError = "无法播放「${song.title}」：缺少 MV / 原唱 / 伴唱"
            showToast(playbackError!!)
            return
        }
        playbackError = null
        nowPlaying = song
        isFavorite = listeningPrefs.isFavorite(song.id)
        try {
            player.play(song, vocalMode, autoPlay = true)
            isPlaying = true
            durationSec = song.durationSec
            currentTimeSec = 0
        } catch (e: Exception) {
            playbackError = "播放失败：${e.message ?: "未知错误"}"
            showToast(playbackError!!)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
        isPlaying = player.isPlaying()
    }

    fun nextTrack() {
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
        when (orderMode) {
            PlayOrderMode.Loop -> {
                val cur = nowPlaying ?: return
                replay()
                showToast("单曲循环：${cur.title}")
                return
            }
            PlayOrderMode.Shuffle -> {
                val pool = (queue.toList() + catalog.toList()).distinctBy { it.id }
                    .filter { it.id != nowPlaying?.id }
                val upcoming = pool.randomOrNull() ?: return
                queue.removeAll { it.id == upcoming.id }
                nextSong = queue.randomOrNull()
                    ?: forYouRecommendations.firstOrNull { it.id != upcoming.id }
                play(upcoming)
            }
            PlayOrderMode.Sequential -> {
                val upcoming = when {
                    queue.isNotEmpty() -> queue.removeAt(0)
                    nextSong != null && nextSong?.id != nowPlaying?.id -> nextSong
                    else -> forYouRecommendations.firstOrNull { it.id != nowPlaying?.id }
                        ?: catalog.firstOrNull { it.id != nowPlaying?.id }
                } ?: run {
                    showToast("队列已播完")
                    return
                }
                nextSong = queue.firstOrNull()
                    ?: forYouRecommendations.firstOrNull { it.id != upcoming.id }
                    ?: catalog.firstOrNull { it.id != upcoming.id }
                play(upcoming)
            }
        }
    }

    fun clearPlaybackError() {
        playbackError = null
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

    fun refreshNas(path: String = libraryPath) = refreshLibrary(path)

    fun refreshLibrary(path: String = libraryPath) {
        val source = activeSource ?: run {
            libraryError = "未选择曲库来源"
            return
        }
        viewModelScope.launch {
            libraryLoading = true
            libraryError = null
            try {
                libraryPath = path
                when (source.kind) {
                    LibrarySourceKind.Nas -> {
                        applyNasConfig(source)
                        val entries = container.nasSource.listDirectory(path)
                        libraryEntries.clear()
                        libraryEntries.addAll(
                            entries.map { e ->
                                LibraryEntry(
                                    id = e.id,
                                    name = e.name,
                                    path = e.path,
                                    kind = when (e.kind) {
                                        NasEntryKind.Folder -> LibraryEntryKind.Folder
                                        NasEntryKind.Audio -> LibraryEntryKind.Audio
                                        NasEntryKind.Video -> LibraryEntryKind.Video
                                        NasEntryKind.Manifest -> LibraryEntryKind.Manifest
                                        NasEntryKind.Other -> LibraryEntryKind.Other
                                    },
                                    playableUri = e.absoluteUrl,
                                    sourceId = source.id,
                                )
                            },
                        )
                        runCatching {
                            val fromJson = container.nasSource.loadLibraryJson(path)
                            if (fromJson.isNotEmpty()) mergeCatalog(fromJson)
                            else {
                                val songs = container.nasSource.listSongs(path)
                                if (songs.isNotEmpty()) mergeCatalog(songs)
                            }
                        }
                    }
                    LibrarySourceKind.Usb -> {
                        val root = source.localRootPath
                            ?: throw LocalLibraryError.NotFound("U 盘未挂载")
                        val browse = if (path.isBlank()) source.browseRoot else path
                        libraryPath = browse
                        val entries = container.localSource.listDirectory(root, browse)
                        libraryEntries.clear()
                        libraryEntries.addAll(entries.map { it.copy(sourceId = source.id) })
                        runCatching {
                            val fromJson = container.localSource.loadLibraryJson(browse)
                            if (fromJson.isNotEmpty()) {
                                mergeCatalog(fromJson.map { it.copy(source = MediaSource.Usb) })
                            } else {
                                val songs = container.localSource.listSongs(browse)
                                if (songs.isNotEmpty()) {
                                    mergeCatalog(songs.map { it.copy(source = MediaSource.Usb) })
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                libraryEntries.clear()
                libraryError = when (e) {
                    is NasError -> e.message
                    is LocalLibraryError -> e.message
                    else -> e.message ?: "无法浏览曲库"
                }
            } finally {
                libraryLoading = false
            }
        }
    }

    fun openNasEntry(entry: LibraryEntry) = openLibraryEntry(entry)

    fun openLibraryEntry(entry: LibraryEntry) {
        val source = activeSource ?: return
        viewModelScope.launch {
            when (entry.kind) {
                LibraryEntryKind.Folder -> {
                    val bundled = when (source.kind) {
                        LibrarySourceKind.Nas -> runCatching {
                            container.nasSource.songFromFolder(entry.path, entry.name)
                        }.getOrNull()
                        LibrarySourceKind.Usb -> runCatching {
                            container.localSource.songFromFolder(entry.path, entry.name)
                                ?.copy(source = MediaSource.Usb, artist = source.displayName)
                        }.getOrNull()
                    }
                    if (bundled != null && bundled.anyPlayableUrl() != null) {
                        mergeCatalog(listOf(bundled))
                        addToQueue(bundled)
                        openPlayback(bundled)
                    } else {
                        refreshLibrary(entry.path)
                    }
                }
                LibraryEntryKind.Audio, LibraryEntryKind.Video -> {
                    val uri = entry.playableUri
                    val song = Song(
                        id = entry.id,
                        title = entry.name.substringBeforeLast('.'),
                        artist = source.displayName,
                        relativePath = entry.path,
                        source = if (source.kind == LibrarySourceKind.Usb) MediaSource.Usb else MediaSource.Nas,
                        mvUrl = if (entry.kind == LibraryEntryKind.Video) uri else null,
                        originalUrl = if (entry.kind == LibraryEntryKind.Audio) uri else null,
                        accompanimentUrl = null,
                    )
                    addToQueue(song)
                    openPlayback(song)
                }
                LibraryEntryKind.Manifest -> showToast("请使用 library.json 清单（MV/原唱/伴唱）")
                else -> showToast("暂不支持打开：${entry.name}")
            }
        }
    }

    fun nasGoUp() = libraryGoUp()

    fun libraryGoUp() {
        val source = activeSource ?: return
        when (source.kind) {
            LibrarySourceKind.Nas -> {
                val parent = libraryPath.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "/")
                refreshLibrary(if (parent.isEmpty()) "/" else parent)
            }
            LibrarySourceKind.Usb -> {
                val root = source.browseRoot.trimEnd('/')
                val current = libraryPath.trimEnd('/')
                if (current == root || current.length <= root.length) {
                    showToast("已在曲库根目录")
                    return
                }
                val parent = current.substringBeforeLast('/', missingDelimiterValue = root)
                refreshLibrary(if (parent.length < root.length) root else parent)
            }
        }
    }

    private fun applyNasConfig(source: LibrarySource) {
        val cfg = NasConfig(
            baseUrlString = source.nasBaseUrl ?: BrandConfig.defaultNasBaseUrl,
            libraryPath = source.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath,
        )
        container.nasConfigStore.save(cfg)
        container.nasSource.reloadConfig()
    }

    fun saveSettingsAndTest(test: Boolean) {
        viewModelScope.launch {
            settingsBusy = true
            settingsMessage = null
            settingsIsError = false
            try {
                when (settingsEditorKind) {
                    LibrarySourceKind.Nas, null -> {
                        val id = editingSourceId ?: ("nas-" + UUID.randomUUID().toString().take(8))
                        val source = LibrarySource(
                            id = id,
                            kind = LibrarySourceKind.Nas,
                            displayName = settingsDisplayName.trim().ifBlank { "NAS" },
                            enabled = true,
                            nasBaseUrl = settingsBaseUrl.trim(),
                            nasLibraryPath = settingsPath.trim(),
                        )
                        sourceStore.upsert(source)
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
                        activeSourceId = id
                        sourceStore.setActiveSourceId(id)
                        reloadSources(scanUsb = false)
                        if (test) {
                            val msg = container.nasSource.testConnection()
                            settingsMessage = msg
                            settingsIsError = false
                            showToast(msg)
                        } else {
                            settingsMessage = "已保存"
                            showToast("NAS 来源已保存")
                            settingsEditorKind = null
                        }
                    }
                    LibrarySourceKind.Usb -> {
                        val id = editingSourceId ?: return@launch
                        val prev = sourceStore.load().firstOrNull { it.id == id } ?: return@launch
                        sourceStore.upsert(
                            prev.copy(
                                displayName = settingsDisplayName.trim().ifBlank { prev.displayName },
                                relativeMusicPath = settingsRelativeMusicPath.trim().trim('/'),
                            ),
                        )
                        reloadSources(scanUsb = false)
                        settingsMessage = "已保存"
                        showToast("U 盘来源已保存")
                        settingsEditorKind = null
                    }
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
