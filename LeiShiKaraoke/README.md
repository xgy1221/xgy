# 雷石K歌（tvOS）

Apple TV / tvOS 原生点歌壳：**视觉跟 tvOS HIG**，信息架构参考点歌场景。音源默认连你的 NAS。

## 打开工程

需要 **Xcode 的是 CI 的 macOS runner**，不是你必须买 Mac。  
本地若有 Mac：打开 `LeiShiKaraoke.xcodeproj` → Apple TV 模拟器。

## 无 Mac：装到 Apple TV

见项目文档 `docs/install-apple-tv.md`（Agent Store）：

1. 加入 Apple Developer Program  
2. **把 Team ID 发给我们** → 配置 GitHub Secrets  
3. Actions 归档 → TestFlight → Apple TV 安装  

工作流：`.github/workflows/tvos-build.yml`

## NAS

- 默认 Base URL：`https://nas.xgy1221.top`  
- 默认路径：`/volume1/music`  
- **账号密码只在 App「设置」里输入**，存 Keychain，禁止写进仓库  

## 品牌

`LeiShiKaraoke/Brand/BrandConfig.swift`
