import SwiftUI

enum MainTab: String, CaseIterable, Identifiable {
    case freeSing = "随心唱"
    case ott
    case singers = "歌星"
    case hot = "热门"
    case ranking = "排行榜"
    case kids = "儿童"
    case nasLibrary = "NAS曲库"
    case features = "玩法"
    case era = "年代"
    case dance = "舞蹈"
    case opera = "戏曲"

    var id: String { title }

    var title: String {
        switch self {
        case .ott: return BrandConfig.ottTabTitle
        default: return rawValue
        }
    }

    var isImplemented: Bool {
        switch self {
        case .freeSing, .singers, .hot, .kids, .nasLibrary: return true
        default: return false
        }
    }
}

enum MediaSource: String, Hashable {
    case mock
    case nas
    case remoteHTTP
}

struct Song: Identifiable, Hashable {
    let id: String
    let title: String
    let artist: String
    var isHD: Bool = false
    var isVIP: Bool = false
    var duration: TimeInterval = 240
    var tag: String? = nil
    var coverColor: Color = .blue
    /// 媒体地址；mock 阶段也用 nas:// 或 https://nas.local 占位，标明来自 NAS。
    var mediaURL: URL? = URL(string: "nas://mock/tracks/placeholder.mp3")
    var coverURL: URL? = nil
    var source: MediaSource = .nas
    var relativePath: String? = nil

    var displayTitle: String {
        isHD ? "\(title)(HD)" : title
    }

    static func == (lhs: Song, rhs: Song) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct Singer: Identifiable, Hashable {
    let id: String
    let name: String
    var regionLabel: String? = nil
    var coverColor: Color = .gray
    var isFeaturedWide: Bool = false

    static func == (lhs: Singer, rhs: Singer) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct HomeTile: Identifiable, Hashable {
    enum Kind: Hashable {
        case daily
        case songOrder
        case singerOrder
        case categoryOrder
        case freeZone
        case myQueue
        case favorites
        case kRecommend
        case ranking
    }

    let id: String
    let kind: Kind
    let title: String
    let tint: Color
    var badgeCount: Int? = nil
    var isWide: Bool = false

    static func == (lhs: HomeTile, rhs: HomeTile) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct PlaylistCard: Identifiable, Hashable {
    let id: String
    let title: String
    var subtitle: String? = nil
    var badge: String? = nil
    var tint: Color = .blue
    var symbolName: String = "music.note"

    static func == (lhs: PlaylistCard, rhs: PlaylistCard) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct AIRecommendItem: Identifiable, Hashable {
    let id: String
    let song: Song
}

struct GuessSongCard: Identifiable, Hashable {
    let id: String
    let song: Song
    let tag: String
    let tagColor: Color

    static func == (lhs: GuessSongCard, rhs: GuessSongCard) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct GuessPlaylistCard: Identifiable, Hashable {
    let id: String
    let title: String
    let tag: String
    let tagColor: Color
    let tint: Color

    static func == (lhs: GuessPlaylistCard, rhs: GuessPlaylistCard) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct HotPageModel {
    var aiItems: [AIRecommendItem]
    var posters: [PlaylistCard]
    var bottomCards: [PlaylistCard]
}

struct KidsPageModel {
    var posters: [PlaylistCard]
    var brands: [PlaylistCard]
}

enum VocalMode: String {
    case accompaniment = "伴唱"
    case original = "原唱"
}

enum PlayOrderMode: String {
    case sequential = "顺序"
    case shuffle = "随机"
    case loop = "单曲"
}
