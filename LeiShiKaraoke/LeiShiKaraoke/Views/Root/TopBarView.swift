import SwiftUI

struct TopBarView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        HStack(spacing: 16) {
            brand
            Spacer(minLength: 12)
            // 主操作优先：搜索 / 已点 / 微信点歌
            utilityButton(systemName: "magnifyingglass", title: "搜索") {
                store.showToast("搜索 / 首拼 · 后续接入")
            }
            queueButton
            utilityButton(systemName: "iphone", title: "微信点歌") {
                store.showToast("微信点歌二维码 · 占位")
            }
            utilityButton(systemName: "person.crop.circle", title: "我的") {}
            utilityButton(systemName: "gearshape.fill", title: "设置") {
                store.selectedTab = .nasLibrary
                store.showToast("设置 / NAS 音源入口见 NAS曲库")
            }
            // VIP 弱化靠右，不抢主焦点
            vipPill
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.top, 24)
        .padding(.bottom, 8)
        .focusSection()
    }

    private var brand: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color.red, Color.orange],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 48, height: 48)
                Image(systemName: "mic.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)
            }
            Text(BrandConfig.logoTitle)
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(.white)
            Text(BrandConfig.editionBadge)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Color.black.opacity(0.85))
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(AppTheme.vipGold, in: Capsule())
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(BrandConfig.logoTitle) \(BrandConfig.editionBadge)")
    }

    private var vipPill: some View {
        Button {
            store.showToast("VIP 占位 · 自有曲库可不启用")
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "diamond.fill")
                    .font(.system(size: 14))
                VStack(alignment: .leading, spacing: 0) {
                    Text(BrandConfig.vipHeadline)
                        .font(.system(size: 16, weight: .semibold))
                    Text(BrandConfig.vipSubtitle)
                        .font(.system(size: 12))
                        .opacity(0.8)
                }
            }
            .foregroundStyle(AppTheme.vipGold.opacity(0.9))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .strokeBorder(AppTheme.vipGold.opacity(0.55), lineWidth: 1.5)
                    .background(Capsule().fill(Color.black.opacity(0.25)))
            )
        }
        .buttonStyle(.card)
        .opacity(0.85)
    }

    private var queueButton: some View {
        Button {
            store.isQueuePresented = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "list.bullet")
                    .font(.system(size: 18, weight: .semibold))
                Text("已点")
                    .font(.system(size: 20, weight: .medium))
                Text("\(store.playback.queueCount)")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(AppTheme.accentRed, in: Capsule())
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(AppTheme.pillFill, in: Capsule())
        }
        .buttonStyle(.card)
    }

    private func utilityButton(systemName: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemName)
                    .font(.system(size: 18, weight: .semibold))
                Text(title)
                    .font(.system(size: 20, weight: .medium))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(AppTheme.pillFill, in: Capsule())
        }
        .buttonStyle(.card)
    }
}
