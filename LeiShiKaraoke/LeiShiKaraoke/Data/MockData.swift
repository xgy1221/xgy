import SwiftUI

enum MockData {
    static let homeTiles: [HomeTile] = [
        .init(id: "daily", kind: .daily, title: "每日推荐", tint: Color(red: 0.15, green: 0.55, blue: 0.45), isWide: true),
        .init(id: "song", kind: .songOrder, title: "歌曲点歌", tint: Color(red: 0.20, green: 0.45, blue: 0.95)),
        .init(id: "singer", kind: .singerOrder, title: "歌手点歌", tint: Color(red: 0.85, green: 0.20, blue: 0.28)),
        .init(id: "category", kind: .categoryOrder, title: "分类点歌", tint: Color(red: 0.15, green: 0.28, blue: 0.72)),
        .init(id: "free", kind: .freeZone, title: "免费专区·即刻开唱", tint: Color(red: 0.18, green: 0.42, blue: 0.88)),
        .init(id: "queue", kind: .myQueue, title: "我的点唱", tint: Color(red: 0.92, green: 0.35, blue: 0.55), badgeCount: 4),
        .init(id: "fav", kind: .favorites, title: "收藏歌曲", tint: Color(red: 0.92, green: 0.72, blue: 0.28)),
        .init(id: "krec", kind: .kRecommend, title: "K歌推荐", tint: Color(red: 0.45, green: 0.70, blue: 0.95)),
        .init(id: "rank", kind: .ranking, title: "排行榜", tint: Color(red: 0.25, green: 0.72, blue: 0.45)),
    ]

    static let featuredSingers: [Singer] = [
        .init(id: "naying", name: "那英", coverColor: Color(red: 0.55, green: 0.35, blue: 0.45), isFeaturedWide: true),
        .init(id: "eason", name: "陈奕迅", coverColor: Color(red: 0.85, green: 0.82, blue: 0.78), isFeaturedWide: true),
    ]

    static let singerGrid: [Singer] = [
        .init(id: "jolin", name: "蔡依林", coverColor: Color(red: 0.75, green: 0.45, blue: 0.55)),
        .init(id: "wangqi", name: "王琪", coverColor: Color(red: 0.40, green: 0.55, blue: 0.70)),
        .init(id: "timi", name: "卓依婷", coverColor: Color(red: 0.70, green: 0.50, blue: 0.40)),
        .init(id: "jiang", name: "蒋大为", coverColor: Color(red: 0.45, green: 0.40, blue: 0.55)),
        .init(id: "daolang", name: "刀郎", coverColor: Color(red: 0.35, green: 0.45, blue: 0.40)),
    ]

    static let singerRegions: [PlaylistCard] = [
        .init(id: "cn-m", title: "大陆男歌手", tint: Color(red: 0.12, green: 0.25, blue: 0.55)),
        .init(id: "cn-f", title: "大陆女歌手", tint: Color(red: 0.20, green: 0.65, blue: 0.75)),
        .init(id: "hk-m", title: "港台男歌手", tint: Color(red: 0.90, green: 0.55, blue: 0.20)),
        .init(id: "hk-f", title: "港台女歌手", tint: Color(red: 0.20, green: 0.45, blue: 0.90)),
        .init(id: "group", title: "华语组合", tint: Color(red: 0.85, green: 0.20, blue: 0.25)),
        .init(id: "intl", title: "国外歌手", tint: Color(red: 0.20, green: 0.65, blue: 0.35)),
    ]

    static let aiSongs: [Song] = [
        .init(id: "xiaoban", title: "小半", artist: "陈粒", coverColor: Color(red: 0.55, green: 0.35, blue: 0.45), mediaURL: URL(string: "nas://nas.local/karaoke/tracks/xiaoban.mp3"), source: .nas, relativePath: "/karaoke/tracks/xiaoban.mp3"),
        .init(id: "chahua", title: "茶花开了", artist: "王睿卓", isHD: true, coverColor: Color(red: 0.70, green: 0.40, blue: 0.35), mediaURL: URL(string: "nas://nas.local/karaoke/tracks/chahua.mp3"), source: .nas, relativePath: "/karaoke/tracks/chahua.mp3"),
        .init(id: "yongchun", title: "咏春", artist: "七朵组合", isHD: true, coverColor: Color(red: 0.85, green: 0.55, blue: 0.30), mediaURL: URL(string: "nas://nas.local/karaoke/tracks/yongchun.mp3"), source: .nas, relativePath: "/karaoke/tracks/yongchun.mp3"),
        .init(id: "shimian", title: "失眠", artist: "Suki刘舒妤", coverColor: Color(red: 0.35, green: 0.40, blue: 0.60), mediaURL: URL(string: "nas://nas.local/karaoke/tracks/shimian.mp3"), source: .nas, relativePath: "/karaoke/tracks/shimian.mp3"),
        .init(id: "shexiang", title: "奢香夫人", artist: "凤凰传奇", isVIP: true, coverColor: Color(red: 0.75, green: 0.30, blue: 0.35), mediaURL: URL(string: "nas://nas.local/karaoke/tracks/shexiang.mp3"), source: .nas, relativePath: "/karaoke/tracks/shexiang.mp3"),
    ]

