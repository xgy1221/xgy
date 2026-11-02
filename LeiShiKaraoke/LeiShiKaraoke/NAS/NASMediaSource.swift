import Foundation

enum NASError: LocalizedError {
    case notConfigured
    case invalidURL
    case unauthorized
    case forbidden
    case notFound
    case timeout
    case server(Int, String)
    case transport(String)
    case emptyListing
    case unsupportedContent

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "请先在设置中填写 NAS 地址"
        case .invalidURL: return "NAS 地址无效，请检查 Base URL 与路径"
        case .unauthorized: return "认证失败：请在设置中填写正确的账号和密码"
        case .forbidden: return "没有权限访问该目录"
        case .notFound: return "找不到该路径，请确认曲库目录是否正确"
        case .timeout: return "连接超时，请检查网络或 NAS 是否在线"
        case .server(let code, let msg): return "服务器错误（\(code)）：\(msg)"
        case .transport(let msg): return "无法连接 NAS：\(msg)"
        case .emptyListing: return "目录为空，或服务器未开启目录列表"
        case .unsupportedContent: return "无法解析目录页（可尝试放置 library.json 清单）"
        }
    }
}

struct NASEntry: Identifiable, Hashable {
    enum Kind: Hashable { case folder, audio, video, manifest, other }
    let id: String
    let name: String
    let path: String
    let kind: Kind
    var absoluteURL: URL?
}

protocol NASBrowsing: AnyObject {
    var config: NASConfig { get set }
    func listDirectory(at path: String) async throws -> [NASEntry]
}

protocol NASMediaSource: NASBrowsing {
    func listSongs(under path: String) async throws -> [Song]
    func loadManifest(at path: String?) async throws -> [Song]
    func resolvePlaybackURL(for song: Song) async throws -> URL
    func testConnection() async throws -> String
}

/// HTTPS 目录浏览 + 音频扩展名扫描；支持 Basic 鉴权（凭证来自 Keychain）。
final class HTTPSNASMediaSource: NASMediaSource, @unchecked Sendable {
    var config: NASConfig
    private let session: URLSession

    init(config: NASConfig = NASConfigStore.load(), session: URLSession? = nil) {
        self.config = config
        if let session {
            self.session = session
        } else {
            let cfg = URLSessionConfiguration.default
            cfg.timeoutIntervalForRequest = 20
            cfg.timeoutIntervalForResource = 60
            cfg.waitsForConnectivity = true
            self.session = URLSession(configuration: cfg)
        }
    }

    func testConnection() async throws -> String {
        let entries = try await listDirectory(at: config.normalizedPath)
        return "已连接，共 \(entries.count) 项"
    }

    func listDirectory(at path: String) async throws -> [NASEntry] {
        let url = try makeURL(path: path, directory: true)
        let data = try await fetchData(from: url)
        let entries = try parseListing(data: data, pageURL: url, parentPath: normalizePath(path))
        if entries.isEmpty { throw NASError.emptyListing }
        return entries.sorted { lhs, rhs in
            if lhs.kind == .folder && rhs.kind != .folder { return true }
            if lhs.kind != .folder && rhs.kind == .folder { return false }
            return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
        }
    }

    func listSongs(under path: String) async throws -> [Song] {
        let entries = try await listDirectory(at: path)
        return entries.compactMap { entry -> Song? in
            guard entry.kind == .audio || entry.kind == .video else { return nil }
            return song(from: entry)
        }
    }

    func loadManifest(at path: String? = nil) async throws -> [Song] {
        let manifestRel = path ?? (config.normalizedPath + "/library.json")
        let url = try makeURL(path: manifestRel, directory: false)
        let data = try await fetchData(from: url)
        if let songs = try? decodeJSONManifest(data) { return songs }
        if let text = String(data: data, encoding: .utf8),
           text.contains("#EXTM3U") || text.lowercased().contains(".mp3") {
            return parseM3U(text, basePath: config.normalizedPath)
        }
        throw NASError.unsupportedContent
    }

