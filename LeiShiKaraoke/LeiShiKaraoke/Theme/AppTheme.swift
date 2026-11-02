import SwiftUI

enum AppTheme {
    static let backgroundTop = Color(red: 0.04, green: 0.05, blue: 0.10)
    static let backgroundBottom = Color(red: 0.10, green: 0.07, blue: 0.18)
    static let tabSelected = Color(red: 0.90, green: 0.16, blue: 0.18)
    static let focusOrange = Color(red: 1.0, green: 0.55, blue: 0.12)
    static let vipGold = Color(red: 0.95, green: 0.78, blue: 0.35)
    static let accentRed = Color(red: 0.92, green: 0.18, blue: 0.20)
    static let pillFill = Color.white.opacity(0.12)
    static let cardRadius: CGFloat = 16
    static let contentPadding: CGFloat = 48

    static var backgroundGradient: LinearGradient {
        LinearGradient(
            colors: [backgroundTop, backgroundBottom],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

extension View {
    func karaokeCardStyle(cornerRadius: CGFloat = AppTheme.cardRadius) -> some View {
        self
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }
}
