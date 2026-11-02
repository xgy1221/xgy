import SwiftUI

struct NASLibraryView: View {
    @Environment(AppStore.self) private var store
    @State private var entries: [NASEntry] = []
    @State private var songs: [Song] = []
    @State private var statusText = "未连接"
    @State private var hostDraft = "nas.local"
    @State private var pathDraft = "/karaoke"

    var body: some View {
        HStack(alignment: .top, spacing: 28) {
            configPanel
                .frame(width: 420)
            contentPanel
            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 28)
        .focusSection()
    }

    private var configPanel: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("NAS 音源")
                .font(.system(size: 32, weight: .bold))
                .foregroundStyle(.white)
            Text("推荐 HTTP(S)/WebDAV；SMB 需网关。账号密码正式版进 Keychain。")
                .font(.system(size: 20))
                .foregroundStyle(.white.opacity(0.65))
                .fixedSize(horizontal: false, vertical: true)

            labeledField("地址", text: hostDraft)
            labeledField("根路径", text: pathDraft)

            HStack(spacing: 14) {
                Button("保存并连接") {
                    Task { await connect() }
                }
                .buttonStyle(.card)

                Button("加载清单") {
                    Task { await loadManifest() }
                }
                .buttonStyle(.card)
            }

            Text(statusText)
                .font(.system(size: 20))
                .foregroundStyle(AppTheme.vipGold)

            Spacer(minLength: 0)
        }
        .padding(24)
        .frame(maxHeight: .infinity, alignment: .topLeading)
        .background(
            RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                .fill(Color.white.opacity(0.08))
        )
    }

    private var contentPanel: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(songs.isEmpty ? "目录 / 曲目" : "来自 NAS 的曲目（mock URL）")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(.white)

            if !store.nas.config.isConfigured && songs.isEmpty && entries.isEmpty {
                emptyState
            } else if !songs.isEmpty {
                songList
            } else if !entries.isEmpty {
                entryList
            } else {
                emptyState
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "externaldrive.badge.icloud")
                .font(.system(size: 56))
                .foregroundStyle(.white.opacity(0.45))
            Text("尚未配置或未扫描 NAS")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(.white)
            Text("填写地址后点「保存并连接」。mock 阶段也会写入 nas:// 媒体 URL。")
                .font(.system(size: 22))
                .foregroundStyle(.white.opacity(0.55))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, minHeight: 280)
    }

    private var entryList: some View {
        HStack(spacing: 16) {
            ForEach(entries) { entry in
                Button {
                    Task { await openEntry(entry) }
                } label: {
                    VStack(spacing: 12) {
                        Image(systemName: symbol(for: entry.kind))
                            .font(.system(size: 36))
                        Text(entry.name)
                            .font(.system(size: 22, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .frame(width: 180, height: 140)
                    .background(Color.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.card)
            }
        }
    }

    private var songList: some View {
        VStack(spacing: 10) {
            ForEach(songs) { song in
                Button {
                    store.openPlayback(starting: song)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(song.displayTitle)
                                .font(.system(size: 24, weight: .semibold))
                            Text(song.artist)
                                .font(.system(size: 18))
                                .opacity(0.7)
                            Text(song.mediaURL?.absoluteString ?? "")
                                .font(.system(size: 14, design: .monospaced))
                                .opacity(0.45)
                                .lineLimit(1)
                        }
                        .foregroundStyle(.white)
                        Spacer()
                        Text(song.source.rawValue.uppercased())
                            .font(.system(size: 14, weight: .bold))
                            .padding(6)
                            .background(AppTheme.focusOrange.opacity(0.8), in: Capsule())
                            .foregroundStyle(.white)
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.card)
            }
        }
    }

    private func labeledField(_ title: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 18))
                .foregroundStyle(.white.opacity(0.6))
            Text(text)
                .font(.system(size: 24, weight: .medium, design: .monospaced))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.black.opacity(0.35), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        // tvOS 完整 TextField 需真机键盘；stub 用按钮改默认值
        .onTapGesture {
            if title == "地址" {
                hostDraft = hostDraft == "nas.local" ? "192.168.1.10" : "nas.local"
            }
        }
    }

    private func connect() async {
        store.nas.config.host = hostDraft
        store.nas.config.rootPath = pathDraft
        store.nas.config.protocolKind = .https
        do {
            try await store.nas.connect()
            entries = try await store.nas.listDirectory(at: pathDraft)
            statusText = "已连接（stub）\(hostDraft)"
        } catch {
            statusText = error.localizedDescription
            entries = []
        }
    }

    private func loadManifest() async {
        store.nas.config.host = hostDraft
        store.nas.config.rootPath = pathDraft
        do {
            songs = try await store.nas.loadManifest()
            statusText = "清单已加载 · \(songs.count) 首"
        } catch {
            statusText = error.localizedDescription
        }
    }

    private func openEntry(_ entry: NASEntry) async {
        if entry.kind == .folder {
            do {
                songs = try await store.nas.listSongs(under: entry.path)
                statusText = "目录 \(entry.name) · \(songs.count) 首"
            } catch {
                statusText = error.localizedDescription
            }
        } else if entry.kind == .manifest {
            await loadManifest()
        }
    }

    private func symbol(for kind: NASEntry.Kind) -> String {
        switch kind {
        case .folder: return "folder.fill"
        case .audio: return "music.note"
        case .video: return "film"
        case .manifest: return "doc.text"
        case .other: return "doc"
        }
    }
}