    func resolvePlaybackURL(for song: Song) async throws -> URL {
        if let url = song.mediaURL { return url }
        if let rel = song.relativePath {
            return try makeURL(path: rel, directory: false)
        }
        throw NASError.invalidURL
    }

    // MARK: - Networking

    private func fetchData(from url: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/html,application/json,*/*", forHTTPHeaderField: "Accept")
        if let auth = basicAuthHeader() {
            request.setValue(auth, forHTTPHeaderField: "Authorization")
        }

        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                throw NASError.transport("无效响应")
            }
            switch http.statusCode {
            case 200...299:
                return data
            case 401:
                throw NASError.unauthorized
            case 403:
                throw NASError.forbidden
            case 404:
                throw NASError.notFound
            default:
                let msg = HTTPURLResponse.localizedString(forStatusCode: http.statusCode)
                throw NASError.server(http.statusCode, msg)
            }
        } catch let error as NASError {
            throw error
        } catch let error as URLError {
            switch error.code {
            case .timedOut:
                throw NASError.timeout
            case .userAuthenticationRequired:
                throw NASError.unauthorized
            default:
                throw NASError.transport(error.localizedDescription)
            }
        } catch {
            throw NASError.transport(error.localizedDescription)
        }
    }

    private func basicAuthHeader() -> String? {
        let user = KeychainCredentialStore.read(account: .username) ?? ""
        let pass = KeychainCredentialStore.read(account: .password) ?? ""
        guard !user.isEmpty || !pass.isEmpty else { return nil }
        let raw = "\(user):\(pass)"
        guard let data = raw.data(using: .utf8) else { return nil }
        return "Basic \(data.base64EncodedString())"
    }

    private func makeURL(path: String, directory: Bool) throws -> URL {
        guard var base = URL(string: config.normalizedBaseURLString) else {
            throw NASError.invalidURL
        }
        // Ensure scheme present
        if base.scheme == nil {
            throw NASError.invalidURL
        }
        let rel = normalizePath(path).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if !rel.isEmpty {
            base = base.appendingPathComponent(rel)
        }
        if directory && !base.absoluteString.hasSuffix("/") {
            return URL(string: base.absoluteString + "/") ?? base
        }
        return base
    }

    private func normalizePath(_ path: String) -> String {
        var p = path.trimmingCharacters(in: .whitespacesAndNewlines)
        if p.isEmpty { return config.normalizedPath }
        if !p.hasPrefix("/") { p = "/" + p }
        return p
    }

    // MARK: - Parsing

    private func parseListing(data: Data, pageURL: URL, parentPath: String) throws -> [NASEntry] {
        if let jsonSongs = try? decodeJSONManifest(data) {
            return jsonSongs.map { song in
                NASEntry(
                    id: song.id,
                    name: song.title,
                    path: song.relativePath ?? parentPath,
                    kind: .audio,
                    absoluteURL: song.mediaURL
                )
            }
        }

        guard let html = String(data: data, encoding: .utf8)
                ?? String(data: data, encoding: .isoLatin1) else {
            throw NASError.unsupportedContent
        }

        // Generic directory index: collect hrefs
        let pattern = #"href\s*=\s*\"([^\"]+)\""#
        let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
        let range = NSRange(html.startIndex..<html.endIndex, in: html)
        var seen = Set<String>()
        var result: [NASEntry] = []

        regex?.enumerateMatches(in: html, options: [], range: range) { match, _, _ in
            guard let match,
                  let r = Range(match.range(at: 1), in: html) else { return }
            let href = String(html[r])
            if href.hasPrefix("#") || href.hasPrefix("?") { return }
            if href.lowercased().hasPrefix("javascript:") { return }
            if href == "../" || href == "./" || href == "/" { return }

            guard let absolute = URL(string: href, relativeTo: pageURL)?.absoluteURL else { return }
            let name = absolute.lastPathComponent.removingPercentEncoding ?? absolute.lastPathComponent
            if name.isEmpty || name == ".." { return }
            let key = absolute.absoluteString
            if seen.contains(key) { return }
            seen.insert(key)

            let childPath = path(from: absolute) ?? (parentPath + "/" + name)
            let kind = classify(name: name, href: href, url: absolute)
            result.append(
                NASEntry(
                    id: key,
                    name: name.trimmingCharacters(in: CharacterSet(charactersIn: "/")),
                    path: childPath,
                    kind: kind,
                    absoluteURL: absolute
                )
            )
        }

        return result
    }

    private func path(from url: URL) -> String? {
        guard let base = URL(string: config.normalizedBaseURLString) else { return url.path }
        let basePath = base.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        var path = url.path
        if !basePath.isEmpty, path.hasPrefix("/\(basePath)") {
            path = String(path.dropFirst(basePath.count + 1))
            if !path.hasPrefix("/") { path = "/" + path }
        }
        return path.isEmpty ? "/" : path
    }

    private func classify(name: String, href: String, url: URL) -> NASEntry.Kind {
        let lower = name.lowercased()
        if href.hasSuffix("/") || url.hasDirectoryPath { return .folder }
        if lower.hasSuffix(".json") || lower == "library.json" || lower.hasSuffix(".m3u") || lower.hasSuffix(".m3u8") {
            return .manifest
        }
        let ext = (lower as NSString).pathExtension
        if config.audioExtensions.contains(ext) {
            if ["mp4", "m4v", "mov", "mkv"].contains(ext) { return .video }
            return .audio
        }
        return .other
    }

    private func song(from entry: NASEntry) -> Song {
        let baseName = (entry.name as NSString).deletingPathExtension
        let parts = baseName.split(separator: "-", maxSplits: 1).map {
            $0.trimmingCharacters(in: .whitespaces)
        }
        let title = parts.first.map(String.init) ?? baseName
        let artist = parts.count > 1 ? parts[1] : "未知歌手"
        return Song(
            id: entry.id,
            title: title,
            artist: artist,
            mediaURL: entry.absoluteURL,
            source: .nas,
            relativePath: entry.path
        )
    }

    private struct ManifestFile: Decodable {
        struct Item: Decodable {
            var id: String?
            var title: String
            var artist: String?
            var path: String?
            var url: String?
            var duration: Double?
        }
        var songs: [Item]?
        var tracks: [Item]?
    }

    private func decodeJSONManifest(_ data: Data) throws -> [Song] {
        let file = try JSONDecoder().decode(ManifestFile.self, from: data)
        let items = file.songs ?? file.tracks ?? []
        return items.enumerated().map { idx, item in
            let rel = item.path
            let media = item.url.flatMap(URL.init(string:))
                ?? rel.flatMap { config.url(forRelativePath: $0) }
            return Song(
                id: item.id ?? "manifest-\(idx)-\(item.title)",
                title: item.title,
                artist: item.artist ?? "未知歌手",
                duration: item.duration ?? 240,
                mediaURL: media,
                source: .nas,
                relativePath: rel
            )
        }
    }

    private func parseM3U(_ text: String, basePath: String) -> [Song] {
        var songs: [Song] = []
        var pendingTitle: String?
        for raw in text.components(separatedBy: .newlines) {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty { continue }
            if line.hasPrefix("#EXTINF:") {
                pendingTitle = line.split(separator: ",").last.map(String.init)
                continue
            }
            if line.hasPrefix("#") { continue }
            let name = URL(fileURLWithPath: line).lastPathComponent
            let title = pendingTitle ?? (name as NSString).deletingPathExtension
            pendingTitle = nil
            let rel: String
            if line.hasPrefix("http") {
                songs.append(
                    Song(
                        id: line,
                        title: title,
                        artist: "NAS",
                        mediaURL: URL(string: line),
                        source: .nas,
                        relativePath: nil
                    )
                )
                continue
            } else if line.hasPrefix("/") {
                rel = line
            } else {
                rel = basePath + "/" + line
            }
            songs.append(
                Song(
                    id: rel,
                    title: title,
                    artist: "NAS",
                    mediaURL: config.url(forRelativePath: rel),
                    source: .nas,
                    relativePath: rel
                )
            )
        }
        return songs
    }
}

/// 保留名称兼容；默认走 HTTPS 实现。
typealias StubNASMediaSource = HTTPSNASMediaSource
