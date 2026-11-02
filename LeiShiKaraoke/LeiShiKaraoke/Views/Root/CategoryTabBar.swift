import SwiftUI

struct CategoryTabBar: View {
    @Binding var selection: MainTab
    @FocusState private var focusedTab: MainTab?

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(MainTab.allCases) { tab in
                    Button {
                        selection = tab
                    } label: {
                        VStack(spacing: 8) {
                            Text(tab.title)
                                .font(.system(size: 26, weight: selection == tab ? .bold : .regular))
                                .foregroundStyle(selection == tab ? AppTheme.textPrimary : AppTheme.textSecondary)
                            Capsule()
                                .fill(selection == tab ? Color.white : Color.clear)
                                .frame(width: selection == tab ? 28 : 0, height: 3)
                        }
                        .padding(.horizontal, 18)
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                    .focused($focusedTab, equals: tab)
                }
            }
            .padding(.horizontal, AppTheme.contentPadding)
        }
        .focusSection()
        .onChange(of: focusedTab) { _, newValue in
            if let newValue { selection = newValue }
        }
        .onAppear { focusedTab = selection }
        .onChange(of: selection) { _, newValue in
            if focusedTab != newValue { focusedTab = newValue }
        }
    }
}
