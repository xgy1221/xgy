import SwiftUI

struct KidsView: View {
    @Environment(AppStore.self) private var store
    private let page = MockData.kidsPage

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 10) {
                Image(systemName: "face.smiling.fill")
                    .foregroundStyle(.cyan)
                Text("儿童内容 · 建议家长陪同")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))
            }
            .padding(.leading, 4)

            HStack(spacing: 16) {
                ForEach(page.posters) { card in
                    Button {
                        store.openPlayback()
                    } label: {
                        ZStack(alignment: .bottomLeading) {
                            RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                                .fill(card.tint.gradient)
                            VStack(alignment: .leading, spacing: 6) {
                                Image(systemName: card.symbolName)
                                    .font(.system(size: 36))
                                Spacer()
                                Text(card.title)
                                    .font(.system(size: 24, weight: .bold))
                                if let subtitle = card.subtitle {
                                    Text(subtitle)
                                        .font(.system(size: 16))
                                        .opacity(0.85)
                                }
                            }
                            .foregroundStyle(.white)
                            .padding(16)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.card)
                }
            }
            .frame(height: 300)

            HStack(spacing: 18) {
                ForEach(page.brands) { card in
                    Button {
                        store.openPlayback()
                    } label: {
                        HStack(spacing: 16) {
                            Image(systemName: card.symbolName)
                                .font(.system(size: 40))
                            Text(card.title)
                                .font(.system(size: 30, weight: .bold))
                            Spacer()
                        }
                        .foregroundStyle(.white)
                        .padding(24)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(card.tint, in: RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous))
                    }
                    .buttonStyle(.card)
                }
            }
            .frame(height: 150)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 28)
        .focusSection()
    }
}
