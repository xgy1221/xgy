import SwiftUI

struct GuessYouWantView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("猜你想唱")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .padding(.horizontal, 56)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(MockData.guessSongs) { card in
                        Button {
                            store.addToQueue(card.song)
                        } label: {
                            HStack(spacing: 14) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text(card.tag)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(AppTheme.textSecondary)
                                    Text(card.song.displayTitle)
                                        .font(.system(size: 24, weight: .semibold))
                                        .lineLimit(1)
                                    Text(card.song.artist)
                                        .font(.system(size: 18))
                                        .foregroundStyle(AppTheme.textSecondary)
                                }
                                Spacer(minLength: 8)
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 30))
                                    .symbolRenderingMode(.hierarchical)
                            }
                            .foregroundStyle(AppTheme.textPrimary)
                            .padding(18)
                            .frame(width: 340, height: 118)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding(.horizontal, 56)
            }
            .focusSection()

            Text("歌单推荐")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .padding(.horizontal, 56)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(Array(MockData.guessPlaylists.enumerated()), id: \.element.id) { idx, card in
                        Button {
                            store.showToast("打开歌单：\(card.title)")
                        } label: {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(card.tag)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(AppTheme.textSecondary)
                                PosterPlaceholder(
                                    symbol: "music.note.list",
                                    title: card.title,
                                    subtitle: nil,
                                    hue: 0.55 + Double(idx) * 0.1
                                )
                                .frame(width: 170, height: 110)
                            }
                            .frame(width: 170)
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding(.horizontal, 56)
            }
            .focusSection()
        }
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color.black.opacity(0.2), Color.black.opacity(0.92)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}
