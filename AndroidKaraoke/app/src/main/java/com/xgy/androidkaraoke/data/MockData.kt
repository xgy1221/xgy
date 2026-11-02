package com.xgy.androidkaraoke.data

import androidx.compose.ui.graphics.Color
import com.xgy.androidkaraoke.model.HomeTile
import com.xgy.androidkaraoke.model.HomeTileKind
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.PlaylistCard
import com.xgy.androidkaraoke.model.Singer
import com.xgy.androidkaraoke.model.Song

object MockData {
    val homeTiles = listOf(
        HomeTile("daily", HomeTileKind.Daily, "每日推荐", Color(0xFF267A66), isWide = true),
        HomeTile("song", HomeTileKind.SongOrder, "歌曲点歌", Color(0xFF3373F2)),
        HomeTile("singer", HomeTileKind.SingerOrder, "歌手点歌", Color(0xFFD93347)),
        HomeTile("category", HomeTileKind.CategoryOrder, "分类点歌", Color(0xFF2647B8)),
        HomeTile("free", HomeTileKind.FreeZone, "免费专区·即刻开唱", Color(0xFF2E6BE0)),
        HomeTile("queue", HomeTileKind.MyQueue, "我的点唱", Color(0xFFE85A8C), badgeCount = 4),
        HomeTile("fav", HomeTileKind.Favorites, "收藏歌曲", Color(0xFFEBB847)),
        HomeTile("krec", HomeTileKind.KRecommend, "K歌推荐", Color(0xFF73B2F2)),
        HomeTile("rank", HomeTileKind.Ranking, "排行榜", Color(0xFF40B873)),
    )

    val featuredSingers = listOf(
        Singer("naying", "那英", coverColor = Color(0xFF8C5973), isFeaturedWide = true),
        Singer("eason", "陈奕迅", coverColor = Color(0xFFD9D1C7), isFeaturedWide = true),
    )

    val singerGrid = listOf(
        Singer("jolin", "蔡依林", coverColor = Color(0xFFBF738C)),
        Singer("wangqi", "王琪", coverColor = Color(0xFF668CB2)),
        Singer("timi", "卓依婷", coverColor = Color(0xFFB28066)),
        Singer("jiang", "蒋大为", coverColor = Color(0xFF73668C)),
        Singer("daolang", "刀郎", coverColor = Color(0xFF597366)),
    )

    val singerRegions = listOf(
        PlaylistCard("cn-m", "大陆男歌手", tint = Color(0xFF1F408C)),
        PlaylistCard("cn-f", "大陆女歌手", tint = Color(0xFF33A6BF)),
        PlaylistCard("hk-m", "港台男歌手", tint = Color(0xFFE68C33)),
        PlaylistCard("hk-f", "港台女歌手", tint = Color(0xFF3373E6)),
        PlaylistCard("group", "华语组合", tint = Color(0xFFD93340)),
        PlaylistCard("intl", "国外歌手", tint = Color(0xFF33A659)),
    )

    private fun nasSong(
        id: String,
        title: String,
        artist: String,
        color: Color,
        hd: Boolean = false,
        tag: String? = null,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        isHd = hd,
        tag = tag,
        coverColor = color,
        mediaUrl = "nas://mock/$id.mp3",
        source = MediaSource.Nas,
        relativePath = "/volume1/music/mock/$id.mp3",
    )

    val aiSongs = listOf(
        nasSong("xiaoban", "小半", "陈粒", Color(0xFF8C5973)),
        nasSong("chahua", "茶花开了", "王睿卓", Color(0xFFB26659), hd = true),
        nasSong("yongchun", "咏春", "七朵组合", Color(0xFFD98C4D), hd = true),
        nasSong("shimian", "失眠", "Suki刘舒妤", Color(0xFF596699)),
        nasSong("shexiang", "奢香夫人", "凤凰传奇", Color(0xFFBF4D59)),
    )

    val hotPosters = listOf(
        PlaylistCard("douyin", "抖音热歌精选", "#火爆全网的歌#", tint = Color(0xFFA65973)),
        PlaylistCard("china", "唱响中国之歌", "歌声致敬新时代", tint = Color(0xFF4066B3)),
        PlaylistCard("onehit", "1人1首成名曲", "这些歌，百唱不厌", badge = "经典珍藏", tint = Color(0xFF66594D)),
        PlaylistCard("duet", "经典对唱", "把我唱给你听", tint = Color(0xFF8C4D66)),
    )

    val hotBottom = listOf(
        PlaylistCard("camp", "5天好声音\n唱歌训练营", tint = Color(0xFFE68033)),
        PlaylistCard("krec2", "K歌推荐", tint = Color(0xFF4080E6)),
        PlaylistCard("classic", "经典珍藏", tint = Color(0xFF33A666)),
        PlaylistCard("lang", "语种精选", tint = Color(0xFFD9404D)),
    )

    val kidsPosters = listOf(
        PlaylistCard("tongyao", "经典童谣", "爸妈带娃一起唱", tint = Color(0xFFD97340)),
        PlaylistCard("douyin-kids", "抖音最火儿歌", tint = Color(0xFFE65959)),
        PlaylistCard("english", "宝宝必学", "英文儿歌", tint = Color(0xFF73A6E6)),
        PlaylistCard("learn", "唱儿歌 学知识", tint = Color(0xFFF2B366)),
        PlaylistCard("anime", "宝宝最爱", "动漫主题曲", tint = Color(0xFF59B273)),
    )

    val kidsBrands = listOf(
        PlaylistCard("beiwa", "贝瓦儿歌", tint = Color(0xFF59C759)),
        PlaylistCard("babybus", "宝宝巴士", tint = Color(0xFF59B2CC)),
        PlaylistCard("erge", "儿歌大全", tint = Color(0xFFD9404D)),
    )

    val guessSongs = listOf(
        nasSong("xihai", "西海情歌", "刀郎", Color(0xFF73598C), tag = "热歌TOP"),
        nasSong("hongyan", "鸿雁", "呼斯楞", Color(0xFF4D6680), tag = "歌星热歌"),
        nasSong("fenmo", "粉墨人生", "李玉刚", Color(0xFF8C664D), tag = "新歌速递"),
        nasSong("qinghuaci", "青花瓷", "周杰伦", Color(0xFF406680), tag = "热歌TOP"),
    )

    val playlists = listOf(
        PlaylistCard("p1", "夜色温柔", badge = "个性歌单", tint = Color(0xFF4D668C)),
        PlaylistCard("p2", "KTV必点", badge = "热门歌单", tint = Color(0xFF8C594D)),
        PlaylistCard("p3", "怀旧金曲", badge = "经典", tint = Color(0xFF59664D)),
    )

    val nowPlaying = nasSong("xihai", "西海情歌", "刀郎", Color(0xFF73598C)).copy(durationSec = 346)
    val nextSong = nasSong("hongyan", "鸿雁", "呼斯楞", Color(0xFF4D6680))
}
