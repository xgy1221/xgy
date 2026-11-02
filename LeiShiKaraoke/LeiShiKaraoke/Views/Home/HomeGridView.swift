import SwiftUI

struct HomeGridView: View {
    @Environment(AppStore.self) private var store
    private let tiles = MockData.homeTiles

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            HStack(alignment: .top, spacing: 20) {
                dailyTile
                ForEach(tiles.filter { !$0.isWide && $0.kind != .freeZone && row1Kinds.contains($0.kind) }) { tile in
                    HomeTileButton(tile: tile) { handle(tile) }
                        .frame(maxWidth: .infinity)
                        .frame(height: 260)
                }
            }
            .frame(height: 260)

            HStack(spacing: 20) {
                ForEach(tiles.filter { row2Kinds.contains($0.kind) }) { tile in
                    HomeTileButton(tile: tile) { handle(tile) }
                        .frame(maxWidth: .infinity)
                        .frame(height: 160)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 32)
        .focusSection()
    }

    private var row1Kinds: Set<HomeTile.Kind> { [.songOrder, .singerOrder, .categoryOrder] }
    private var row2Kinds: Set<HomeTile.Kind> { [.freeZone, .myQueue, .favorites, .kRecommend, .ranking] }

    private var dailyTile: some View {
        Button {
            store.openPlayback(starting: MockData.nowPlaying)
        } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 0.12, green: 0.35, blue: 0.40),
                                Color(red: 0.08, green: 0.15, blue: 0.28),
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 8) {
                        Capsule()
                            .fill(Color.green)
                            .frame(width: 6, height: 22)
                            .shadow(color: .green.opacity(0.8), radius: 6)
                        Text("每日推荐")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(.white)
                    }
                    Text("点击开唱 · mock MV")
                        .font(.system(size: 20))
                        .foregroundStyle(.white.opacity(0.75))
                    Spacer()
                    Text("♪ 西海情歌 — 刀郎")
                        .font(.system(size: 18))
                        .foregroundStyle(.cyan.opacity(0.9))
                        .padding(.bottom, 8)
                }
                .padding(24)
            }
            .frame(width: 520, height: 260)
        }
        .buttonStyle(.card)
    }

    private func handle(_ tile: HomeTile) {
        switch tile.kind {
        case .myQueue:
            store.isQueuePresented = true
        case .daily, .kRecommend, .freeZone:
            store.openPlayback()
        case .songOrder, .singerOrder, .categoryOrder:
            store.selectedTab = tile.kind == .singerOrder ? .singers : .hot
        case .favorites:
            store.showToast("收藏歌曲 · 占位")
        case .ranking:
            store.selectedTab = .ranking
        }
    }
}

struct HomeTileButton: View {
    let tile: HomeTile
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(tile.tint.gradient)
                VStack(spacing: 16) {
                    Image(systemName: symbol)
                        .font(.system(size: 44, weight: .semibold))
                        .symbolRenderingMode(.hierarchical)
                    Text(tile.title)
                        .font(.system(size: 26, weight: .bold))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                .foregroundStyle(.white)
                .padding(16)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                if let badge = tile.badgeCount {
                    Text("\(badge)")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(AppTheme.accentRed, in: Capsule())
                        .offset(x: -12, y: 12)
                }
            }
        }
        .buttonStyle(.card)
    }

    private var symbol: String {
        switch tile.kind {
        case .daily: return "sparkles"
        case .songOrder: return "music.note"
        case .singerOrder: return "person.crop.circle.badge.magnifyingglass"
        case .categoryOrder: return "opticaldisc"
        case .freeZone: return "gift.fill"
        case .myQueue: return "mic.fill"
        case .favorites: return "music.note.list"
        case .kRecommend: return "list.bullet.rectangle"
        case .ranking: return "chart.bar.fill"
        }
    }
}
