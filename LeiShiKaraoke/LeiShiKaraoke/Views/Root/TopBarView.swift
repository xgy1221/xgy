import SwiftUI

struct TopBarView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        HStack(spacing: 18) {
            brand
            Spacer(minLength: 24)
            iconButton("magnifyingglass", "搜索") {
                store.showToast("搜索 · 后续接入")
            }
            queueButton
            iconButton("qrcode", "微信点歌") {
                store.showToast("微信点歌 · 占位")
            }
            iconButton("person.crop.circle", "我的") {}
            iconButton("gearshape", "设置") {
                store.isSettingsPresented = true
            }
        }
        .padding(.horizontal, AppTheme.contentPadding)
        .padding(.top, 28)
        .padding(.bottom, 10)
        .focusSection()
    }

    private var brand: some View {
        HStack(spacing: 14) {
            Image(systemName: "music.mic")
                .font(.system(size: 28, weight: .semibold))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(.white)
            VStack(alignment: .leading, spacing: 0) {
                Text(BrandConfig.logoTitle)
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(BrandConfig.tagline)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var queueButton: some View {
        Button {
            store.isQueuePresented = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "list.bullet")
                Text("已点")
                Text("\(store.playback.queueCount)")
                    .font(.system(size: 16, weight: .bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(AppTheme.surfaceStrong, in: Capsule())
            }
            .font(.system(size: 20, weight: .medium))
            .foregroundStyle(AppTheme.textPrimary)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
        }
        .buttonStyle(.card)
    }

    private func iconButton(_ system: String, _ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: system)
                Text(title)
            }
            .font(.system(size: 20, weight: .medium))
            .foregroundStyle(AppTheme.textPrimary)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
        }
        .buttonStyle(.card)
    }
}