    static let hotPosters: [PlaylistCard] = [
        .init(id: "douyin", title: "抖音热歌精选", subtitle: "#火爆全网的歌#", tint: Color(red: 0.65, green: 0.35, blue: 0.45), symbolName: "flame.fill"),
        .init(id: "china", title: "唱响中国之歌", subtitle: "歌声致敬新时代", tint: Color(red: 0.25, green: 0.40, blue: 0.70), symbolName: "flag.fill"),
        .init(id: "onehit", title: "1人1首成名曲", subtitle: "这些歌，百唱不厌", badge: "经典珍藏", tint: Color(red: 0.40, green: 0.35, blue: 0.30), symbolName: "star.fill"),
        .init(id: "duet", title: "经典对唱", subtitle: "把我唱给你听", tint: Color(red: 0.55, green: 0.30, blue: 0.40), symbolName: "person.2.fill"),
    ]

    static let hotBottom: [PlaylistCard] = [
        .init(id: "camp", title: "5天好声音\n唱歌训练营", tint: Color(red: 0.92, green: 0.50, blue: 0.20), symbolName: "mic.fill"),
        .init(id: "krec2", title: "K歌推荐", tint: Color(red: 0.25, green: 0.50, blue: 0.90), symbolName: "music.note.list"),
        .init(id: "classic", title: "经典珍藏", tint: Color(red: 0.20, green: 0.65, blue: 0.40), symbolName: "opticaldisc"),
        .init(id: "lang", title: "语种精选", tint: Color(red: 0.85, green: 0.25, blue: 0.30), symbolName: "waveform"),
    ]

    static let kidsPosters: [PlaylistCard] = [
        .init(id: "tongyao", title: "经典童谣", subtitle: "爸妈带娃一起唱", tint: Color(red: 0.85, green: 0.45, blue: 0.25), symbolName: "book.fill"),
        .init(id: "douyin-kids", title: "抖音最火儿歌", subtitle: nil, tint: Color(red: 0.90, green: 0.35, blue: 0.35), symbolName: "flame.fill"),
        .init(id: "english", title: "宝宝必学", subtitle: "英文儿歌", tint: Color(red: 0.45, green: 0.65, blue: 0.90), symbolName: "globe"),
        .init(id: "learn", title: "唱儿歌 学知识", subtitle: nil, tint: Color(red: 0.95, green: 0.70, blue: 0.40), symbolName: "lightbulb.fill"),
        .init(id: "anime", title: "宝宝最爱", subtitle: "动漫主题曲", tint: Color(red: 0.35, green: 0.70, blue: 0.45), symbolName: "tv.fill"),
    ]

    static let kidsBrands: [PlaylistCard] = [
        .init(id: "beiwa", title: "贝瓦儿歌", tint: Color(red: 0.35, green: 0.78, blue: 0.35), symbolName: "pawprint.fill"),
        .init(id: "babybus", title: "宝宝巴士", tint: Color(red: 0.35, green: 0.70, blue: 0.80), symbolName: "bus.fill"),
        .init(id: "erge", title: "儿歌大全", tint: Color(red: 0.85, green: 0.25, blue: 0.30), symbolName: "music.note.list"),
    ]

    static let nowPlaying = Song(
        id: "xihai",
        title: "西海情歌",
        artist: "刀郎",
        duration: 346,
        coverColor: Color(red: 0.45, green: 0.35, blue: 0.55)
    )

    static let nextSong = Song(
        id: "hongyan",
        title: "鸿雁",
        artist: "呼斯楞",
        duration: 280,
        coverColor: Color(red: 0.40, green: 0.50, blue: 0.65)
    )

    static let guessSongs: [GuessSongCard] = [
        .init(id: "g1", song: .init(id: "suiyue", title: "岁月如笔写...", artist: "马健涛"), tag: "热歌TOP", tagColor: .orange),
        .init(id: "g2", song: .init(id: "zaijian", title: "再见太难", artist: "joysaaaa", isHD: true), tag: "新歌速递", tagColor: Color(red: 0.40, green: 0.70, blue: 0.95)),
        .init(id: "g3", song: .init(id: "pengyou", title: "朋友", artist: "周华健", isHD: true), tag: "热歌TOP", tagColor: Color(red: 0.20, green: 0.40, blue: 0.75)),
        .init(id: "g4", song: .init(id: "shouxin", title: "手心里的温柔", artist: "刀郎"), tag: "歌星热歌", tagColor: Color(red: 0.60, green: 0.35, blue: 0.75)),
    ]

    static let guessPlaylists: [GuessPlaylistCard] = [
        .init(id: "gp1", title: "深夜必唱", tag: "个性歌单", tagColor: Color(red: 0.60, green: 0.35, blue: 0.75), tint: Color(red: 0.35, green: 0.25, blue: 0.45)),
        .init(id: "gp2", title: "K房热榜", tag: "热门歌单", tagColor: Color(red: 0.20, green: 0.40, blue: 0.75), tint: Color(red: 0.25, green: 0.35, blue: 0.55)),
        .init(id: "gp3", title: "怀旧金曲", tag: "精选歌单", tagColor: Color(red: 0.85, green: 0.70, blue: 0.30), tint: Color(red: 0.50, green: 0.40, blue: 0.30)),
        .init(id: "gp4", title: "男女对唱", tag: "精选歌单", tagColor: Color(red: 0.85, green: 0.70, blue: 0.30), tint: Color(red: 0.55, green: 0.30, blue: 0.40)),
    ]

    static var hotPage: HotPageModel {
        HotPageModel(
            aiItems: aiSongs.map { AIRecommendItem(id: $0.id, song: $0) },
            posters: hotPosters,
            bottomCards: hotBottom
        )
    }

    static var kidsPage: KidsPageModel {
        KidsPageModel(posters: kidsPosters, brands: kidsBrands)
    }
}
