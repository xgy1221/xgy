import SwiftUI

/// 已点队列：相对参考图的优化 —— 顶栏一键可达。
struct QueueSheetView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text("已点列表")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundStyle(.white)
                    Text("\(store.playback.queueCount)")
                        .font(.system(size: 24, weight: .bold))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(AppTheme.accentRed, in: Capsule())
                        .foregroundStyle(.white)
                    Spacer()
                    Button("关闭") { store.isQueuePresented = false }
                        .buttonStyle(.card)
                }

                if let now = store.playback.nowPlaying {
                    labeledRow(title: "正在播放", song: now, tint: AppTheme.focusOrange)
                }
                if let next = store.playback.nextSong {
                    labeledRow(title: "下一首", song: next, tint: Color.white.opacity(0.2))
                }

                Text("排队中")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.8))

                if store.playback.queue.isEmpty {
                    Text("还没有已点歌曲，去宫格点一首吧")
                        .font(.system(size: 24))
                        .foregroundStyle(.white.opacity(0.55))
                        .frame(maxWidth: .infinity, minHeight: 120)
                } else {
                    List {
                        ForEach(store.playback.queue) { song in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(song.displayTitle)
                                        .font(.system(size: 26, weight: .semibold))
                                    Text(song.artist)
                                        .font(.system(size: 20))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(song.source == .nas ? "NAS" : "本地")
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 6)
                        }
                    }
                    .frame(minHeight: 280)
                }
            }
            .padding(40)
            .frame(width: 900, height: 680)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color(red: 0.10, green: 0.11, blue: 0.18))
            )
        }
        .onExitCommand { store.isQueuePresented = false }
    }

    private func labeledRow(title: String, song: Song, tint: Color) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(.white.opacity(0.7))
                .frame(width: 100, alignment: .leading)
            Text("\(song.displayTitle) — \(song.artist)")
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(.white)
            Spacer()
        }
        .padding(16)
        .background(tint, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}
