package com.xgy.androidkaraoke.model

import androidx.compose.ui.graphics.Color

enum class MainTab(val title: String, val implemented: Boolean = true) {
    FreeSing("随心唱"),
    Singers("歌星"),
    Hot("热门"),
    Kids("儿童"),
    Ranking("排行榜", implemented = false),
    Features("玩法", implemented = false),
}

enum class MediaSource { Mock, Nas, Usb, RemoteHttp }

/**
 * 一首歌 = MV + 原唱 + 伴唱（任一轨可缺，播放器按可用轨降级）。
 * 不强制账号登录。
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val isHd: Boolean = false,
    val durationSec: Long = 240,
    val tag: String? = null,
    /** 曲风 / 风格标签，用于「常唱」偏好推荐。 */
    val genres: List<String> = emptyList(),
    val coverColor: Color = Color(0xFF3A5A8C),
    val coverUrl: String? = null,
    val source: MediaSource = MediaSource.Nas,
    val relativePath: String? = null,
    val mvUrl: String? = null,
    val originalUrl: String? = null,
    val accompanimentUrl: String? = null,
    /** 热门兜底权重（越大越偏热门）。 */
    val hotScore: Int = 0,
) {
    val displayTitle: String get() = if (isHd) "$title(HD)" else title

    val hasMv: Boolean get() = !mvUrl.isNullOrBlank()
    val hasOriginal: Boolean get() = !originalUrl.isNullOrBlank()
    val hasAccompaniment: Boolean get() = !accompanimentUrl.isNullOrBlank()

    val styleTags: List<String>
        get() = (genres + listOfNotNull(tag)).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun audioUrl(mode: VocalMode): String? = when (mode) {
        VocalMode.Original -> originalUrl ?: accompanimentUrl ?: mvUrl
        VocalMode.Accompaniment -> accompanimentUrl ?: originalUrl ?: mvUrl
    }

    fun anyPlayableUrl(): String? = mvUrl ?: accompanimentUrl ?: originalUrl
}

data class Singer(
    val id: String,
    val name: String,
    val regionLabel: String? = null,
    val coverColor: Color = Color.Gray,
    val isFeaturedWide: Boolean = false,
)

enum class HomeTileKind {
    Daily, SongOrder, SingerOrder, CategoryOrder,
    FreeZone, MyQueue, Favorites, KRecommend, Ranking,
}

data class HomeTile(
    val id: String,
    val kind: HomeTileKind,
    val title: String,
    val tint: Color,
    val badgeCount: Int? = null,
    val isWide: Boolean = false,
)

data class PlaylistCard(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val tint: Color,
)

enum class VocalMode { Accompaniment, Original }
enum class PlayOrderMode { Sequential, Shuffle, Loop }

enum class Overlay {
    None, Playback, Guess, Queue, Settings, NasLibrary, PhoneOrder,
}
