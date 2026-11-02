# 安装到索尼 Android TV（KD-65A9F）

目标机：**Sony KD-65A9F** · **Android 10（API 29）** · Android TV。  
产物：`AndroidKaraoke/app/build/outputs/apk/debug/app-debug.apk`  
可下载副本：`dist/androidkaraoke-debug.apk`

## 构建

```bash
cd AndroidKaraoke
./gradlew :app:assembleDebug
```

## 侧载前

1. 开启开发者选项与 **USB 调试**（adb 时）。
2. 允许安装未知应用。

## U 盘 / 浏览器

拷入 APK 后用文件管理器或浏览器下载安装。

## adb（家庭局域网）

```bash
adb connect 192.168.50.28:5555
adb install -r app-debug.apk
```

云端到不了该内网地址，需在同一局域网的电脑执行。

## 首次使用

1. 打开应用（**无需登录**）。
2. **设置**：确认 NAS 预填 `https://nas.xgy1221.top` 与 `/volume1/music`，按需填账号密码 → 测试连接 → **浏览曲库**。
3. 顶栏 **扫码点歌**：手机与电视同一 Wi‑Fi，扫码打开页面即可点歌。
4. 播放中可切换 **原唱 / 伴唱**；有 MV 则播视频。
