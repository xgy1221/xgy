import SwiftUI

struct PlaybackControlsBar: View {
    @Environment(AppStore.self) private var store
    @FocusState private var focusedAction: PlaybackAction?

    enum PlaybackAction: Hashable {
        case skipPrelude, order, replay, play, vocal, next, favorite, effects, dislike
    }

    var body: some View {
        VStack(spacing: 14) {
            HStack {
                Spacer()
                Button {
                    store.isGuessPresented = true
                } label: {
                    Text("按【下键】查看推荐")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(AppTheme.accentRed, in: Capsule())
                }
                .buttonStyle(.plain)
                .padding(.trailing, 48)
            }

            VStack(spacing: 18) {
                progressRow
                controlGroups
            }
            .padding(.horizontal, 40)
            .padding(.vertical, 22)
            .background(.ultraThinMaterial.opacity(0.9), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 48)
        }
        .onAppear {
            focusedAction = .play
        }
    }

    private var progressRow: some View {
        HStack(spacing: 16) {
            Text(store.playback.formatTime(store.playback.currentTime))
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(AppTheme.accentRed)
                .frame(width: 80, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.25)).frame(height: 6)
                    Capsule()
                        .fill(AppTheme.accentRed)
                        .frame(width: max(8, geo.size.width * store.playback.progress), height: 6)
                    Circle()
                        .fill(Color.white)
                        .frame(width: 18, height: 18)
                        .offset(x: max(0, geo.size.width * store.playback.progress - 9))
                }
                .frame(maxHeight: .infinity)
            }
            .frame(height: 24)

            Text(store.playback.formatTime(store.playback.nowPlaying?.duration ?? 346))
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 80, alignment: .trailing)
        }
    }

    /// 分组：核心演唱 | 队列收藏 | 其他 —— 优化焦点路径
    private var controlGroups: some View {
        HStack(spacing: 36) {
            group {
                actionButton(.skipPrelude, title: "跳过前奏", system: "forward.end.alt")
                actionButton(.order, title: store.playback.orderMode.rawValue, system: "arrow.left.arrow.right")
            }
            group(emphasized: true) {
                actionButton(.replay, title: "重唱", system: "arrow.counterclockwise")
                actionButton(.play, title: store.playback.isPlaying ? "播放" : "播放", system: store.playback.isPlaying ? "pause.fill" : "play.fill")
                actionButton(.vocal, title: store.playback.vocalMode.rawValue, system: "person.wave.2.fill")
                actionButton(.next, title: "切歌", system: "forward.end.fill")
            }
            group {
                actionButton(.favorite, title: "收藏", system: store.playback.isFavorite ? "heart.fill" : "heart")
                actionButton(.effects, title: "音效", system: "slider.vertical.3")
                actionButton(.dislike, title: "不喜欢", system: "hand.thumbsdown")
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func group<Content: View>(emphasized: Bool = false, @ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 18) {
            content()
        }
        .padding(.horizontal, emphasized ? 12 : 0)
        .padding(.vertical, emphasized ? 6 : 0)
        .background {
            if emphasized {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
            }
        }
        .focusSection()
    }

    private func actionButton(_ action: PlaybackAction, title: String, system: String) -> some View {
        Button {
            handle(action)
        } label: {
            VStack(spacing: 8) {
                Image(systemName: system)
                    .font(.system(size: 28, weight: .semibold))
                Text(title)
                    .font(.system(size: 20, weight: .medium))
            }
            .foregroundStyle(.white)
            .frame(width: 110, height: 90)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(focusedAction == action ? AppTheme.focusOrange : Color.clear)
            )
        }
        .buttonStyle(.plain)
        .focused($focusedAction, equals: action)
    }

    private func handle(_ action: PlaybackAction) {
        switch action {
        case .skipPrelude: store.playback.skipPrelude()
        case .order: store.playback.cycleOrderMode()
        case .replay: store.playback.replay()
        case .play: store.playback.togglePlayPause()
        case .vocal: store.playback.toggleVocalMode()
        case .next: store.playback.next()
        case .favorite: store.playback.toggleFavorite()
        case .effects: store.showToast("音效 · 占位")
        case .dislike: store.playback.next()
        }
    }
}
