import SwiftUI

struct SingersView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 20) {
                searchTile
                ForEach(MockData.featuredSingers) { singer in
                    featuredSingerCard(singer)
                }
            }
            .frame(height: 200)

            HStack(spacing: 18) {
                ForEach(MockData.singerGrid) { singer in
                    singerCard(singer)
                }
                hotSingersCard
            }
            .frame(height: 170)

            HStack(spacing: 18) {
                ForEach(MockData.singerRegions) { region in
                    Button {
                        store.showToast("\(region.title) · 曲库后置")
                    } label: {
                        Text(region.title)
                            .font(.system(size: 26, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(region.tint, in: RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous))
                    }
                    .buttonStyle(.card)
                }
            }
            .frame(height: 120)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 28)
        .focusSection()
    }

    private var searchTile: some View {
        Button {
            store.showToast("歌星搜索 / 首拼 · 占位")
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(Color(red: 0.22, green: 0.45, blue: 0.85))
                VStack(spacing: 14) {
                    Image(systemName: "person.crop.circle.badge.magnifyingglass")
                        .font(.system(size: 48))
                    Text("歌星搜索")
                        .font(.system(size: 28, weight: .bold))
                }
                .foregroundStyle(.white)
            }
            .frame(width: 220)
        }
        .buttonStyle(.card)
    }

    private func featuredSingerCard(_ singer: Singer) -> some View {
        Button {
            store.openPlayback()
        } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(singer.coverColor.gradient)
                Text(singer.name)
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(singer.name == "陈奕迅" ? Color.black.opacity(0.85) : .white)
                    .padding(20)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.card)
    }

    private func singerCard(_ singer: Singer) -> some View {
        Button {
            store.openPlayback()
        } label: {
            ZStack(alignment: .bottom) {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(singer.coverColor)
                Circle()
                    .fill(Color.black.opacity(0.35))
                    .frame(width: 70, height: 70)
                    .offset(x: 48, y: -40)
                Text(singer.name)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.bottom, 14)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.card)
    }

    private var hotSingersCard: some View {
        Button {
            store.showToast("热门歌星 · 占位")
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(Color(red: 0.25, green: 0.28, blue: 0.40))
                VStack(spacing: 10) {
                    Image(systemName: "person.3.fill")
                        .font(.system(size: 36))
                    Text("热门歌星")
                        .font(.system(size: 24, weight: .bold))
                }
                .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.card)
    }
}
