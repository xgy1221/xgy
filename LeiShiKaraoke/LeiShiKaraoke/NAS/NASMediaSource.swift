import Foundation

enum NASProtocolKind: String, CaseIterable, Identifiable, Codable {
    case https
    case http
    case webdav
    case smb // 预留：tvOS 无原生 SMB，需网关转 HTTP

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .https: return "HTTPS"
        case .http: return "HTTP"
        case .webdav: return "WebDAV"
        case .smb: return "SMB（需网关）"
        }
    }
}

struct NASConfig: Codable, Equatable {
    var protocolKind: NASProtocolKind = .https
    var host: String = ""
    var port: Int? = nil
    var username: String = ""
    /// 密码勿长期明文；正式版进 Keychain。
    var password: String = ""
    var rootPath: String = "/karaoke"
    var manifestPath: String? = "/karaoke/library.json"

    var isConfigured: Bool {
        !host.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var baseURL: URL? {
        guard isConfigured else { return nil }
        var components = URLComponents()
        components.scheme = protocolKind == .webdav ? "https" : protocolKind.rawValue
        if protocolKind == .smb {
            components.scheme = "smb"
        }
        components.host = host
        components.port = port
        components.path = rootPath.hasPrefix("/") ? rootPath : "/\(rootPath)"
        return components.url
    }
}

enum NASError: LocalizedError {
    case notConfigured
    case unsupportedProtocol(NASProtocolKind)
    case notImplemented
    case connectionFailed(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "尚未配置 NAS 地址"
        case .unsupportedProtocol(let kind): return "协议暂未实装：\(kind.displayName)"
        case .notImplemented: return "NAS 访问为 stub，请在 Xcode 真机环境接入"
        case .connectionFailed(let msg): return "连接失败：\(msg)"
        }
    }
}

/// 目录/清单浏览数据源。
protocol NASBrowsing: AnyObject {
    var config: NASConfig { get set }
    func connect() async throws
    func listDirectory(at path: String) async throws -> [NASEntry]
}

struct NASEntry: Identifiable, Hashable {
    enum Kind: Hashable { case folder, audio, video, manifest, other }
    let id: String
    let name: String
    let path: String
    let kind: Kind
}

/// 将 NAS 资源解析为可播放 Song。
protocol NASMediaSource: NASBrowsing {
    func listSongs(under path: String) async throws -> [Song]
    func loadManifest() async throws -> [Song]
    func resolvePlaybackURL(for song: Song) async throws -> URL
}

/// 本期 stub：不发起真实网络，返回 mock 且 source=.nas 的曲目。
final class StubNASMediaSource: NASMediaSource {
    var config: NASConfig
    private(set) var isConnected = false

    init(config: NASConfig = NASConfig()) {
        self.config = config
    }

    func connect() async throws {
        guard config.isConfigured else { throw NASError.notConfigured }
        if config.protocolKind == .smb {
            throw NASError.unsupportedProtocol(.smb)
        }
        // stub：假装连通
        try await Task.sleep(nanoseconds: 200_000_000)
        isConnected = true
    }

    func listDirectory(at path: String) async throws -> [NASEntry] {
        _ = try await ensureReady()
        return [
            NASEntry(id: "tracks", name: "tracks", path: "\(path)/tracks", kind: .folder),
            NASEntry(id: "mv", name: "mv", path: "\(path)/mv", kind: .folder),
            NASEntry(id: "lib", name: "library.json", path: "\(path)/library.json", kind: .manifest),
        ]
    }

    func listSongs(under path: String) async throws -> [Song] {
        _ = try await ensureReady()
        return MockData.aiSongs.map { song in
            var s = song
            s.source = .nas
            s.relativePath = "\(path)/\(song.id).mp3"
            s.mediaURL = URL(string: "nas://\(config.host)\(path)/\(song.id).mp3")
            return s
        }
    }

    func loadManifest() async throws -> [Song] {
        _ = try await ensureReady()
        var songs = [MockData.nowPlaying, MockData.nextSong] + MockData.aiSongs
        for i in songs.indices {
            songs[i].source = .nas
            songs[i].mediaURL = URL(string: "nas://\(config.host)/karaoke/\(songs[i].id).mp3")
            songs[i].relativePath = "/karaoke/\(songs[i].id).mp3"
        }
        return songs
    }

    func resolvePlaybackURL(for song: Song) async throws -> URL {
        if let url = song.mediaURL { return url }
        throw NASError.notImplemented
    }

    private func ensureReady() async throws -> Bool {
        if !isConnected { try await connect() }
        return true
    }
}
