import SwiftUI

struct KidsView: View {
    @Environment(AppStore.self) private var store
    private let page = MockData.kidsPage

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 10) {
                Image(systemName: "face.smiling")
                    .foregroundStyle(AppTheme.textSecondary)
                Text("儿童内容 · 建议家长陪同")
                    .font(.system(size: 22))
                    .foregroundStyle(AppTheme.textSecondary)
            }

            ShelfHeader(title: "儿童", subtitle: "儿歌与启蒙")

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 18) {
                    ForEach(Array(page.posters.enumerated()), id: \.element.id) { idx, card in
                        Button { store.openPlayback() } label: {
                            PosterPlaceholder(
                                symbol: card.symbolName,
                                title: card.title,
                                subtitle: card.subtitle,
                                hue: 0.35 + Double(idx) * 0.08
                            )
                            .frame(width: 220, height: 300)
                        }
                        .buttonStyle(.card)
                    }
                }
            }

            Text("品牌专区")
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)

            HStack(spacing: 18) {
                ForEach(page.brands) { card in
                    Button { store.openPlayback() } label: {
                        HStack(spacing: 16) {
                            Image(systemName: card.symbolName)
                                .font(.system(size: 32))
                            Text(card.title)
                                .font(.system(size: 26, weight: .semibold))
                            Spacer()
                        }
                        .foregroundStyle(AppTheme.textPrimary)
                        .padding(24)
                        .frame(maxWidth: .infinity, minHeight: 120)
                        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous))
                    }
                    .buttonStyle(.card)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 32)
        .focusSection()
    }
}
