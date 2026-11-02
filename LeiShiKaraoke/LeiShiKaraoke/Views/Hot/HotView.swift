import SwiftUI

struct HotView: View {
    @Environment(AppStore.self) private var store
    private let page = MockData.hotPage

    var body: some View {
        HStack(alignment: .top, spacing: 28) {
            recommendColumn
                .frame(width: 380)
            VStack(alignment: .leading, spacing: 22) {
                ShelfHeader(title: "热门", subtitle: "精选歌单与海报墙")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 18) {
                        ForEach(Array(page.posters.enumerated()), id: \.element.id) { idx, card in
                            Button { store.openPlayback() } label: {
                                PosterPlaceholder(
                                    symbol: card.symbolName,
                                    title: card.title,
                                    subtitle: card.subtitle,
                                    hue: 0.55 + Double(idx) * 0.07
                                )
                                .frame(width: 220, height: 320)
                            }
                            .buttonStyle(.card)
                        }
                    }
                }
                Text("更多入口")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                HStack(spacing: 16) {
                    ForEach(page.bottomCards) { card in
                        Button { store.openPlayback() } label: {
                            HStack(spacing: 12) {
                                Image(systemName: card.symbolName)
                                    .font(.system(size: 26))
                                Text(card.title.replacingOccurrences(of: "\n", with: " "))
                                    .font(.system(size: 20, weight: .semibold))
                                    .lineLimit(2)
                                Spacer(minLength: 0)
                            }
                            .foregroundStyle(AppTheme.textPrimary)
                            .padding(18)
                            .frame(maxWidth: .infinity, minHeight: 96)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.card)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 32)
        .focusSection()
    }

    private var recommendColumn: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("为你推荐")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .padding(.bottom, 4)

            ForEach(page.aiItems) { item in
                Button {
                    store.openPlayback(starting: item.song)
                } label: {
                    HStack(spacing: 14) {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(item.song.coverColor.opacity(0.55))
                            .frame(width: 64, height: 64)
                            .overlay {
                                Image(systemName: "music.note")
                                    .foregroundStyle(.white.opacity(0.8))
                            }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.song.displayTitle)
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundStyle(AppTheme.textPrimary)
                                .lineLimit(1)
                            Text(item.song.artist)
                                .font(.system(size: 18))
                                .foregroundStyle(AppTheme.textSecondary)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(12)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.card)
            }

            Button {
                store.showToast("更多推荐 · 占位")
            } label: {
                Text("查看更多")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(AppTheme.textPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(AppTheme.surfaceStrong, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.card)

            Spacer(minLength: 0)
        }
        .focusSection()
    }
}
