package com.xgy.androidkaraoke.data

import androidx.compose.ui.graphics.Color
import com.xgy.androidkaraoke.model.HomeTile
import com.xgy.androidkaraoke.model.HomeTileKind
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.PlaylistCard
import com.xgy.androidkaraoke.model.Singer
import com.xgy.androidkaraoke.model.Song

object MockData {
    /**
     * 公开样例媒体，便于无 NAS 时验证 MV/播放控制。
     * 正式曲库来自 NAS library.json（mv / original / accompaniment）。
     */
    private const val SAMPLE_MV =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    private const val SAMPLE_AUDIO =
        "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"

    val homeTiles = listOf(
        // 「每日推荐」已改为首页迷你唱页（MiniNowPlayingCard），不再用假瓷砖
        HomeTile("song", HomeTileKind.SongOrder, "歌曲点歌", Color(0xFF3373F2)),
        HomeTile("singer", HomeTileKind.SingerOrder, "歌手点歌", Color(0xFFD93347)),
        HomeTile("category", HomeTileKind.CategoryOrder, "分类点歌", Color(0xFF2647B8)),
        // 「免费专区」瓷砖已按雷电反馈移除
        HomeTile("queue", HomeTileKind.MyQueue, "我的点唱", Color(0xFFE85A8C), badgeCount = 4),
        HomeTile("fav", HomeTileKind.Favorites, "收藏歌曲", Color(0xFFEBB847)),
        HomeTile("krec", HomeTileKind.KRecommend, "K歌推荐", Color(0xFF73B2F2)),
        // 排行榜 Tab 已移除；首页不再展示排行榜瓷砖
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

    private fun demoSong(
        id: String,
        title: String,
        artist: String,
        color: Color,
        genres: List<String>,
        hd: Boolean = false,
        tag: String? = null,
        duration: Long = 240,
        hotScore: Int = 10,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        isHd = hd,
        tag = tag,
        genres = genres,
        coverColor = color,
        durationSec = duration,
        source = MediaSource.Mock,
        relativePath = "/demo/$id",
        mvUrl = SAMPLE_MV,
        originalUrl = SAMPLE_AUDIO,
        accompanimentUrl = SAMPLE_AUDIO,
        hotScore = hotScore,
    )

    val aiSongs = listOf(
        demoSong("xiaoban", "小半", "陈粒", Color(0xFF8C5973), genres = listOf("民谣", "独立"), tag = "治愈", hotScore = 40),
        demoSong("chahua", "茶花开了", "王睿卓", Color(0xFFB26659), genres = listOf("民谣", "原生态"), hd = true, hotScore = 35),
        demoSong("yongchun", "咏春", "七朵组合", Color(0xFFD98C4D), genres = listOf("古风", "女声"), hd = true, tag = "国风", hotScore = 55),
        demoSong("shimian", "失眠", "Suki刘舒妤", Color(0xFF596699), genres = listOf("流行", "抒情"), hotScore = 30),
        demoSong("shexiang", "奢香夫人", "凤凰传奇", Color(0xFFBF4D59), genres = listOf("民族", "劲歌"), tag = "热歌TOP", hotScore = 80),
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
        demoSong("xihai", "西海情歌", "刀郎", Color(0xFF73598C), genres = listOf("民谣", "西部"), tag = "热歌TOP", duration = 346, hotScore = 90),
        demoSong("hongyan", "鸿雁", "呼斯楞", Color(0xFF4D6680), genres = listOf("民歌", "草原"), tag = "歌星热歌", hotScore = 70),
        demoSong("fenmo", "粉墨人生", "李玉刚", Color(0xFF8C664D), genres = listOf("戏曲", "古风"), tag = "新歌速递", hotScore = 45),
        demoSong("qinghuaci", "青花瓷", "周杰伦", Color(0xFF406680), genres = listOf("流行", "中国风"), tag = "热歌TOP", hotScore = 95),
    )

    /** 扩充曲库，便于常唱偏好排序有区分度。 */
    val moreSongs = listOf(
        demoSong("daoxiang", "稻香", "周杰伦", Color(0xFF557799), genres = listOf("流行", "中国风"), hotScore = 88),
        demoSong("dongfengpo", "东风破", "周杰伦", Color(0xFF668866), genres = listOf("流行", "中国风"), hotScore = 82),
        demoSong("2002", "2002年的第一场雪", "刀郎", Color(0xFF886655), genres = listOf("民谣", "西部"), hotScore = 75),
        demoSong("luhua", "冲动的惩罚", "刀郎", Color(0xFF775544), genres = listOf("民谣", "西部"), hotScore = 68),
        demoSong("zui", "最炫民族风", "凤凰传奇", Color(0xFFCC5566), genres = listOf("民族", "劲歌"), tag = "热歌TOP", hotScore = 92),
        demoSong("lang", "自由飞翔", "凤凰传奇", Color(0xFFAA4455), genres = listOf("民族", "劲歌"), hotScore = 78),
        demoSong("gufeng2", "梨花香", "七朵组合", Color(0xFFDDAA66), genres = listOf("古风", "女声"), hotScore = 50),
        demoSong("minyao2", "奇妙能力歌", "陈粒", Color(0xFF997788), genres = listOf("民谣", "独立"), hotScore = 48),
        demoSong("liuxing1", "晴天", "周杰伦", Color(0xFF6699AA), genres = listOf("流行", "抒情"), hotScore = 93),
        demoSong("caoyuan", "父亲的草原母亲的河", "德德玛", Color(0xFF558877), genres = listOf("民歌", "草原"), hotScore = 60),
    )

    val playlists = listOf(
        PlaylistCard("p1", "夜色温柔", badge = "个性歌单", tint = Color(0xFF4D668C)),
        PlaylistCard("p2", "KTV必点", badge = "热门歌单", tint = Color(0xFF8C594D)),
        PlaylistCard("p3", "怀旧金曲", badge = "经典", tint = Color(0xFF59664D)),
    )

    val catalog: List<Song> = (aiSongs + guessSongs + moreSongs).distinctBy { it.id }

    val nowPlaying = demoSong(
        "xihai", "西海情歌", "刀郎", Color(0xFF73598C),
        genres = listOf("民谣", "西部"), duration = 346, hotScore = 90,
    )
    val nextSong = demoSong(
        "hongyan", "鸿雁", "呼斯楞", Color(0xFF4D6680),
        genres = listOf("民歌", "草原"), hotScore = 70,
    )
}
