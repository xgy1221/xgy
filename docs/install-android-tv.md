# 安装到索尼 Android TV（KD-65A9F）

目标机：**Sony KD-65A9F** · **Android 10（API 29）** · Android TV。  
产物：`AndroidKaraoke/app/build/outputs/apk/debug/app-debug.apk`

## 构建 APK

```bash
cd AndroidKaraoke
./gradlew :app:assembleDebug
```

## 侧载前

1. 开启开发者选项（关于电视里连续点击「版本」等项，以机内菜单为准）。
2. 开启 **USB 调试**（adb 时需要）。
3. 允许安装未知应用 / 未知来源。

## U 盘

1. APK 拷入 U 盘（建议 FAT32）。
2. 电视文件管理器打开并安装；按提示授权来源。

## adb

```bash
adb connect <电视IP>:5555   # 若使用网络调试
adb install -r app-debug.apk
```

## 浏览器

将 APK 放到可下载的 HTTPS 地址，用电视浏览器下载后安装。

## 首次使用

打开应用 → **设置** → 确认 NAS 预填 `https://nas.xgy1221.top` 与 `/volume1/music` → 输入账号密码（不会写入源码）→ **测试连接**。用方向键验证焦点与返回键。
