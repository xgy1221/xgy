import SwiftUI

struct RootView: View {
    @Environment(AppStore.self) private var store

    var body: some View {
        ZStack {
            AppTheme.backgroundGradient.ignoresSafeArea()

            VStack(spacing: 0) {
                TopBarView()
                CategoryTabBar(selection: Bindable(store).selectedTab)
                content
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding(.top, 8)
            }

            if store.isQueuePresented {
                QueueSheetView()
                    .zIndex(5)
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
                        .background(Color.black.opacity(0.75), in: Capsule())
                        .padding(.bottom, 60)
                }
                .zIndex(20)
                .transition(.opacity)
                .allowsHitTesting(false)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: store.isPlaybackPresented)
        .animation(.easeInOut(duration: 0.2), value: store.isGuessPresented)
        .animation(.easeInOut(duration: 0.2), value: store.isQueuePresented)
        .animation(.easeInOut(duration: 0.2), value: store.toastMessage)
    }

    @ViewBuilder
    private var content: some View {
        switch store.selectedTab {
        case .freeSing:
            HomeGridView()
        case .singers:
            SingersView()
        case .hot:
            HotView()
        case .kids:
            KidsView()
        case .nasLibrary:
            NASLibraryView()
        default:
            PlaceholderTabView(tab: store.selectedTab)
        }
    }
}

struct PlaceholderTabView: View {
    let tab: MainTab

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "rectangle.stack")
                .font(.system(size: 64))
                .foregroundStyle(.white.opacity(0.5))
            Text(tab.title)
                .font(.system(size: 40, weight: .bold))
                .foregroundStyle(.white)
            Text("内容页占位 · 后续接入曲库 / NAS")
                .font(.system(size: 24))
                .foregroundStyle(.white.opacity(0.55))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .focusable()
    }
}
