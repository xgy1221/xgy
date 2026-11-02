import SwiftUI

struct CategoryTabBar: View {
    @Binding var selection: MainTab
    @FocusState private var focusedTab: MainTab?

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(MainTab.allCases) { tab in
                    Button {
                        selection = tab
                    } label: {
                        Text(tab.title)
                            .font(.system(size: 28, weight: selection == tab ? .bold : .medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 22)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(selection == tab ? AppTheme.tabSelected : Color.clear)
                            )
                    }
                    .buttonStyle(.plain)
                    .focused($focusedTab, equals: tab)
                }
            }
            .padding(.horizontal, AppTheme.contentPadding)
            .padding(.vertical, 8)
        }
        .focusSection()
        .onChange(of: focusedTab) { _, newValue in
            if let newValue {
                selection = newValue
            }
        }
        .onAppear {
            focusedTab = selection
        }
        .onChange(of: selection) { _, newValue in
            if focusedTab != newValue {
                focusedTab = newValue
            }
        }
    }
}
