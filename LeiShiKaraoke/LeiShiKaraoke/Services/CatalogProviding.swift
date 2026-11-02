import Foundation

/// 曲库接入点：后续换成 JSON / 自建 API 实现，View 层只依赖本协议。
protocol CatalogProviding {
    func homeTiles() async -> [HomeTile]
    func featuredSingers() async -> [Singer]
    func singerGrid() async -> [Singer]
    func singerRegions() async -> [PlaylistCard]
    func hotPage() async -> HotPageModel
    func kidsPage() async -> KidsPageModel
    func search(keyword: String) async -> [Song]
    func guessYouWant() async -> (songs: [GuessSongCard], playlists: [GuessPlaylistCard])
}

struct MockCatalogService: CatalogProviding {
    func homeTiles() async -> [HomeTile] { MockData.homeTiles }
    func featuredSingers() async -> [Singer] { MockData.featuredSingers }
    func singerGrid() async -> [Singer] { MockData.singerGrid }
    func singerRegions() async -> [PlaylistCard] { MockData.singerRegions }
    func hotPage() async -> HotPageModel { MockData.hotPage }
    func kidsPage() async -> KidsPageModel { MockData.kidsPage }
    func search(keyword: String) async -> [Song] {
        let all = MockData.aiSongs + [MockData.nowPlaying, MockData.nextSong]
        guard !keyword.isEmpty else { return all }
        return all.filter {
            $0.title.localizedCaseInsensitiveContains(keyword)
                || $0.artist.localizedCaseInsensitiveContains(keyword)
        }
    }
    func guessYouWant() async -> (songs: [GuessSongCard], playlists: [GuessPlaylistCard]) {
        (MockData.guessSongs, MockData.guessPlaylists)
    }
}

/// 播放控制接入点：后续接到 AVPlayer。
protocol MediaPlaybackControlling: AnyObject {
    var nowPlaying: Song? { get }
    var nextSong: Song? { get }
    var isPlaying: Bool { get }
    var currentTime: TimeInterval { get }
    var vocalMode: VocalMode { get }
    var orderMode: PlayOrderMode { get }
    var queueCount: Int { get }

    func play(_ song: Song)
    func togglePlayPause()
    func next()
    func replay()
    func skipPrelude()
    func toggleVocalMode()
    func cycleOrderMode()
    func seek(to time: TimeInterval)
}
