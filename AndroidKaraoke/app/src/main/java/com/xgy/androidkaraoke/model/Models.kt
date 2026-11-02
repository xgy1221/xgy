package com.xgy.androidkaraoke.model

enum class MainTab(val title: String) {
    Songs("点歌"),
    Singers("歌星"),
    Playlists("歌单"),
    Charts("榜单"),
}

enum class SongListFilter(val title: String) {
    All("全部歌曲"),
    Frequent("我的常唱"),
    Week("本周点唱"),
}

enum class VocalMode { Accompaniment, Original }

enum class Overlay { None, Playback, Queue, Settings, Search, Remote, Profile }

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val playlist: String = "我的曲库",
    val mvUrl: String? = null,
    val originalUrl: String? = null,
    val accompanyUrl: String? = null,
    val relativePath: String? = null,
) {
    val hasMv: Boolean get() = !mvUrl.isNullOrBlank()
    val hasOriginal: Boolean get() = !originalUrl.isNullOrBlank()
    val hasAccompaniment: Boolean get() = !accompanyUrl.isNullOrBlank()
}

data class LibrarySource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val path: String,
    val username: String,
) {
    val normalizedBaseUrl: String get() = baseUrl.trim().trimEnd('/')

    val normalizedPath: String
        get() {
            var p = path.trim()
            if (p.isEmpty()) p = "/"
            if (!p.startsWith("/")) p = "/$p"
            while (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
            return p
        }

    companion object {
        fun homeDefault() = LibrarySource(
            id = "home",
            name = "家庭 NAS",
            baseUrl = com.xgy.androidkaraoke.BrandConfig.defaultNasBaseUrl,
            path = com.xgy.androidkaraoke.BrandConfig.defaultNasLibraryPath,
            username = "",
        )
    }
}
