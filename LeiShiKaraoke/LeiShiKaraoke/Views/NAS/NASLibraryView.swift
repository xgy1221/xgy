import SwiftUI

struct NASLibraryView: View {
    @Environment(AppStore.self) private var store
    @State private var pathStack: [String] = []
    @State private var entries: [NASEntry] = []
    @State private var songs: [Song] = []
    @State private var status = ""
    @State private var isLoading = false

    private var currentPath: String {
        pathStack.last ?? store.nas.config.normalizedPath
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .firstTextBaseline) {
                ShelfHeader(
                    title: "NAS 曲库",
                    subtitle: "\(store.nas.config.normalizedBaseURLString)\(currentPath)"
                )
                Spacer()
                Button("设置") { store.isSettingsPresented = true }
                    .buttonStyle(.card)
                Button("刷新") { Task { await reload() } }
                    .buttonStyle(.card)
                if pathStack.count > 0 {
                    Button("上级") {
                        _ = pathStack.popLast()
                        Task { await reload() }
                    }
                    .buttonStyle(.card)
                }
            }

            if isLoading {
                ProgressView("正在连接 NAS…")
                    .font(.system(size: 24))
                    .foregroundStyle(AppTheme.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: 200)
            } else if !status.isEmpty && entries.isEmpty && songs.isEmpty {
                emptyState
            } else {
                content
            }

            if !status.isEmpty {
                Text(status)
                    .font(.system(size: 20))
                    .foregroundStyle(status.contains("失败") || status.contains("无法") || status.contains("错误") ? AppTheme.danger : AppTheme.textSecondary)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 32)
        .focusSection()
        .task { await reload() }
        .onChange(of: store.isSettingsPresented) { _, open in
            if !open {
                store.reloadNASConfigFromStore()
                Task { await reload() }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 18) {
            Image(systemName: "externaldrive.badge.exclamationmark")
                .font(.system(size: 56, weight: .light))
                .foregroundStyle(AppTheme.textTertiary)
            Text("还没有内容")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
            Text(status.isEmpty ? "请在设置中填写账号密码后重试" : status)
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
            Button("打开设置") { store.isSettingsPresented = true }
                .buttonStyle(.card)
        }
        .frame(maxWidth: .infinity, minHeight: 280)
    }

    @ViewBuilder
    private var content: some View {
        let folders = entries.filter { $0.kind == .folder || $0.kind == .manifest }
        let audioEntries = entries.filter { $0.kind == .audio || $0.kind == .video }

        if !folders.isEmpty {
            Text("文件夹")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(folders) { entry in
                        Button {
                            Task { await open(entry) }
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: entry.kind == .manifest ? "doc.text" : "folder.fill")
                                Text(entry.name)
                                    .lineLimit(1)
                            }
                            .font(.system(size: 22, weight: .medium))
                            .foregroundStyle(AppTheme.textPrimary)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 18)
                            .frame(width: 240, alignment: .leading)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                        .buttonStyle(.card)
                    }
                }
            }
        }

        let displaySongs = songs.isEmpty ? audioEntries.map(songFromEntry) : songs
        if !displaySongs.isEmpty {
            Text("歌曲 (\(displaySongs.count))")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
                .padding(.top, 8)

            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(displaySongs) { song in
                        HStack(spacing: 16) {
                            Button {
                                store.openPlayback(starting: song)
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(song.displayTitle)
                                            .font(.system(size: 24, weight: .semibold))
                                        Text(song.artist)
                                            .font(.system(size: 18))
                                            .foregroundStyle(AppTheme.textSecondary)
                                    }
                                    Spacer()
                                    Text("播放")
                                        .font(.system(size: 18, weight: .medium))
                                }
                                .foregroundStyle(AppTheme.textPrimary)
                                .padding(16)
                                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .buttonStyle(.card)

                            Button {
                                store.addToQueue(song)
                            } label: {
                                Image(systemName: "plus")
                                    .font(.system(size: 22, weight: .semibold))
                                    .frame(width: 72, height: 72)
                            }
                            .buttonStyle(.card)
                        }
                    }
                }
            }
        }
    }

    private func songFromEntry(_ entry: NASEntry) -> Song {
        let base = (entry.name as NSString).deletingPathExtension
        return Song(
            id: entry.id,
            title: base,
            artist: "NAS",
            mediaURL: entry.absoluteURL,
            source: .nas,
            relativePath: entry.path
        )
    }

    private func reload() async {
        isLoading = true
        songs = []
        entries = []
        defer { isLoading = false }
        store.reloadNASConfigFromStore()
        do {
            entries = try await store.nas.listDirectory(at: currentPath)
            status = "共 \(entries.count) 项"
        } catch {
            status = error.localizedDescription
            entries = []
        }
    }

    private func open(_ entry: NASEntry) async {
        if entry.kind == .folder {
            pathStack.append(entry.path)
            await reload()
        } else if entry.kind == .manifest {
            isLoading = true
            defer { isLoading = false }
            do {
                songs = try await store.nas.loadManifest(at: entry.path)
                status = "清单 \(songs.count) 首"
            } catch {
                status = error.localizedDescription
            }
        }
    }
}
