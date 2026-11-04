package com.xgy.androidkaraoke.model

import androidx.compose.ui.graphics.Color

enum class MainTab(val title: String, val implemented: Boolean = true) {
    FreeSing("随心唱"),
    Singers("歌星"),
    // 热门 / 儿童 / 排行榜 / 玩法 已从顶部分类移除
    Hot("热门", implemented = false),
    Kids("儿童", implemented = false),
    Ranking("排行榜", implemented = false),
    Features("玩法", implemented = false),
}

enum class MediaSource { Mock, Nas, Usb, RemoteHttp }

/**
 * 一首歌 = MV + 原唱 + 伴唱（任一轨可缺，播放器按可用轨降级）。
 * - **原唱** = 完整混音（源 MV 音轨），不是 Demucs 人声干声
 * - **伴唱** = 纯伴奏
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
    /** 外挂 LRC 地址（file:// 或群晖 Download URL）。 */
    val lyricsUrl: String? = null,
    /** 热门兜底权重（越大越偏热门）。 */
    val hotScore: Int = 0,
) {
    val displayTitle: String get() = if (isHd) "$title(HD)" else title

    val hasMv: Boolean get() = !mvUrl.isNullOrBlank()
    /** 独立原唱轨（与 MV 不是同一文件）才算「有原唱」。 */
    val hasOriginal: Boolean
        get() = !originalUrl.isNullOrBlank() && originalUrl != mvUrl
    /** 独立伴唱轨（与 MV / 原唱不是同一文件）才算「有伴唱」。 */
    val hasAccompaniment: Boolean
        get() = !accompanimentUrl.isNullOrBlank() &&
            accompanimentUrl != mvUrl &&
            accompanimentUrl != originalUrl
    val hasLyrics: Boolean get() = !lyricsUrl.isNullOrBlank()

    /** 原曲混音与伴奏都独立存在时，才允许切换。 */
    val canToggleVocal: Boolean get() = hasOriginal && hasAccompaniment

    val styleTags: List<String>
        get() = (genres + listOfNotNull(tag)).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun preferredVocalMode(): VocalMode = when {
        hasAccompaniment -> VocalMode.Accompaniment
        hasOriginal -> VocalMode.Original
        else -> VocalMode.Accompaniment
    }

    fun audioUrl(mode: VocalMode): String? = when (mode) {
        VocalMode.Original -> originalUrl ?: accompanimentUrl ?: mvUrl
        VocalMode.Accompaniment -> accompanimentUrl ?: originalUrl ?: mvUrl
    }

    fun anyPlayableUrl(): String? = mvUrl ?: accompanimentUrl ?: originalUrl

    /** 仅保留 MV（或唯一可播轨），用于伴唱轨失败时降级。 */
    fun mvOnlyFallback(): Song {
        val sole = mvUrl ?: originalUrl ?: accompanimentUrl
        return copy(mvUrl = sole, originalUrl = null, accompanimentUrl = null, lyricsUrl = lyricsUrl)
    }
}

data class Singer(
    val id: String,
    val name: String,
    val regionLabel: String? = null,
    val coverColor: Color = Color.Gray,
    val isFeaturedWide: Boolean = false,
)

/** 曲库聚合歌星（NAS/USB 真实元数据）。 */
data class LibraryArtist(
    val name: String,
    val songCount: Int,
    val coverColor: Color,
    val habitWeight: Float = 0f,
) {
    val id: String get() = "artist:${name.lowercase()}"
}

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
    None, Playback, Guess, Queue, Settings, NasLibrary, PhoneOrder, Search, ArtistSongs,
}
