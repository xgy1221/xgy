import Foundation

/// 品牌文案集中配置 —— 换成自有品牌时只改这里。
enum BrandConfig {
    /// App 显示名（系统主屏幕 / 设置）
    static let appDisplayName = "雷石K歌·专业版"

    /// 顶栏主 Logo 文案
    static let logoTitle = "雷石K歌"

    /// 金色角标
    static let editionBadge = "专业版"

    /// 分类 Tab 中的品牌频道名
    static let ottTabTitle = "雷石K歌·OTT"

    /// Bundle ID 占位（上架前替换）
    static let bundleIdentifier = "com.example.leishikaraoke"

    /// VIP 胶囊默认文案（可按运营位替换）
    static let vipHeadline = "VIP会员"
    static let vipSubtitle = "会员截至:2027/11/03"
}
