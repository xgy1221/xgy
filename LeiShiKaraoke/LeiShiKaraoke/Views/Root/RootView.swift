import SwiftUI

struct RootView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ZStack {
            AppTheme.ambientBackground

            VStack(spacing: 0) {
                TopBarView()
                CategoryTabBar(selection: Bindable(store).selectedTab)
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding(.top, 12)
            }

            if store.isQueuePresented {
                QueueSheetView().zIndex(5)
            }
            if store.isSettingsPresented {
                SettingsView().zIndex(6)
            }
            if store.isPlaybackPresented {
                PlaybackOverlayView()
                    .transition(.opacity)
                    .zIndex(10)
            }
            if let toast = store.toastMessage {
                VStack {
                    Spacer()
                    Text(toast)
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 16)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(.bottom, 56)
                }
                .zIndex(20)
                .allowsHitTesting(false)
            }
        }
        .animation(.easeInOut(duration: 0.22), value: store.isPlaybackPresented)
        .animation(.easeInOut(duration: 0.2), value: store.isGuessPresented)
        .animation(.easeInOut(duration: 0.2), value: store.isQueuePresented)
        .animation(.easeInOut(duration: 0.2), value: store.isSettingsPresented)
        .animation(.easeInOut(duration: 0.2), value: store.toastMessage)
    }

    @ViewBuilder
    private var content: some View {
        switch store.selectedTab {
        case .freeSing: HomeGridView()
        case .singers: SingersView()
        case .hot: HotView()
        case .kids: KidsView()
        case .nasLibrary: NASLibraryView()
        default: PlaceholderTabView(tab: store.selectedTab)
        }
    }
}

struct PlaceholderTabView: View {
    let tab: MainTab

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.stack.3d.up")
                .font(.system(size: 56, weight: .light))
                .foregroundStyle(AppTheme.textTertiary)
            Text(tab.title)
                .font(.system(size: 36, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
            Text("内容即将接入")
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .focusable()
    }
}
