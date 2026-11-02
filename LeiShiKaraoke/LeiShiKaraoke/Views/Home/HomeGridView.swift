import SwiftUI

struct HomeGridView: View {
    @Environment(AppStore.self) private var store

    private let entries: [(title: String, subtitle: String, symbol: String, hue: Double, action: HomeAction)] = [
        ("每日推荐", "今日精选一首", "sparkles", 0.58, .play),
        ("歌曲点歌", "按歌名找歌", "music.note", 0.62, .hot),
        ("歌手点歌", "按歌星浏览", "person.wave.2", 0.72, .singers),
        ("分类点歌", "风格与场景", "square.grid.2x2", 0.48, .hot),
        ("我的点唱", "查看已点队列", "list.bullet", 0.08, .queue),
        ("收藏", "喜欢的歌", "heart", 0.92, .toast("收藏 · 占位")),
        ("K歌推荐", "适合麦霸", "mic", 0.55, .play),
        ("排行榜", "热门榜单", "chart.bar", 0.45, .ranking),
    ]

    private enum HomeAction {
        case play, queue, singers, hot, ranking, toast(String)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: AppTheme.shelfSpacing) {
            ShelfHeader(title: "随心唱", subtitle: "选一首，立刻开唱")

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 22) {
                    ForEach(Array(entries.enumerated()), id: \.offset) { index, item in
                        Button {
                            handle(item.action)
                        } label: {
                            PosterPlaceholder(
                                symbol: item.symbol,
                                title: item.title,
                                subtitle: item.subtitle,
                                hue: item.hue
                            )
                            .frame(width: index == 0 ? 420 : 240, height: index == 0 ? 280 : 280)
                        }
                        .buttonStyle(.card)
                    }
                }
            }

            ShelfHeader(title: "继续点歌", subtitle: "快捷入口")
            HStack(spacing: 20) {
                quickChip(title: "已点 \(store.playback.queueCount)", system: "list.bullet") {
                    store.isQueuePresented = true
                }
                quickChip(title: "NAS 曲库", system: "externaldrive") {
                    store.selectedTab = .nasLibrary
                }
                quickChip(title: "设置 NAS", system: "gearshape") {
                    store.isSettingsPresented = true
                }
                Spacer()
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 36)
        .focusSection()
    }

    private func quickChip(title: String, system: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: system)
                Text(title)
            }
            .font(.system(size: 22, weight: .medium))
            .foregroundStyle(AppTheme.textPrimary)
            .padding(.horizontal, 22)
            .padding(.vertical, 16)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.card)
    }

    private func handle(_ action: HomeAction) {
        switch action {
        case .play: store.openPlayback()
        case .queue: store.isQueuePresented = true
        case .singers: store.selectedTab = .singers
        case .hot: store.selectedTab = .hot
        case .ranking: store.selectedTab = .ranking
        case .toast(let msg): store.showToast(msg)
        }
    }
}
