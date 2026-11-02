package com.xgy.androidkaraoke.model

import androidx.compose.ui.graphics.Color

enum class MainTab(val title: String, val implemented: Boolean = true) {
    FreeSing("随心唱"),
    Singers("歌星"),
    Hot("热门"),
    Kids("儿童"),
    NasLibrary("NAS曲库"),
    Ranking("排行榜", implemented = false),
    Features("玩法", implemented = false),
}

enum class MediaSource { Mock, Nas, RemoteHttp }

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val isHd: Boolean = false,
    val durationSec: Long = 240,
    val tag: String? = null,
    val coverColor: Color = Color(0xFF3A5A8C),
    val mediaUrl: String? = null,
    val coverUrl: String? = null,
    val source: MediaSource = MediaSource.Nas,
    val relativePath: String? = null,
) {
    val displayTitle: String get() = if (isHd) "$title(HD)" else title
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

enum class Overlay { None, Playback, Guess, Queue, Settings }
