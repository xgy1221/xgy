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
import com.xgy.androidkaraoke.library.LibraryEntry
import com.xgy.androidkaraoke.library.LibraryEntryKind
import com.xgy.androidkaraoke.library.LibrarySource
import com.xgy.androidkaraoke.library.LibrarySourceKind
import com.xgy.androidkaraoke.library.LocalLibraryError
import androidx.compose.ui.graphics.Color
import com.xgy.androidkaraoke.model.LibraryArtist
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.model.PlayOrderMode
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.lyrics.LrcParser
import com.xgy.androidkaraoke.lyrics.LyricsDocument
import com.xgy.androidkaraoke.nas.NasConfig
import com.xgy.androidkaraoke.nas.NasEntryKind
import com.xgy.androidkaraoke.nas.NasError
import com.xgy.androidkaraoke.nas.SynologyFileStation
import com.xgy.androidkaraoke.recommend.RecommendationEngine
import com.xgy.androidkaraoke.remote.LanAddresses
import com.xgy.androidkaraoke.remote.PhoneRequestServer
import com.xgy.androidkaraoke.search.SongSearch
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.absoluteValue

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
    var nextSong by mutableStateOf<Song?>(null)
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

    /**
     * 已索引曲库（仅 USB/NAS/远程真实曲目）。
     * 不再混入 MockData 演示歌；点歌 / 搜索 / 手机页一律只看这里。
     */
    val catalog = mutableStateListOf<Song>()

    val forYouRecommendations: List<Song>
        get() {
            prefsEpoch
            return RecommendationEngine.rank(
                candidates = libraryCatalog,
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
                candidates = libraryCatalog,
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
    /** null = 未在编辑；Nas = 编辑 NAS；Usb = 编辑 U 盘子路径 */
    var settingsEditorKind by mutableStateOf<LibrarySourceKind?>(null)
        private set

    /** 设置主菜单分区：根菜单 / 添加媒体库 / 关于 */
    enum class SettingsSection { Root, Library, About }
    var settingsSection by mutableStateOf(SettingsSection.Root)
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
    var phoneServerError by mutableStateOf<String?>(null)
        private set
    var phoneOrderHost by mutableStateOf<String?>(null)
        private set
    val phoneOrderCandidates = mutableStateListOf<LanAddresses.Candidate>()
    var phoneOrderTip by mutableStateOf<String?>(null)
        private set

    // ——— 搜索 ———
    var searchQuery by mutableStateOf("")
        private set
    /** 空关键词：最近唱过 + 习惯推荐 + 曲库浏览；有关键词：仅匹配真实曲库。 */
    val searchResults: List<Song>
        get() {
            prefsEpoch
            val lib = libraryCatalog
            if (lib.isEmpty()) return emptyList()
            val q = searchQuery.trim()
            if (q.isNotEmpty()) return SongSearch.filter(lib, q)
            val recent = recentSongs
            val ranked = RecommendationEngine.rank(
                candidates = lib,
                artistWeights = listeningPrefs.artistWeights(),
                genreWeights = listeningPrefs.genreWeights(),
                excludeIds = recent.map { it.id }.toSet(),
                nowPlayingId = nowPlaying?.id,
                limit = 60,
            )
            return (recent + ranked).distinctBy { it.id }.take(80)
        }

    var playbackError by mutableStateOf<String?>(null)
        private set

    /** 当前曲目已解析的 LRC；无歌词时为 null（不展示假歌词）。 */
    var lyricsDoc by mutableStateOf<LyricsDocument?>(null)
        private set
    var lyricLineIndex by mutableIntStateOf(-1)
        private set
    /** 当前行「唱到哪」进度 0..1 */
    var lyricLineProgress by mutableFloatStateOf(0f)
        private set
    private var lyricsJob: Job? = null

    var libraryGuideDismissed by mutableStateOf(sourceStore.isLibraryGuideDismissed())
        private set

    /** 尚无真实曲库且用户未关掉引导 */
    val shouldShowLibraryGuide: Boolean
        get() = !libraryGuideDismissed && libraryCatalog.isEmpty()

    val recentSongs: List<Song>
        get() {
            prefsEpoch
            return listeningPrefs.historySongIds().mapNotNull { id ->
                libraryCatalog.firstOrNull { it.id == id }
            }.take(12)
        }

    val favoriteSongs: List<Song>
        get() {
            prefsEpoch
            return listeningPrefs.favoriteIds().mapNotNull { id ->
                libraryCatalog.firstOrNull { it.id == id }
            }
        }

    /** 真实曲库（NAS/USB/远程）；演示 Mock 一律排除。 */
    val libraryCatalog: List<Song>
        get() = catalog.filter { it.source != MediaSource.Mock }

    /** 曲库聚合歌星，习惯权重高的靠前。 */
    val libraryArtists: List<LibraryArtist>
        get() {
            prefsEpoch
            val songs = libraryCatalog
            if (songs.isEmpty()) return emptyList()
            val weights = listeningPrefs.artistWeights()
            return songs
                .groupBy { it.artist.trim().ifBlank { "未知歌星" } }
                .map { (name, list) ->
                    LibraryArtist(
                        name = name,
                        songCount = list.size,
                        coverColor = artistColor(name),
                        habitWeight = weights[name] ?: 0f,
                    )
                }
                .sortedWith(
                    compareByDescending<LibraryArtist> { it.habitWeight }
                        .thenByDescending { it.songCount }
                        .thenBy { it.name },
                )
        }

    val recommendedArtists: List<LibraryArtist>
        get() = libraryArtists.filter { it.habitWeight > 0f }.ifEmpty { libraryArtists.take(8) }

    var artistSearchQuery by mutableStateOf("")
        private set
    var browsingArtist by mutableStateOf<String?>(null)
        private set

    val artistSearchResults: List<LibraryArtist>
        get() {
            val q = artistSearchQuery.trim()
            val all = libraryArtists
            if (q.isEmpty()) return all
            return all.filter {
                it.name.contains(q, ignoreCase = true) ||
                    SongSearch.initials(it.name).contains(q, ignoreCase = true)
            }
        }

    val browsingArtistSongs: List<Song>
        get() {
            val name = browsingArtist ?: return emptyList()
            return libraryCatalog.filter { it.artist.trim().equals(name, ignoreCase = true) }
                .sortedBy { it.title }
        }

    /**
     * 首页迷你唱页目标（仅真实曲库）：
     * 正在播 → 最近唱过 → 习惯推荐 → 曲库第一首可播放。
     * 曲库空时才为空。
     */
    val continueSingSong: Song?
        get() {
            prefsEpoch
            return pickHomePreviewSong()
        }

    var libraryIndexing by mutableStateOf(false)
        private set

    val progress: Float
        get() {
            val total = durationSec.coerceAtLeast(1)
            return (currentTimeSec.toFloat() / total).coerceIn(0f, 1f)
        }

    private var toastJob: Job? = null
    private var tickJob: Job? = null
    private var phoneServer: PhoneRequestServer? = null
    private var knownUsbIds: Set<String> = emptySet()
    private var didBootstrapMiniPlayer = false
    private var playJob: Job? = null
    /** Source error：最多重试 1 次（重连 SID + 必要时降级 MV）。 */
    private var sourceErrorRetries = 0

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = player.isPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                completeCurrentAndNext()
            }
            if (playbackState == Player.STATE_READY) {
                sourceErrorRetries = 0
                if (playbackError != null) playbackError = null
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            viewModelScope.launch { handlePlayerError(error) }
        }
    }

    init {
        migrateLegacyNasIfNeeded()
        // 先清掉已知演示歌历史，避免「最近唱过」残留小半等
        listeningPrefs.removeSongIds(LEGACY_DEMO_SONG_IDS)
        bumpPrefs()
        reloadSources(scanUsb = true, announceUsb = false)
        indexEnabledLibraries()
        player.addListener(playerListener)
        // 后台起服务即可；扫码 IP 提示只在打开「扫码点歌」时展示，不在首页 toast
        startPhoneServer(refreshIp = true, announceTips = false)
        startTicker()
    }

    private fun migrateLegacyNasIfNeeded() {
        val existing = sourceStore.load()
        // 把旧的 /volume1/music 映射为共享路径 /music
        var changed = false
        val migrated = existing.map { src ->
            if (src.kind != LibrarySourceKind.Nas) return@map src
            val mapped = SynologyFileStation.normalizeLibraryPathForSave(
                src.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath,
            )
            if (mapped != src.nasLibraryPath) {
                changed = true
                src.copy(nasLibraryPath = mapped)
            } else {
                src
            }
        }
        if (changed) {
            sourceStore.save(migrated)
            migrated.firstOrNull { it.kind == LibrarySourceKind.Nas }?.let { nas ->
                container.nasConfigStore.save(
                    NasConfig(
                        baseUrlString = nas.nasBaseUrl ?: BrandConfig.defaultNasBaseUrl,
                        libraryPath = nas.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath,
                    ),
                )
            }
        }
        if (sourceStore.load().any { it.kind == LibrarySourceKind.Nas }) return
        val cfg = container.nasConfigStore.load()
        val path = SynologyFileStation.normalizeLibraryPathForSave(cfg.normalizedPath)
        sourceStore.upsert(
            LibrarySource(
                id = "nas-default",
                kind = LibrarySourceKind.Nas,
                displayName = "NAS 默认",
                enabled = true,
                nasBaseUrl = cfg.normalizedBaseUrl,
                nasLibraryPath = path,
            ),
        )
        if (path != cfg.normalizedPath) {
            container.nasConfigStore.save(cfg.copy(libraryPath = path))
        }
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

    fun openSettings(section: SettingsSection = SettingsSection.Root) {
        reloadSources(scanUsb = true, announceUsb = false)
        settingsEditorKind = null
        editingSourceId = null
        settingsMessage = null
        nasWizardStep = NasWizardStep.Connect
        settingsUsername = container.credentialStore.username()
        settingsPassword = ""
        settingsSection = section
        overlay = Overlay.Settings
    }

    fun openSettingsLibrary() = openSettings(SettingsSection.Library)

    fun openSettingsAbout() {
        settingsSection = SettingsSection.About
        settingsEditorKind = null
        settingsMessage = null
    }

    fun backToSettingsRoot() {
        cancelSourceEditor()
        settingsSection = SettingsSection.Root
    }

    fun beginAddNas() {
        settingsSection = SettingsSection.Library
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
                showToast(msg)
                // 进入目录选择后清掉连接成功文案，避免与列目录错误叠在一起
                settingsMessage = null
                settingsIsError = false
                nasWizardStep = NasWizardStep.PickDirectory
                // 群晖：从共享列表根开始（/ → list_share），不要进 /volume1
                refreshDirPicker("/")
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
            settingsMessage = null
            try {
                val normalized = SynologyFileStation.normalizeLibraryPathForSave(path)
                dirPickPath = if (normalized == "/" || path == "/") "/" else normalized
                // 若用户残留 /volume1/music，映射为 /music 再列
                val folders = container.nasSource.listFolders(dirPickPath)
                dirPickFolders.clear()
                dirPickFolders.addAll(folders)
                if (folders.isEmpty() && dirPickPath != "/") {
                    dirPickError = null // 空目录允许「选为曲库根」
                }
            } catch (e: Exception) {
                dirPickFolders.clear()
                dirPickError = (e as? NasError)?.message ?: (e.message ?: "无法列出目录")
            } finally {
                dirPickLoading = false
            }
        }
    }

    fun dirPickerGoUp() {
        if (dirPickPath == "/" || dirPickPath.isBlank()) {
            showToast("已在共享列表根")
            return
        }
        val parent = dirPickPath.trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "/")
        refreshDirPicker(if (parent.isEmpty()) "/" else parent)
    }

    fun dirPickerEnter(folderPath: String) {
        refreshDirPicker(SynologyFileStation.normalizeLibraryPathForSave(folderPath))
    }

    /** 步骤 2：确认当前浏览路径为曲库根目录并保存来源。 */
    fun confirmNasLibraryDirectory() {
        viewModelScope.launch {
            settingsBusy = true
            settingsMessage = null
            try {
                var path = SynologyFileStation.normalizeLibraryPathForSave(dirPickPath.ifBlank { "/" })
                if (path == "/" || path.isBlank()) {
                    settingsIsError = true
                    dirPickError = "请先进入一个共享文件夹（如 music → 路径 /music）再选为曲库根"
                    return@launch
                }
                settingsPath = path
                dirPickPath = path
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
                dirPickError = null
                reindexLibrariesAndBootstrap()
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
            reindexLibrariesAndBootstrap()
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

    fun updateArtistSearchQuery(q: String) {
        artistSearchQuery = q
    }

    fun openArtistSearch() {
        if (libraryCatalog.isEmpty()) {
            showToast("请先添加媒体库")
            openSettingsLibrary()
            return
        }
        artistSearchQuery = ""
        browsingArtist = null
        overlay = Overlay.ArtistSongs
    }

    fun openArtistSongs(artistName: String) {
        browsingArtist = artistName
        artistSearchQuery = ""
        overlay = Overlay.ArtistSongs
    }

    fun clearBrowsingArtist() {
        browsingArtist = null
    }

    fun dismissLibraryGuide() {
        libraryGuideDismissed = true
        sourceStore.setLibraryGuideDismissed(true)
    }

    fun openPhoneOrder() {
        overlay = Overlay.PhoneOrder
        startPhoneServer(refreshIp = true, announceTips = true)
    }

    /** 在多个局域网地址间轮换（家用 Wi‑Fi vs VPN/虚拟网卡）。 */
    fun cyclePhoneOrderAddress() {
        refreshPhoneLanCandidates()
        if (phoneOrderCandidates.isEmpty()) {
            phoneOrderUrl = null
            phoneOrderTip = null
            showToast("未发现局域网 IP，请确认电视已连 Wi‑Fi")
            return
        }
        val hosts = phoneOrderCandidates.map { it.host }
        val current = phoneOrderHost
        val idx = hosts.indexOf(current).let { if (it < 0) 0 else (it + 1) % hosts.size }
        applyPhoneOrderHost(hosts[idx], announce = true)
    }

    private fun refreshPhoneLanCandidates() {
        val list = LanAddresses.listIpv4()
        phoneOrderCandidates.clear()
        phoneOrderCandidates.addAll(list)
    }

    private fun applyPhoneOrderHost(host: String?, announce: Boolean = false) {
        phoneOrderHost = host
        phoneOrderUrl = host?.let { "http://$it:${PhoneRequestServer.DEFAULT_PORT}/" }
        phoneOrderTip = LanAddresses.tipIfSuspicious(host, phoneOrderCandidates.toList())
        if (announce && host != null) {
            showToast("已切换：$host")
        }
    }

    fun openPlayback(song: Song? = null) {
        when {
            song != null -> {
                // 同一首歌：不重新 prepare，首页迷你唱页 → 全屏无缝续播
                if (nowPlaying?.id == song.id) {
                    player.resume()
                } else {
                    play(song)
                }
            }
            nowPlaying != null -> player.resume()
            queue.isNotEmpty() -> play(queue.removeAt(0))
            else -> {
                showToast("没有可播放的歌曲")
                return
            }
        }
        overlay = Overlay.Playback
    }

    /** 首页迷你唱页点击：进入全屏演唱页。 */
    fun openContinueSinging() {
        val song = continueSingSong
        if (song == null) {
            showToast("还没有唱过的曲库歌曲")
            if (libraryCatalog.isEmpty()) openSettingsLibrary() else openSearch()
            return
        }
        openPlayback(song)
    }

    fun openGuess() {
        overlay = Overlay.Guess
    }

    fun dismissOverlay() {
        when (overlay) {
            Overlay.Guess -> overlay = Overlay.Playback
            Overlay.NasLibrary -> overlay = Overlay.Settings
            Overlay.ArtistSongs -> {
                if (browsingArtist != null && artistSearchQuery.isEmpty()) {
                    // 从某歌星歌曲列表返回歌星搜索/列表
                    browsingArtist = null
                } else {
                    browsingArtist = null
                    artistSearchQuery = ""
                    overlay = Overlay.None
                }
            }
            Overlay.Settings -> {
                when {
                    settingsEditorKind == LibrarySourceKind.Nas &&
                        nasWizardStep == NasWizardStep.PickDirectory -> {
                        nasWizardStep = NasWizardStep.Connect
                        settingsMessage = null
                    }
                    settingsEditorKind != null -> cancelSourceEditor()
                    settingsSection != SettingsSection.Root -> settingsSection = SettingsSection.Root
                    else -> overlay = Overlay.None
                }
            }
            Overlay.Playback, Overlay.Queue, Overlay.PhoneOrder, Overlay.Search -> overlay = Overlay.None
            Overlay.None -> Unit
        }
    }

    fun addToQueue(song: Song, fromPhone: Boolean = false) {
        if (song.source == MediaSource.Mock) {
            showToast("演示歌曲不可点，请使用自己的曲库")
            return
        }
        if (song.anyPlayableUrl().isNullOrBlank()) {
            showToast("无法点歌「${song.title}」：缺少可播放文件")
            return
        }
        // 仅收纳真实曲库曲目
        mergeCatalog(listOf(song))
        listeningPrefs.recordQueued(song.artist, song.styleTags, song.id)
        bumpPrefs()

        if (nowPlaying?.id == song.id) {
            showToast("正在演唱：${song.title}")
            return
        }
        if (queue.any { it.id == song.id }) {
            showToast("已在已点队列：${song.title}")
            return
        }

        if (nowPlaying == null) {
            // 空闲时直接开唱；不把「正在播」留在已点列表里
            play(song)
            showToast(
                if (fromPhone) "手机点歌开始：${song.title}"
                else "开始播放：${song.title}",
            )
        } else {
            queue.add(song)
            if (nextSong == null) nextSong = song
            showToast(
                if (fromPhone) "手机点歌：${song.title}"
                else "已加入已点：${song.title}",
            )
        }
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
        if (song.source == MediaSource.Mock) {
            playbackError = "演示歌曲不可播放，请使用自己的曲库"
            showToast(playbackError!!)
            return
        }
        if (song.anyPlayableUrl().isNullOrBlank() && song.relativePath.isNullOrBlank()) {
            playbackError = "无法播放「${song.title}」：缺少 MV / 原唱 / 伴唱"
            showToast(playbackError!!)
            return
        }
        playbackError = null
        sourceErrorRetries = 0
        clearLyrics()
        // 进入「正在播」即移出已点队列（主流 K 歌：已点 = 待唱）
        queue.removeAll { it.id == song.id }
        nextSong = queue.firstOrNull()
        nowPlaying = song
        isFavorite = listeningPrefs.isFavorite(song.id)
        isPlaying = true
        durationSec = song.durationSec
        currentTimeSec = 0
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val resolved = resolvePlayable(song)
                if (resolved.anyPlayableUrl().isNullOrBlank()) {
                    playbackError = "无法播放「${song.title}」：文件不存在或 NAS 未连接"
                    showToast(playbackError!!)
                    isPlaying = false
                    return@launch
                }
                nowPlaying = resolved
                player.play(resolved, vocalMode, autoPlay = true)
                player.setPreferEmbeddedSubtitles(!resolved.hasLyrics)
                isPlaying = true
                loadLyricsFor(resolved)
            } catch (e: Exception) {
                playbackError = friendlyPlayError(e)
                showToast(playbackError!!)
                isPlaying = false
            }
        }
    }

    private fun clearLyrics() {
        lyricsJob?.cancel()
        lyricsDoc = null
        lyricLineIndex = -1
        lyricLineProgress = 0f
    }

    private fun loadLyricsFor(song: Song) {
        lyricsJob?.cancel()
        if (!song.hasLyrics) {
            clearLyrics()
            return
        }
        lyricsJob = viewModelScope.launch {
            val text = runCatching {
                when (song.source) {
                    MediaSource.Nas -> container.nasSource.loadLyricsText(song)
                    MediaSource.Usb -> container.localSource.loadLyricsText(song)
                    else -> null
                }
            }.getOrNull()
            if (nowPlaying?.id != song.id) return@launch
            val doc = text?.let { LrcParser.parse(it) }?.takeUnless { it.isEmpty }
            lyricsDoc = doc
            if (doc == null) {
                lyricLineIndex = -1
                lyricLineProgress = 0f
                // 无有效 LRC 时允许内嵌字幕兜底
                player.setPreferEmbeddedSubtitles(true)
            } else {
                player.setPreferEmbeddedSubtitles(false)
                syncLyricsToPosition(player.currentPositionMs())
            }
        }
    }

    private fun syncLyricsToPosition(positionMs: Long) {
        val doc = lyricsDoc ?: return
        lyricLineIndex = doc.lineIndexAt(positionMs)
        lyricLineProgress = if (lyricLineIndex >= 0) doc.lineProgressAt(positionMs) else 0f
    }

    private suspend fun resolvePlayable(song: Song): Song {
        return when (song.source) {
            MediaSource.Nas -> container.nasSource.resolveForPlayback(song)
            else -> song
        }
    }

    private suspend fun handlePlayerError(error: androidx.media3.common.PlaybackException) {
        val song = nowPlaying ?: run {
            playbackError = friendlyPlaybackException(error)
            showToast(playbackError!!)
            return
        }
        val canRetryNas = song.source == MediaSource.Nas && sourceErrorRetries < 1
        if (canRetryNas) {
            sourceErrorRetries++
            try {
                showToast("播放异常，正在重连 NAS…")
                container.nasSource.forceRelogin()
                var resolved = container.nasSource.resolveForPlayback(song)
                // 伴唱/原唱轨失败：降级为单路 MV（或唯一可播轨）
                val hadSeparateTracks = resolved.hasOriginal || resolved.hasAccompaniment
                if (resolved.hasMv || hadSeparateTracks) {
                    resolved = resolved.mvOnlyFallback()
                    if (hadSeparateTracks) showToast("音轨失败，改为播放 MV")
                }
                if (resolved.anyPlayableUrl().isNullOrBlank()) {
                    throw NasError.NotFound
                }
                nowPlaying = resolved
                player.play(resolved, vocalMode, autoPlay = true)
                return
            } catch (e: Exception) {
                playbackError = friendlyPlayError(e)
                showToast(playbackError!!)
                isPlaying = false
                return
            }
        }
        playbackError = friendlyPlaybackException(error)
        showToast(playbackError!!)
        isPlaying = false
    }

    private fun friendlyPlayError(e: Exception): String {
        return when (e) {
            is NasError.Unauthorized, is NasError.DsmNeedsCredentials ->
                "播放失败：NAS 登录失效，请到设置重连 NAS"
            is NasError.NotFound ->
                "播放失败：文件不存在，请检查曲库路径"
            is NasError.Timeout ->
                "播放失败：连接 NAS 超时，请检查网络"
            is NasError.Forbidden ->
                "播放失败：没有权限访问该文件"
            is NasError -> e.message ?: "播放失败：无法连接 NAS"
            else -> "播放失败：${e.message?.take(60) ?: "未知错误"}（可尝试设置里重连 NAS）"
        }
    }

    private fun friendlyPlaybackException(error: androidx.media3.common.PlaybackException): String {
        val detail = (error.cause?.message ?: error.message ?: "").lowercase()
        return when {
            "401" in detail || "403" in detail || "sid" in detail || "unauthor" in detail ->
                "播放失败：NAS 登录失效，请到设置重连 NAS"
            "404" in detail || "not found" in detail || "no such file" in detail ->
                "播放失败：文件不存在，请检查曲库路径"
            "ssl" in detail || "cert" in detail || "handshake" in detail ->
                "播放失败：无法建立安全连接，请检查 NAS 地址"
            "timeout" in detail || "timed out" in detail ->
                "播放失败：连接 NAS 超时，请检查网络"
            "source" in detail ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "播放失败：无法打开媒体（请重连 NAS，或确认文件仍在）"
            else ->
                "播放失败：无法打开媒体（请重连 NAS，或确认文件仍在）"
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
        isPlaying = player.isPlaying()
    }

    fun nextTrack() {
        val current = nowPlaying
        if (current != null) {
            // 切歌视为唱完/跳过：写入最近唱过，且不再留在已点
            listeningPrefs.recordPlayCompleted(current.artist, current.styleTags, current.id)
            bumpPrefs()
            queue.removeAll { it.id == current.id }
        }
        advanceToNext()
    }

    private fun completeCurrentAndNext() {
        nowPlaying?.let { song ->
            listeningPrefs.recordPlayCompleted(song.artist, song.styleTags, song.id)
            bumpPrefs()
            queue.removeAll { it.id == song.id }
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
                val upcoming = when {
                    queue.isNotEmpty() -> queue.random().also { pick ->
                        queue.removeAll { it.id == pick.id }
                    }
                    else -> null
                }
                if (upcoming == null) {
                    finishQueue()
                    return
                }
                nextSong = queue.randomOrNull()
                play(upcoming)
            }
            PlayOrderMode.Sequential -> {
                val upcoming = if (queue.isNotEmpty()) queue.removeAt(0) else null
                if (upcoming == null) {
                    finishQueue()
                    return
                }
                nextSong = queue.firstOrNull()
                play(upcoming)
            }
        }
    }

    private fun finishQueue() {
        nextSong = null
        nowPlaying = null
        isPlaying = false
        player.pause()
        showToast("队列已播完")
    }

    private fun artistColor(name: String): Color {
        val palette = listOf(
            Color(0xFF2E5BB8), Color(0xFF8C5973), Color(0xFF267A66),
            Color(0xFFB26659), Color(0xFF596699), Color(0xFF66594D),
            Color(0xFF4066B3), Color(0xFF597366), Color(0xFF8C4D66),
            Color(0xFF4D6680),
        )
        val idx = name.hashCode().absoluteValue % palette.size
        return palette[idx]
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
                                        NasEntryKind.Lyrics -> LibraryEntryKind.Lyrics
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
                    val (title, artist) = com.xgy.androidkaraoke.nas.KaraokeFileNames.parseTitleArtist(entry.name)
                    val media = if (source.kind == LibrarySourceKind.Usb) MediaSource.Usb else MediaSource.Nas
                    val known = catalog.firstOrNull { it.id == entry.id }
                    // 扁平原文件：视频只挂 MV；纯音频只挂可播轨，不误报有原唱/有伴唱
                    val song = if (entry.kind == LibraryEntryKind.Video && uri != null) {
                        Song(
                            id = entry.id,
                            title = title,
                            artist = artist,
                            relativePath = entry.path,
                            source = media,
                            mvUrl = uri,
                            originalUrl = null,
                            accompanimentUrl = null,
                            lyricsUrl = known?.lyricsUrl,
                        )
                    } else {
                        Song(
                            id = entry.id,
                            title = title,
                            artist = artist,
                            relativePath = entry.path,
                            source = media,
                            mvUrl = null,
                            originalUrl = uri,
                            accompanimentUrl = null,
                            lyricsUrl = known?.lyricsUrl,
                        )
                    }
                    addToQueue(song)
                    openPlayback(song)
                }
                LibraryEntryKind.Manifest -> showToast("请使用 library.json 清单（MV/原唱/伴唱）")
                LibraryEntryKind.Lyrics -> showToast("歌词会随曲包一起播放（.lrc）")
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
                        // 选完 /music 根后立刻扫进 catalog，驱动首页迷你唱页自动播
                        reindexLibrariesAndBootstrap()
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
                        reindexLibrariesAndBootstrap()
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
            if (song.source == MediaSource.Mock) return@forEach
            val idx = catalog.indexOfFirst { it.id == song.id }
            if (idx >= 0) catalog[idx] = song else catalog.add(song)
        }
        pruneListeningToLibrary()
        maybeBootstrapMiniPlayer()
    }

    private fun pruneListeningToLibrary() {
        val valid = libraryCatalog.map { it.id }.toSet()
        if (valid.isEmpty()) {
            listeningPrefs.removeSongIds(LEGACY_DEMO_SONG_IDS)
        } else {
            listeningPrefs.retainOnlySongIds(valid)
        }
        bumpPrefs()
    }

    private fun isPlayable(song: Song): Boolean = !song.anyPlayableUrl().isNullOrBlank()

    private fun pickHomePreviewSong(): Song? {
        nowPlaying?.takeIf(::isPlayable)?.let { return it }
        recentSongs.firstOrNull(::isPlayable)?.let { return it }
        forYouRecommendations.firstOrNull(::isPlayable)?.let { return it }
        return libraryCatalog.firstOrNull(::isPlayable)
    }

    /**
     * 曲库索引完成后自动预览。
     * 曲库仍空时不标记完成，等 NAS/USB 扫完再试。
     */
    private fun maybeBootstrapMiniPlayer() {
        if (nowPlaying != null) {
            didBootstrapMiniPlayer = true
            Log.i(TAG, "mini bootstrap skip: already playing ${nowPlaying?.title}")
            return
        }
        if (didBootstrapMiniPlayer) {
            Log.i(TAG, "mini bootstrap skip: already attempted")
            return
        }
        if (overlay == Overlay.Playback || overlay == Overlay.Guess) {
            Log.i(TAG, "mini bootstrap skip: overlay=$overlay")
            return
        }
        if (libraryIndexing) {
            Log.i(TAG, "mini bootstrap wait: still indexing")
            return
        }
        if (libraryCatalog.isEmpty()) {
            Log.i(TAG, "mini bootstrap wait: catalog empty")
            return
        }
        val song = pickHomePreviewSong()
        if (song == null) {
            Log.w(TAG, "mini bootstrap skip: no playable song in ${libraryCatalog.size} catalog entries")
            return
        }
        didBootstrapMiniPlayer = true
        Log.i(TAG, "mini bootstrap play: ${song.title} / ${song.artist} mv=${song.hasMv}")
        play(song)
    }

    /** 后台把已启用 NAS/USB 来源扫进 catalog，供搜索/迷你唱页使用。 */
    fun reindexLibrariesAndBootstrap() {
        // 允许在曲库保存后再自动预览一次
        if (nowPlaying == null) didBootstrapMiniPlayer = false
        indexEnabledLibraries()
    }

    private fun indexEnabledLibraries() {
        viewModelScope.launch {
            libraryIndexing = true
            var total = 0
            try {
                val sources = librarySources.filter { it.enabled }.toList()
                Log.i(TAG, "index libraries: ${sources.size} enabled source(s)")
                for (source in sources) {
                    when (source.kind) {
                        LibrarySourceKind.Nas -> {
                            val result = runCatching {
                                applyNasConfig(source)
                                val path = source.nasLibraryPath ?: BrandConfig.defaultNasLibraryPath
                                val fromJson = container.nasSource.loadLibraryJson(path)
                                val songs = fromJson.ifEmpty { container.nasSource.listSongs(path) }
                                Log.i(TAG, "NAS index path=$path json=${fromJson.size} flat=${songs.size}")
                                if (songs.isNotEmpty()) mergeCatalog(songs)
                                songs.size
                            }
                            total += result.getOrElse {
                                Log.w(TAG, "NAS index failed: ${it.message}")
                                0
                            }
                        }
                        LibrarySourceKind.Usb -> {
                            val root = source.localRootPath
                            if (root == null) continue
                            val result = runCatching {
                                val browse = source.browseRoot
                                val fromJson = container.localSource.loadLibraryJson(browse)
                                val songs = fromJson.ifEmpty { container.localSource.listSongs(browse) }
                                    .map { it.copy(source = MediaSource.Usb) }
                                Log.i(TAG, "USB index path=$browse json=${fromJson.size} flat=${songs.size}")
                                if (songs.isNotEmpty()) mergeCatalog(songs)
                                songs.size
                            }
                            total += result.getOrElse {
                                Log.w(TAG, "USB index failed: ${it.message}")
                                0
                            }
                        }
                    }
                }
            } finally {
                libraryIndexing = false
                Log.i(TAG, "index done: catalog=${libraryCatalog.size} indexedBatch=$total")
                maybeBootstrapMiniPlayer()
            }
        }
    }

    companion object {
        private const val TAG = "KaraokeHome"

        /** 旧版 MockData 写入 SharedPreferences 的演示曲 id，需主动清掉。 */
        private val LEGACY_DEMO_SONG_IDS = setOf(
            "xiaoban", "chahua", "yongchun", "shimian", "shexiang",
            "xihai", "hongyan", "fenmo", "qinghuaci",
            "daoxiang", "dongfengpo", "2002", "luhua", "zui", "lang",
            "gufeng2", "minyao2", "liuxing1", "caoyuan",
        )
    }

    private fun startPhoneServer(refreshIp: Boolean = false, announceTips: Boolean = false) {
        refreshPhoneLanCandidates()
        if (phoneOrderHost == null || refreshIp ||
            phoneOrderCandidates.none { it.host == phoneOrderHost }
        ) {
            applyPhoneOrderHost(phoneOrderCandidates.firstOrNull()?.host)
        } else {
            applyPhoneOrderHost(phoneOrderHost)
        }

        val existing = phoneServer
        if (existing?.isRunning == true) {
            phoneServerRunning = true
            phoneServerError = null
            if (announceTips && phoneOrderUrl == null) {
                phoneServerError = "无法获取电视局域网 IP，请确认已连 Wi‑Fi"
                showToast(phoneServerError!!)
            }
            return
        }

        phoneServerError = null
        val server = PhoneRequestServer(
            indexHtml = container.phoneOrderHtml,
            catalogProvider = { libraryCatalog },
            queueProvider = { queue.toList() },
            onAddSong = { song ->
                viewModelScope.launch {
                    addToQueue(song, fromPhone = true)
                }
            },
        )
        phoneServer = server
        server.start()
        phoneServerRunning = true
        viewModelScope.launch {
            delay(350)
            if (phoneServer !== server) return@launch
            if (!server.isRunning) {
                phoneServerRunning = false
                phoneServerError = "点歌服务启动失败（端口 ${PhoneRequestServer.DEFAULT_PORT} 可能被占用）"
                phoneOrderUrl = null
                if (announceTips) showToast(phoneServerError!!)
            } else {
                phoneServerRunning = true
                phoneServerError = null
                if (announceTips && phoneOrderUrl == null) {
                    phoneServerError = "无法获取电视局域网 IP，请确认已连 Wi‑Fi"
                    showToast(phoneServerError!!)
                }
            }
        }
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
                syncLyricsToPosition(player.currentPositionMs())
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
