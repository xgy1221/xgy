import SwiftUI

struct SettingsView: View {
    @Environment(AppStore.self) private var store
    @State private var baseURL: String = BrandConfig.defaultNASBaseURL
    @State private var libraryPath: String = BrandConfig.defaultNASLibraryPath
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var status: String = ""
    @State private var isBusy = false
    @FocusState private var focusField: Field?

    enum Field: Hashable {
        case base, path, user, pass
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            HStack(alignment: .top, spacing: 40) {
                formColumn
                helpColumn
            }
            .padding(48)
            .frame(width: 1400, height: 780)
            .background(AppTheme.backgroundElevated, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .onAppear(perform: load)
        .onExitCommand { store.isSettingsPresented = false }
    }

    private var formColumn: some View {
        VStack(alignment: .leading, spacing: 22) {
            HStack {
                Text("设置")
                    .font(.system(size: 40, weight: .bold))
                Spacer()
                Button("完成") { store.isSettingsPresented = false }
                    .buttonStyle(.card)
            }

            Text("NAS 音源")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(AppTheme.textSecondary)

            labeled("Base URL") {
                TextField("https://nas.example.com", text: $baseURL)
                    .focused($focusField, equals: .base)
            }
            labeled("曲库路径") {
                TextField("/volume1/music", text: $libraryPath)
                    .focused($focusField, equals: .path)
            }
            labeled("账号（存 Keychain，不入库）") {
                TextField("用户名", text: $username)
                    .textContentType(.username)
                    .focused($focusField, equals: .user)
            }
            labeled("密码（存 Keychain，不入库）") {
                SecureField("密码", text: $password)
                    .textContentType(.password)
                    .focused($focusField, equals: .pass)
            }

            HStack(spacing: 20) {
                Button("保存") { save() }
                    .buttonStyle(.card)
                Button(isBusy ? "测试中…" : "测试连接") {
                    Task { await test() }
                }
                .buttonStyle(.card)
                .disabled(isBusy)
                Button("清除账号密码") {
                    KeychainCredentialStore.deleteAll()
                    username = ""
                    password = ""
                    status = "已清除 Keychain 中的 NAS 凭证"
                }
                .buttonStyle(.card)
            }

            if !status.isEmpty {
                Text(status)
                    .font(.system(size: 22))
                    .foregroundStyle(status.contains("失败") || status.contains("错误") || status.contains("无法") ? AppTheme.danger : Color.green.opacity(0.85))
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .foregroundStyle(AppTheme.textPrimary)
    }

    private var helpColumn: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("说明")
                .font(.system(size: 28, weight: .semibold))
            Text("默认已预填 \(BrandConfig.defaultNASBaseURL) 与 \(BrandConfig.defaultNASLibraryPath)。账号密码只保存在本机 Keychain，不会写入代码或 Git。")
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Text("支持 HTTPS 目录列表与常见音频扩展名扫描；也可在目录放置 library.json / M3U 清单。")
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Text("装到 Apple TV：无本地 Mac 时用 GitHub Actions + TestFlight，见安装文档。需要你提供 Apple Developer Team ID。")
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .frame(width: 420, alignment: .leading)
        .padding(24)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func labeled<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 18))
                .foregroundStyle(AppTheme.textSecondary)
            content()
                .font(.system(size: 26))
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private func load() {
        let config = NASConfigStore.load()
        baseURL = config.baseURLString
        libraryPath = config.libraryPath
        username = KeychainCredentialStore.read(account: .username) ?? ""
        // 不回填密码明文到 UI 亦可；为方便改密，若已有则留空提示
        password = ""
        if KeychainCredentialStore.read(account: .password) != nil {
            status = "已保存过密码（留空保存则保留原密码）"
        }
    }

    private func save() {
        var config = NASConfig.default
        config.baseURLString = baseURL
        config.libraryPath = libraryPath
        config.protocolKind = .https
        NASConfigStore.save(config)
        store.nas.config = NASConfigStore.load()

        do {
            let user = username.trimmingCharacters(in: .whitespacesAndNewlines)
            try KeychainCredentialStore.save(user, account: .username)
            if !password.isEmpty {
                try KeychainCredentialStore.save(password, account: .password)
            }
            status = "已保存。URL/路径在本地偏好；账号密码在 Keychain。"
            store.showToast("NAS 设置已保存")
        } catch {
            status = "保存失败：\(error.localizedDescription)"
        }
    }

    private func test() async {
        save()
        isBusy = true
        defer { isBusy = false }
        do {
            let message = try await store.nas.testConnection()
            status = message
            store.showToast(message)
        } catch {
            status = "连接失败：\(error.localizedDescription)"
        }
    }
}
