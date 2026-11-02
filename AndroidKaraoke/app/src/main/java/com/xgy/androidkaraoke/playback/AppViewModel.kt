package com.xgy.androidkaraoke.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.xgy.androidkaraoke.AppContainer
import com.xgy.androidkaraoke.library.SongGrouper
import com.xgy.androidkaraoke.model.LibrarySource
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.model.Overlay
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.model.SongListFilter
import com.xgy.androidkaraoke.model.VocalMode
import com.xgy.androidkaraoke.nas.NasLibraryClient
import com.xgy.androidkaraoke.remote.LanRemoteServer
import com.xgy.androidkaraoke.remote.PhoneRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(private val container: AppContainer) : ViewModel(), PhoneRemote {
    var selectedTab by mutableStateOf(MainTab.Songs)
        private set
    var songFilter by mutableStateOf(SongListFilter.All)
        private set
    var drillTitle by mutableStateOf<String?>(null)
        private set
    var overlay by mutableStateOf(Overlay.None)
        private set
    var toastMessage by mutableStateOf<String?>(null)
        private set
    var statusMessage by mutableStateOf("正在准备曲库")
        private set

    var catalog by mutableStateOf<List<Song>>(emptyList())
        private set
    val queue: SnapshotStateList<Song> = emptyList<Song>().toMutableStateList()
    var nowPlaying by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentTimeSec by mutableLongStateOf(0L)
        private set
    var durationSec by mutableLongStateOf(0L)
        private set
    var vocalMode by mutableStateOf(VocalMode.Accompaniment)
        private set
    var isFavorite by mutableStateOf(false)
        private set

    var libraries by mutableStateOf<List<LibrarySource>>(emptyList())
        private set
    var editingId by mutableStateOf<String?>(null)
    var formName by mutableStateOf("家庭 NAS")
    var formUrl by mutableStateOf(com.xgy.androidkaraoke.BrandConfig.defaultNasBaseUrl)
    var formPath by mutableStateOf(com.xgy.androidkaraoke.BrandConfig.defaultNasLibraryPath)
    var formUser by mutableStateOf("")
    var formPass by mutableStateOf("")
    var settingsBusy by mutableStateOf(false)
        private set

    var searchQuery by mutableStateOf("")
    var remoteUrl by mutableStateOf<String?>(null)
        private set

    val videoPlayer: ExoPlayer get() = engine.videoPlayer

    val progress: Float
        get() {
            val total = durationSec.coerceAtLeast(1)
            return (currentTimeSec.toFloat() / total).coerceIn(0f, 1f)
        }

    private val engine = PlaybackEngine(container.appContext, container.http)
    private val scanner = NasLibraryClient()
    private val remote = LanRemoteServer()
    private var toastJob: Job? = null
    private var scanJob: Job? = null

    init {
        libraries = container.libraryStore.loadLibraries()
        catalog = container.libraryStore.loadSongs()
        restoreQueue()
        selectLibrary(libraries.first())
        engine.onEnded = { viewModelScope.launch { nextTrack() } }
        engine.onError = { message -> viewModelScope.launch { showToast(message) } }
        remoteUrl = remote.start(this)
        viewModelScope.launch {
            while (true) {
                engine.sync()
                currentTimeSec = engine.currentPosition() / 1000
                durationSec = engine.durationMs() / 1000
                isPlaying = engine.isPlaying()
                delay(400)
            }
        }
        rescan()
    }

    fun selectTab(tab: MainTab) {
        selectedTab = tab
        drillTitle = null
    }

    fun selectFilter(filter: SongListFilter) {
        songFilter = filter
        drillTitle = null
    }

    fun openSinger(name: String) {
        drillTitle = name
        selectedTab = MainTab.Singers
    }

    fun openPlaylist(name: String) {
        drillTitle = name
        selectedTab = MainTab.Playlists
    }

    fun clearDrill() {
        drillTitle = null
    }

    fun visibleSongs(): List<Song> {
        val drilled = drillTitle
        if (drilled != null && selectedTab == MainTab.Singers) {
            return catalog.filter { it.artist == drilled }
        }
        if (drilled != null && selectedTab == MainTab.Playlists) {
            return catalog.filter { it.playlist == drilled }
        }
        return when (selectedTab) {
            MainTab.Charts -> catalog
                .filter { container.history.count(it.id) > 0 }
                .sortedByDescending { container.history.count(it.id) }
            MainTab.Songs -> when (songFilter) {
                SongListFilter.All -> catalog
                SongListFilter.Frequent -> catalog
                    .filter { container.history.count(it.id) > 0 }
                    .sortedByDescending { container.history.count(it.id) }
                SongListFilter.Week -> {
                    val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                    catalog.filter { container.history.lastPlayed(it.id) >= since }
                        .sortedByDescending { container.history.count(it.id) }
                }
            }
            else -> catalog
        }
    }

    fun playlists(): List<String> = catalog.map { it.playlist }.filter { it.isNotBlank() }.distinct().sorted()

    fun favoriteSongs(): List<Song> = catalog.filter { container.history.isFavorite(it.id) }

    fun recentSongs(): List<Song> = catalog
        .filter { container.history.lastPlayed(it.id) > 0 }
        .sortedByDescending { container.history.lastPlayed(it.id) }

    fun searchResults(): List<Song> {
        val query = searchQuery.trim()
        if (query.isEmpty()) return catalog.take(80)
        return catalog.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }.take(80)
    }

    fun openSearch() {
        overlay = Overlay.Search
    }

    fun openQueue() {
        overlay = Overlay.Queue
    }

    fun openRemote() {
        overlay = Overlay.Remote
    }

    fun openProfile() {
        overlay = Overlay.Profile
    }

    fun openSettings() {
        if (libraries.isNotEmpty() && editingId == null) selectLibrary(libraries.first())
        overlay = Overlay.Settings
    }

    fun openPlayback() {
        if (nowPlaying == null) {
            showToast("还没有正在播放的歌")
            return
        }
        overlay = Overlay.Playback
    }

    fun dismissOverlay() {
        overlay = Overlay.None
    }

    fun singNow(song: Song) {
        queue.removeAll { it.id == song.id }
        persistQueue()
        startSong(song)
        showToast("正在播放 ${song.title}")
    }

    fun enqueue(song: Song) {
        if (nowPlaying?.id == song.id) {
            showToast("这首正在播放")
            return
        }
        if (queue.none { it.id == song.id }) queue.add(song)
        if (nowPlaying == null) {
            queue.removeAll { it.id == song.id }
            persistQueue()
            startSong(song)
        } else {
            persistQueue()
            showToast("已点歌：${song.title}")
        }
    }

    fun removeFromQueue(song: Song) {
        queue.removeAll { it.id == song.id }
        persistQueue()
    }

    fun togglePlayPause() {
        engine.togglePause()
        isPlaying = engine.isPlaying()
    }

    fun nextTrack() {
        val upcoming = queue.firstOrNull()
        if (upcoming == null) {
            engine.stop()
            nowPlaying = null
            isPlaying = false
            showToast("没有下一首了")
            return
        }
        queue.removeAt(0)
        persistQueue()
        startSong(upcoming)
    }

    fun replay() {
        engine.seekTo(0)
        if (!engine.isPlaying()) engine.togglePause()
    }

    fun skipPrelude() {
        engine.seekBy(30_000)
    }

    fun toggleVocal() {
        val song = nowPlaying ?: return
        val target = if (vocalMode == VocalMode.Accompaniment) VocalMode.Original else VocalMode.Accompaniment
        if (engine.setVocal(song, target)) {
            vocalMode = target
            showToast(if (target == VocalMode.Accompaniment) "伴唱" else "原唱")
        } else {
            showToast(if (target == VocalMode.Accompaniment) "这首没有伴唱音源" else "这首没有原唱音源")
        }
    }

    fun toggleFavorite() {
        val id = nowPlaying?.id ?: return
        val on = container.history.toggleFavorite(id)
        isFavorite = on
        showToast(if (on) "已收藏" else "已取消收藏")
    }

    fun formatTime(sec: Long): String = "%02d:%02d".format(sec / 60, sec % 60)

    fun selectLibrary(library: LibrarySource) {
        editingId = library.id
        formName = library.name
        formUrl = library.baseUrl
        formPath = library.path
        formUser = library.username
        formPass = ""
    }

    fun saveLibrary() {
        val id = editingId ?: "lib-${System.currentTimeMillis()}"
        val source = LibrarySource(
            id = id,
            name = formName.trim().ifBlank { "曲库" },
            baseUrl = formUrl.trim(),
            path = formPath.trim(),
            username = formUser.trim(),
        )
        container.credentials.saveLibrary(id, source.username, formPass)
        libraries = libraries.filterNot { it.id == id } + source
        container.libraryStore.saveLibraries(libraries)
        editingId = id
        formPass = ""
        showToast("曲库已保存")
        rescan()
    }

    fun addLibrary() {
        editingId = null
        formName = "曲库 ${libraries.size + 1}"
        formPass = ""
        saveLibrary()
    }

    fun removeLibrary(id: String) {
        libraries = libraries.filterNot { it.id == id }
        container.libraryStore.saveLibraries(libraries)
        if (editingId == id) {
            libraries.firstOrNull()?.let { selectLibrary(it) } ?: run { editingId = null }
        }
        rescan()
    }

    fun rescan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            settingsBusy = true
            if (libraries.isEmpty()) {
                statusMessage = "请先在设置里添加曲库"
                settingsBusy = false
                return@launch
            }
            val missingPassword = libraries.any {
                it.username.isNotBlank() && container.credentials.passwordFor(it.id).isBlank()
            }
            if (libraries.all { it.username.isBlank() }) {
                statusMessage = if (catalog.isEmpty()) {
                    "在设置里填写 NAS 账号后扫描曲库。点歌不用登录"
                } else {
                    "已缓存 ${catalog.size} 首。要更新曲库，请在设置里保存 NAS 账号并重新扫描"
                }
                settingsBusy = false
                return@launch
            }
            if (missingPassword && catalog.isEmpty()) {
                statusMessage = "NAS 密码还没保存，请在设置里填写后点保存并扫描"
            }
            try {
                val songs = withContext(Dispatchers.IO) {
                    libraries.flatMap { library ->
                        val password = container.credentials.passwordFor(library.id)
                        if (library.username.isBlank() || password.isBlank()) return@flatMap emptyList()
                        val files = scanner.collect(library, password) { message ->
                            viewModelScope.launch { statusMessage = message }
                        }
                        SongGrouper.group(files).map { song ->
                            song.copy(
                                id = library.id + ":" + song.id,
                                playlist = if (song.playlist == "我的曲库") library.name else song.playlist,
                            )
                        }
                    }
                }
                if (songs.isNotEmpty()) {
                    catalog = songs
                    container.libraryStore.saveSongs(songs)
                    restoreQueue()
                    statusMessage = "曲库 ${songs.size} 首，每首按 MV、原唱、伴唱配对"
                } else if (catalog.isEmpty()) {
                    statusMessage = "没有扫到歌曲。请按「歌手/歌名/歌名.mp4 + 原唱 + 伴唱」放文件"
                } else {
                    statusMessage = "没有扫到新歌，继续用已缓存的 ${catalog.size} 首"
                }
            } catch (e: Exception) {
                statusMessage = e.message ?: "扫描曲库失败"
                showToast(statusMessage)
            } finally {
                settingsBusy = false
            }
        }
    }

    override fun searchSongs(query: String, artist: String): List<Song> {
        return catalog.filter { song ->
            (query.isBlank() || song.title.contains(query, true) || song.artist.contains(query, true)) &&
                (artist.isBlank() || song.artist == artist)
        }
    }

    override fun singers(): List<String> =
        catalog.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()

    override fun queueSnapshot(): List<Song> = queue.toList()

    override fun nowPlaying(): Song? = nowPlaying

    override fun order(id: String, singNow: Boolean): String {
        val song = catalog.firstOrNull { it.id == id } ?: return "找不到这首歌"
        viewModelScope.launch {
            if (singNow) singNow(song) else enqueue(song)
        }
        return if (singNow) "电视开始播放 ${song.title}" else "已加入已点：${song.title}"
    }

    override fun songCount(): Int = catalog.size

    private fun startSong(song: Song) {
        nowPlaying = song
        container.history.record(song.id)
        isFavorite = container.history.isFavorite(song.id)
        val mode = when {
            song.accompanyUrl != null -> VocalMode.Accompaniment
            song.originalUrl != null || song.mvUrl != null -> VocalMode.Original
            else -> VocalMode.Accompaniment
        }
        vocalMode = mode
        engine.play(song, mode)
        isPlaying = true
        currentTimeSec = 0
        overlay = Overlay.Playback
    }

    private fun restoreQueue() {
        val byId = catalog.associateBy { it.id }
        val restored = container.history.queueIds().mapNotNull { byId[it] }
        queue.clear()
        queue.addAll(restored)
    }

    private fun persistQueue() {
        container.history.saveQueue(queue.map { it.id })
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
        remote.stop()
        engine.release()
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
