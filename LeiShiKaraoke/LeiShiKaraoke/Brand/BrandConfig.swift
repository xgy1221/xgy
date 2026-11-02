import Foundation

/// 品牌文案集中配置 —— 换成自有品牌时只改这里。
enum BrandConfig {
    static let appDisplayName = "雷石K歌"
    static let logoTitle = "雷石K歌"
    static let tagline = "点歌"
    static let ottTabTitle = "精选"
    static let bundleIdentifier = "com.example.leishikaraoke"

    /// NAS 非机密默认值（可进仓库）。账号密码禁止写在此。
    static let defaultNASBaseURL = "https://nas.xgy1221.top"
    static let defaultNASLibraryPath = "/volume1/music"
}
