# 雷石K歌·专业版（tvOS 壳）

原生 **SwiftUI tvOS** 点歌 App 骨架：视觉跟参考截图，交互做了已点队列 / 播放控件分组 / NAS·麦克风模块预留等优化。

## 在 Xcode 打开

1. macOS 安装 Xcode 16+（tvOS 17 SDK）
2. 打开 `LeiShiKaraoke.xcodeproj`
3. 选 Apple TV 模拟器或真机 → Run
4. Archive：Product → Archive（先填 Signing Team；`PRODUCT_BUNDLE_IDENTIFIER` 默认 `com.example.leishikaraoke`）

云端 Linux **无法**编译本工程。

## 品牌改名

编辑 `LeiShiKaraoke/Brand/BrandConfig.swift`。

## 模块边界

| 模块 | 路径 | 状态 |
|------|------|------|
| UI 壳 | `Views/` | 已实现 mock 屏 |
| 曲库协议 | `Services/CatalogProviding.swift` | Mock |
| NAS | `NAS/NASMediaSource.swift` | Stub（HTTP/WebDAV 优先） |
| 麦克风 | `Audio/MicrophoneInput.swift` | Stub + 伴侣/系统预留 |
| 播放 | `Services/AppStore.swift` | 状态机，未接 AVPlayer |

## 参考方案

项目 Agent Store：`docs/ui-plan.md`、`docs/project-context.md`
