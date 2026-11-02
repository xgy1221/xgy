import SwiftUI

/// Apple TV / tvOS HIG：近黑沉浸、克制表面、系统 Focus。
enum AppTheme {
    static let background = Color.black
    static let backgroundElevated = Color(white: 0.07)
    static let surface = Color.white.opacity(0.08)
    static let surfaceStrong = Color.white.opacity(0.12)
    static let textPrimary = Color.white
    static let textSecondary = Color.white.opacity(0.55)
    static let textTertiary = Color.white.opacity(0.35)
    static let accent = Color.white
    static let accentSoft = Color.blue.opacity(0.85)
    static let danger = Color.red.opacity(0.85)
    static let cardRadius: CGFloat = 18
    static let contentPadding: CGFloat = 60
    static let shelfSpacing: CGFloat = 28

    static var ambientBackground: some View {
        ZStack {
            Color.black
            RadialGradient(
                colors: [Color(white: 0.14), Color.black],
                center: .topLeading,
                startRadius: 40,
                endRadius: 900
            )
            .opacity(0.9)
        }
        .ignoresSafeArea()
    }
}

struct PosterPlaceholder: View {
    var symbol: String
    var title: String
    var subtitle: String? = nil
    var hue: Double = 0.6

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color(hue: hue, saturation: 0.35, brightness: 0.28),
                            Color(hue: hue, saturation: 0.20, brightness: 0.12),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Image(systemName: symbol)
                .font(.system(size: 44, weight: .light))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(.white.opacity(0.35))
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 18, weight: .regular))
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }
            .padding(18)
        }
    }
}

struct ShelfHeader: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 20))
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
    }
}
