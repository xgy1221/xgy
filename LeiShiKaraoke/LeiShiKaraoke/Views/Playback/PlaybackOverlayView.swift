import SwiftUI

struct PlaybackOverlayView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(white: 0.12), .black],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack {
                topBar
                Spacer()
                if store.isGuessPresented {
                    GuessYouWantView()
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                } else {
                    PlaybackControlsBar()
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .padding(.bottom, 28)
        }
        .onExitCommand {
            if store.isGuessPresented {
                store.isGuessPresented = false
            } else {
                store.dismissPlayback()
            }
        }
        .onAppear {
            Task { try? await store.microphone.start() }
        }
        .onDisappear { store.microphone.stop() }
        .onMoveCommand { direction in
            if direction == .down, !store.isGuessPresented {
                store.isGuessPresented = true
            } else if direction == .up, store.isGuessPresented {
                store.isGuessPresented = false
            }
        }
    }

    private var topBar: some View {
        HStack(alignment: .top) {
            VStack(spacing: 8) {
                Text("扫码点歌")
                    .font(.system(size: 16))
                    .foregroundStyle(AppTheme.textSecondary)
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(.white)
                    .frame(width: 110, height: 110)
                    .overlay {
                        Image(systemName: "qrcode")
                            .font(.system(size: 56))
                            .foregroundStyle(.black.opacity(0.85))
                    }
            }
            .padding(.leading, 48)
            .padding(.top, 32)

            Spacer()

            VStack(spacing: 8) {
                Text(nowPlayingLine)
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                Text("已点 \(store.playback.queueCount) 首 · 麦：\(store.microphone.status.route.displayName)")
                    .font(.system(size: 20))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .padding(.top, 40)

            Spacer()
            Color.clear.frame(width: 160)
        }
    }

    private var nowPlayingLine: String {
        let now = store.playback.nowPlaying?.title ?? "西海情歌"
        let next = store.playback.nextSong?.title ?? "鸿雁"
        return "正在播放：\(now)    下一首：\(next)"
    }
}
