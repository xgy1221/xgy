import SwiftUI

struct PlaybackOverlayView: View {
    @Environment(AppStore.self) private var store
    @FocusState private var controlsFocused: Bool

    var body: some View {
        ZStack {
            mvBackground

            VStack {
                topBar
                Spacer()
                if store.isGuessPresented {
                    GuessYouWantView()
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                } else {
                    PlaybackControlsBar()
                        .focused($controlsFocused)
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
            controlsFocused = true
            Task { try? await store.microphone.start() }
        }
        .onDisappear {
            store.microphone.stop()
        }
        // 模拟下键：tvOS 上可用 moveCommand；同时提供按钮。
        .onMoveCommand { direction in
            if direction == .down, !store.isGuessPresented {
                store.isGuessPresented = true
            } else if direction == .up, store.isGuessPresented {
                store.isGuessPresented = false
            }
        }
    }

    private var mvBackground: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.15, green: 0.12, blue: 0.28),
                    Color(red: 0.05, green: 0.08, blue: 0.16),
                    Color(red: 0.20, green: 0.10, blue: 0.18),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack(spacing: 12) {
                Image(systemName: "play.rectangle.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(.white.opacity(0.25))
                Text("MV 占位")
                    .font(.system(size: 28))
                    .foregroundStyle(.white.opacity(0.3))
            }
        }
        .ignoresSafeArea()
    }

    private var topBar: some View {
        HStack(alignment: .top) {
            VStack(spacing: 8) {
                Text("扫码点歌")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.white)
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(Color.green, lineWidth: 4)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(.white)
                    )
                    .frame(width: 120, height: 120)
                    .overlay {
                        Image(systemName: "qrcode")
                            .font(.system(size: 64))
                            .foregroundStyle(.black.opacity(0.8))
                    }
            }
            .padding(.leading, 40)
            .padding(.top, 28)

            Spacer()

            VStack(spacing: 8) {
                Text(nowPlayingLine)
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(AppTheme.accentRed)
                Text("已点 \(store.playback.queueCount) 首 · 麦：\(store.microphone.status.route.displayName)")
                    .font(.system(size: 20))
                    .foregroundStyle(.white.opacity(0.7))
            }
            .padding(.top, 36)

            Spacer()
            Color.clear.frame(width: 160, height: 1)
        }
    }

    private var nowPlayingLine: String {
        let now = store.playback.nowPlaying?.title ?? "西海情歌"
        let next = store.playback.nextSong?.title ?? "鸿雁"
        return "正在播放：\(now)  下一首：\(next)"
    }
}
