import Foundation
import Observation
import SwiftUI

@Observable
final class AppStore {
    var selectedTab: MainTab = .freeSing
    var isPlaybackPresented = false
    var isGuessPresented = false
    var isQueuePresented = false
    var isSettingsPresented = false
    var toastMessage: String?

    let catalog: CatalogProviding
    let playback: PlaybackStore
    let nas: HTTPSNASMediaSource
    let microphone: StubMicrophoneInput

    init(
        catalog: CatalogProviding = MockCatalogService(),
        playback: PlaybackStore = PlaybackStore(),
        nas: HTTPSNASMediaSource = HTTPSNASMediaSource(),
        microphone: StubMicrophoneInput = StubMicrophoneInput()
    ) {
        self.catalog = catalog
        self.playback = playback
        self.nas = nas
        self.microphone = microphone
    }

    func openPlayback(starting song: Song? = nil) {
        if let song {
            playback.play(song)
        } else if playback.nowPlaying == nil {
            playback.play(MockData.nowPlaying)
            playback.nextSong = MockData.nextSong
        }
        isGuessPresented = false
        isPlaybackPresented = true
    }

    func dismissPlayback() {
        isGuessPresented = false
        isPlaybackPresented = false
    }

    func addToQueue(_ song: Song) {
        playback.enqueue(song)
        showToast("已加入点唱：\(song.title)")
    }

    func showToast(_ message: String) {
        toastMessage = message
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            if toastMessage == message {
                toastMessage = nil
            }
        }
    }

    func reloadNASConfigFromStore() {
        nas.config = NASConfigStore.load()
    }
}

@Observable
final class PlaybackStore: MediaPlaybackControlling {
    var nowPlaying: Song?
    var nextSong: Song? = MockData.nextSong
    var isPlaying: Bool = true
    var currentTime: TimeInterval = 334
    var vocalMode: VocalMode = .accompaniment
    var orderMode: PlayOrderMode = .sequential
    var queue: [Song] = Array(MockData.aiSongs.prefix(4))
    var isFavorite: Bool = false

    var queueCount: Int { queue.count }

    func play(_ song: Song) {
        nowPlaying = song
        isPlaying = true
        currentTime = min(30, max(0, song.duration * 0.1))
    }

    func enqueue(_ song: Song) {
        queue.append(song)
        if nextSong == nil {
            nextSong = song
        }
    }

    func togglePlayPause() { isPlaying.toggle() }

    func next() {
        if let next = nextSong {
            nowPlaying = next
            if !queue.isEmpty { queue.removeFirst() }
            nextSong = queue.first ?? MockData.aiSongs.randomElement()
            currentTime = 0
            isPlaying = true
        }
    }

    func replay() {
        currentTime = 0
        isPlaying = true
    }

    func skipPrelude() {
        currentTime = min(currentTime + 30, (nowPlaying?.duration ?? 300) - 1)
    }

    func toggleVocalMode() {
        vocalMode = vocalMode == .accompaniment ? .original : .accompaniment
    }

    func cycleOrderMode() {
        switch orderMode {
        case .sequential: orderMode = .shuffle
        case .shuffle: orderMode = .loop
        case .loop: orderMode = .sequential
        }
    }

    func seek(to time: TimeInterval) {
        let total = nowPlaying?.duration ?? 300
        currentTime = min(max(0, time), total)
    }

    func toggleFavorite() { isFavorite.toggle() }

    var progress: Double {
        let total = nowPlaying?.duration ?? 1
        return total > 0 ? currentTime / total : 0
    }

    func formatTime(_ t: TimeInterval) -> String {
        let m = Int(t) / 60
        let s = Int(t) % 60
        return String(format: "%02d:%02d", m, s)
    }
}
