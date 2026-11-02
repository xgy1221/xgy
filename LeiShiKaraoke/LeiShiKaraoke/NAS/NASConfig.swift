import Foundation

enum NASProtocolKind: String, CaseIterable, Identifiable, Codable {
    case https
    case http
    case webdav

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .https: return "HTTPS"
        case .http: return "HTTP"
        case .webdav: return "WebDAV"
        }
    }
}

/// 非机密配置（URL/路径）可进 UserDefaults；密码只走 Keychain。
struct NASConfig: Equatable {
    var baseURLString: String
    var libraryPath: String
    var protocolKind: NASProtocolKind
    var audioExtensions: [String]

    static let defaultAudioExtensions = [
        "mp3", "m4a", "aac", "flac", "wav", "aiff", "alac",
        "mp4", "m4v", "mov", "mkv",
    ]

    static var `default`: NASConfig {
        NASConfig(
            baseURLString: BrandConfig.defaultNASBaseURL,
            libraryPath: BrandConfig.defaultNASLibraryPath,
            protocolKind: .https,
            audioExtensions: defaultAudioExtensions
        )
    }

    var normalizedBaseURLString: String {
        baseURLString.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    var normalizedPath: String {
        var p = libraryPath.trimmingCharacters(in: .whitespacesAndNewlines)
        if p.isEmpty { p = "/" }
        if !p.hasPrefix("/") { p = "/" + p }
        while p.count > 1 && p.hasSuffix("/") { p.removeLast() }
        return p
    }

    var rootURL: URL? {
        guard let base = URL(string: normalizedBaseURLString) else { return nil }
        return base.appendingPathComponent(normalizedPath.trimmingCharacters(in: CharacterSet(charactersIn: "/")))
    }

    func url(forRelativePath path: String) -> URL? {
        guard let base = URL(string: normalizedBaseURLString) else { return nil }
        let trimmed = path.hasPrefix("/") ? String(path.dropFirst()) : path
        return base.appendingPathComponent(trimmed)
    }
}

enum NASConfigStore {
    private static let baseKey = "nas.baseURL"
    private static let pathKey = "nas.libraryPath"
    private static let protoKey = "nas.protocol"

    static func load() -> NASConfig {
        let defaults = UserDefaults.standard
        var config = NASConfig.default
        if let base = defaults.string(forKey: baseKey), !base.isEmpty {
            config.baseURLString = base
        }
        if let path = defaults.string(forKey: pathKey), !path.isEmpty {
            config.libraryPath = path
        }
        if let raw = defaults.string(forKey: protoKey),
           let kind = NASProtocolKind(rawValue: raw) {
            config.protocolKind = kind
        }
        return config
    }

    static func save(_ config: NASConfig) {
        let defaults = UserDefaults.standard
        defaults.set(config.normalizedBaseURLString, forKey: baseKey)
        defaults.set(config.normalizedPath, forKey: pathKey)
        defaults.set(config.protocolKind.rawValue, forKey: protoKey)
    }
}
