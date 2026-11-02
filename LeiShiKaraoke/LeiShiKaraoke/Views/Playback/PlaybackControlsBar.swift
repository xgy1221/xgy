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
                    Text("按下键查看推荐")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(AppTheme.textPrimary)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                        .background(AppTheme.surfaceStrong, in: Capsule())
                }
                .buttonStyle(.plain)
                .padding(.trailing, 56)
            }

            VStack(spacing: 20) {
                progressRow
                controlGroups
            }
            .padding(.horizontal, 44)
            .padding(.vertical, 24)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .padding(.horizontal, 56)
        }
        .onAppear { focusedAction = .play }
    }

    private var progressRow: some View {
        HStack(spacing: 16) {
            Text(store.playback.formatTime(store.playback.currentTime))
                .font(.system(size: 20, weight: .medium).monospacedDigit())
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 72, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.18)).frame(height: 5)
                    Capsule()
                        .fill(Color.white)
                        .frame(width: max(6, geo.size.width * store.playback.progress), height: 5)
                }
                .frame(maxHeight: .infinity)
            }
            .frame(height: 20)

            Text(store.playback.formatTime(store.playback.nowPlaying?.duration ?? 346))
                .font(.system(size: 20, weight: .medium).monospacedDigit())
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 72, alignment: .trailing)
        }
    }

    private var controlGroups: some View {
        HStack(spacing: 32) {
            group {
                actionButton(.skipPrelude, title: "跳过前奏", system: "forward.end.alt")
                actionButton(.order, title: store.playback.orderMode.rawValue, system: "arrow.left.arrow.right")
            }
            group {
                actionButton(.replay, title: "重唱", system: "arrow.counterclockwise")
                actionButton(.play, title: store.playback.isPlaying ? "暂停" : "播放", system: store.playback.isPlaying ? "pause.fill" : "play.fill")
                actionButton(.vocal, title: store.playback.vocalMode.rawValue, system: "person.wave.2")
                actionButton(.next, title: "切歌", system: "forward.end.fill")
            }
            group {
                actionButton(.favorite, title: "收藏", system: store.playback.isFavorite ? "heart.fill" : "heart")
                actionButton(.effects, title: "音效", system: "slider.horizontal.3")
                actionButton(.dislike, title: "不喜欢", system: "hand.thumbsdown")
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func group<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        HStack(spacing: 14) { content() }
            .focusSection()
    }

    private func actionButton(_ action: PlaybackAction, title: String, system: String) -> some View {
        Button { handle(action) } label: {
            VStack(spacing: 10) {
                Image(systemName: system)
                    .font(.system(size: 26, weight: .semibold))
                Text(title)
                    .font(.system(size: 18, weight: .medium))
            }
            .foregroundStyle(AppTheme.textPrimary)
            .frame(width: 118, height: 88)
        }
        .buttonStyle(.card)
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
