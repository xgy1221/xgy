import SwiftUI

struct GuessYouWantView: View {
    @Environment(AppStore.self) private var store
    @FocusState private var focusedSong: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("猜你想唱")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 48)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(MockData.guessSongs) { card in
                        guessSongCard(card)
                    }
                }
                .padding(.horizontal, 48)
            }
            .focusSection()

            Text("歌单推荐")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 48)
                .padding(.top, 8)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(MockData.guessPlaylists) { card in
                        playlistCard(card)
                    }
                }
                .padding(.horizontal, 48)
            }
            .focusSection()
        }
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color.black.opacity(0.05), Color.black.opacity(0.85)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .onAppear {
            focusedSong = MockData.guessSongs.first?.id
        }
    }

    private func guessSongCard(_ card: GuessSongCard) -> some View {
        let focused = focusedSong == card.id
        return Button {
            store.addToQueue(card.song)
        } label: {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(card.tag)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(focused ? .white : card.tagColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            (focused ? Color.white.opacity(0.25) : card.tagColor.opacity(0.35)),
                            in: Capsule()
                        )
                    Text(card.song.displayTitle)
                        .font(.system(size: 24, weight: .bold))
                        .lineLimit(1)
                    Text(card.song.artist)
                        .font(.system(size: 18))
                        .opacity(0.85)
                }
                Spacer(minLength: 8)
                Image(systemName: "plus.circle.fill")
                    .font(.system(size: 32))
            }
            .foregroundStyle(.white)
            .padding(18)
            .frame(width: 340, height: 120)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(focused ? AppTheme.focusOrange : Color.white.opacity(0.12))
            )
        }
        .buttonStyle(.plain)
        .focused($focusedSong, equals: card.id)
    }

    private func playlistCard(_ card: GuessPlaylistCard) -> some View {
        Button {
            store.showToast("打开歌单：\(card.title)")
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                Text(card.tag)
                    .font(.system(size: 14, weight: .bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(card.tagColor.opacity(0.9), in: Capsule())
                    .foregroundStyle(.white)
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(card.tint.gradient)
                    .frame(width: 160, height: 100)
                    .overlay {
                        Image(systemName: "music.note.list")
                            .font(.system(size: 32))
                            .foregroundStyle(.white.opacity(0.8))
                    }
                Text(card.title)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: 160)
        }
        .buttonStyle(.card)
    }
}
