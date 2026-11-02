import Foundation
import Observation

enum MicrophoneRouteKind: String, Equatable {
    case none
    case systemBluetooth
    case companionDevice
    case externalHardware

    var displayName: String {
        switch self {
        case .none: return "未连接"
        case .systemBluetooth: return "系统蓝牙"
        case .companionDevice: return "手机伴侣"
        case .externalHardware: return "外接音频"
        }
    }
}

struct MicrophoneStatus: Equatable {
    var route: MicrophoneRouteKind = .none
    var isActive: Bool = false
    var level: Float = 0 // 0...1
    var detail: String = "未连接麦克风"
}

/// 演唱输入抽象。tvOS 系统 BT 麦不可假设可用；实现可换伴侣/外接/系统探测。
protocol MicrophoneInput: AnyObject {
    var status: MicrophoneStatus { get }
    func start() async throws
    func stop()
    func refreshRoute() async
}

enum MicrophoneError: LocalizedError {
    case unavailable
    case permissionDenied
    case companionNotLinked

    var errorDescription: String? {
        switch self {
        case .unavailable: return "当前设备无可用演唱输入"
        case .permissionDenied: return "麦克风权限未授予"
        case .companionNotLinked: return "尚未连接 iPhone 伴侣 App"
        }
    }
}

/// 模拟电平，便于 UI 联调；不访问真实音频硬件。
@Observable
final class StubMicrophoneInput: MicrophoneInput {
    private(set) var status = MicrophoneStatus()
    private var timerTask: Task<Void, Never>?

    func start() async throws {
        status.route = .none
        status.isActive = true
        status.detail = "Stub 麦（无真实输入）· 请接伴侣 App 或外接方案"
        timerTask?.cancel()
        timerTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 120_000_000)
                await MainActor.run {
                    self?.status.level = Float.random(in: 0.05...0.55)
                }
            }
        }
    }

    func stop() {
        timerTask?.cancel()
        timerTask = nil
        status.isActive = false
        status.level = 0
        status.detail = "已停止"
    }

    func refreshRoute() async {
        // stub：保持 none，真实实现在此探测 AVAudioSession / 伴侣连接
        if status.route == .none {
            status.detail = "未检测到系统输入；建议 iPhone 伴侣或外接麦"
        }
    }
}

/// 预留：iPhone/iPad 采集人声后推到 TV。
final class CompanionMicInput: MicrophoneInput {
    private(set) var status = MicrophoneStatus(
        route: .companionDevice,
        isActive: false,
        level: 0,
        detail: "伴侣麦未实装"
    )

    func start() async throws { throw MicrophoneError.companionNotLinked }
    func stop() { status.isActive = false }
    func refreshRoute() async {}
}

/// 预留：系统蓝牙/有线输入探测。
final class SystemAudioMicInput: MicrophoneInput {
    private(set) var status = MicrophoneStatus(
        route: .systemBluetooth,
        isActive: false,
        level: 0,
        detail: "系统麦探测未实装"
    )

    func start() async throws { throw MicrophoneError.unavailable }
    func stop() { status.isActive = false }
    func refreshRoute() async {}
}
