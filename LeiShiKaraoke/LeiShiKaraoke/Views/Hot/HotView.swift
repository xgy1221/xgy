import SwiftUI

struct HotView: View {
    @Environment(AppStore.self) private var store
    private let page = MockData.hotPage

    var body: some View {
        HStack(alignment: .top, spacing: 22) {
            aiSidebar
                .frame(width: 360)
            VStack(spacing: 20) {
                HStack(spacing: 18) {
                    ForEach(page.posters) { card in
                        posterCard(card)
                    }
                }
                .frame(height: 320)

                HStack(spacing: 18) {
                    ForEach(page.bottomCards) { card in
                        bottomCard(card)
                    }
                }
                .frame(height: 140)
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.bottom, 28)
        .focusSection()
    }

    private var aiSidebar: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Image(systemName: "waveform.path.ecg")
                    .font(.system(size: 22, weight: .bold))
                Text("AI推荐")
                    .font(.system(size: 26, weight: .bold))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .padding(.vertical, 16)

            ForEach(page.aiItems) { item in
                Button {
                    store.openPlayback(starting: item.song)
                } label: {
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(item.song.coverColor)
                            .frame(width: 56, height: 56)
                            .overlay {
                                if item.song.isVIP {
                                    Text("VIP")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundStyle(.black)
                                        .padding(3)
                                        .background(AppTheme.vipGold)
                                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                                }
                            }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.song.displayTitle)
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(item.song.artist)
                                .font(.system(size: 18))
                                .foregroundStyle(.white.opacity(0.7))
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }

            Spacer(minLength: 8)

            Button {
                store.showToast("查看更多 AI 推荐 · 占位")
            } label: {
                HStack {
                    Image(systemName: "music.note")
                    Text("查看更多 >>")
                        .font(.system(size: 22, weight: .semibold))
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
            }
            .buttonStyle(.plain)
        }
        .background(
            RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                .fill(AppTheme.accentRed.opacity(0.92))
        )
        .focusSection()
    }

    private func posterCard(_ card: PlaylistCard) -> some View {
        Button {
            store.openPlayback()
        } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous)
                    .fill(card.tint.gradient)
                VStack(alignment: .leading, spacing: 6) {
                    if let badge = card.badge {
                        Text(badge)
                            .font(.system(size: 14, weight: .bold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(.black.opacity(0.45), in: Capsule())
                    }
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

    private func bottomCard(_ card: PlaylistCard) -> some View {
        Button {
            store.openPlayback()
        } label: {
            HStack(spacing: 14) {
                Image(systemName: card.symbolName)
                    .font(.system(size: 32))
                Text(card.title)
                    .font(.system(size: 24, weight: .bold))
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
            .foregroundStyle(.white)
            .padding(18)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(card.tint, in: RoundedRectangle(cornerRadius: AppTheme.cardRadius, style: .continuous))
        }
        .buttonStyle(.card)
    }
}
