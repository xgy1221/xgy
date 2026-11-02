import SwiftUI

struct QueueSheetView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text("已点")
                        .font(.system(size: 36, weight: .bold))
                    Text("\(store.playback.queueCount)")
                        .font(.system(size: 22, weight: .semibold))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(AppTheme.surfaceStrong, in: Capsule())
                    Spacer()
                    Button("关闭") { store.isQueuePresented = false }
                        .buttonStyle(.card)
                }
                .foregroundStyle(AppTheme.textPrimary)

                if let now = store.playback.nowPlaying {
                    row(label: "正在播放", song: now)
                }
                if let next = store.playback.nextSong {
                    row(label: "下一首", song: next)
                }

                Text("排队中")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(AppTheme.textSecondary)

                if store.playback.queue.isEmpty {
                    Text("还没有已点歌曲")
                        .font(.system(size: 24))
                        .foregroundStyle(AppTheme.textTertiary)
                        .frame(maxWidth: .infinity, minHeight: 140)
                } else {
                    List {
                        ForEach(store.playback.queue) { song in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(song.displayTitle)
                                        .font(.system(size: 24, weight: .semibold))
                                    Text(song.artist)
                                        .font(.system(size: 18))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(song.source == .nas ? "NAS" : "示例")
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .frame(minHeight: 280)
                }
            }
            .padding(40)
            .frame(width: 900, height: 680)
            .background(AppTheme.backgroundElevated, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .onExitCommand { store.isQueuePresented = false }
    }

    private func row(label: String, song: Song) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 18))
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 100, alignment: .leading)
            Text("\(song.displayTitle) — \(song.artist)")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
            Spacer()
        }
        .padding(16)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
