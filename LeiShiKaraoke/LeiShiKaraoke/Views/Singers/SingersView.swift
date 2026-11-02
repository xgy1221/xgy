import SwiftUI

struct SingersView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: AppTheme.shelfSpacing) {
                ShelfHeader(title: "歌星", subtitle: "从歌手找到想唱的歌")

                HStack(spacing: 22) {
                    Button {
                        store.showToast("歌星搜索 · 占位")
                    } label: {
                        PosterPlaceholder(symbol: "magnifyingglass", title: "搜索歌星", subtitle: "首拼 / 全名", hue: 0.60)
                            .frame(width: 260, height: 200)
                    }
                    .buttonStyle(.card)

                    ForEach(Array(MockData.featuredSingers.enumerated()), id: \.element.id) { idx, singer in
                        Button { store.openPlayback() } label: {
                            PosterPlaceholder(
                                symbol: "person.fill",
                                title: singer.name,
                                subtitle: "精选",
                                hue: 0.70 + Double(idx) * 0.08
                            )
                            .frame(maxWidth: .infinity)
                            .frame(height: 200)
                        }
                        .buttonStyle(.card)
                    }
                }

                Text("热门歌星")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 18) {
                        ForEach(MockData.singerGrid) { singer in
                            Button { store.openPlayback() } label: {
                                VStack(spacing: 14) {
                                    Circle()
                                        .fill(
                                            LinearGradient(
                                                colors: [singer.coverColor.opacity(0.8), Color.black.opacity(0.6)],
                                                startPoint: .top,
                                                endPoint: .bottom
                                            )
                                        )
                                        .frame(width: 120, height: 120)
                                        .overlay {
                                            Image(systemName: "person.fill")
                                                .font(.system(size: 40))
                                                .foregroundStyle(.white.opacity(0.7))
                                        }
                                    Text(singer.name)
                                        .font(.system(size: 22, weight: .medium))
                                        .foregroundStyle(AppTheme.textPrimary)
                                }
                                .frame(width: 140)
                            }
                            .buttonStyle(.card)
                        }
                        Button { store.showToast("更多歌星") } label: {
                            VStack(spacing: 14) {
                                Circle()
                                    .fill(AppTheme.surface)
                                    .frame(width: 120, height: 120)
                                    .overlay {
                                        Image(systemName: "ellipsis")
                                            .font(.system(size: 32))
                                            .foregroundStyle(AppTheme.textSecondary)
                                    }
                                Text("更多")
                                    .font(.system(size: 22, weight: .medium))
                                    .foregroundStyle(AppTheme.textPrimary)
                            }
                            .frame(width: 140)
                        }
                        .buttonStyle(.card)
                    }
                }

                Text("按地区")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 220), spacing: 16)], spacing: 16) {
                    ForEach(MockData.singerRegions) { region in
                        Button {
                            store.showToast("\(region.title) · 曲库后置")
                        } label: {
                            HStack {
                                Text(region.title)
                                    .font(.system(size: 22, weight: .semibold))
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            .foregroundStyle(AppTheme.textPrimary)
                            .padding(20)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.card)
                    }
                }
            }
            .padding(.horizontal, AppTheme.contentPadding)
            .padding(.bottom, 40)
        }
        .focusSection()
    }
}
