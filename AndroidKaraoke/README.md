# AndroidKaraoke

面向 **Sony KD-65A9F（Android 10 / API 29）** 的苹果风 Android TV 点歌壳。

- Kotlin + Jetpack Compose + `androidx.tv:tv-material:1.0.0`
- `minSdk 28` / `targetSdk 34`
- NAS 预填 `https://nas.xgy1221.top` + `/volume1/music`；账号密码仅 EncryptedSharedPreferences
- 麦克风为 stub（A9F 蓝牙麦需实机测）

## 构建

```bash
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

安装说明见仓库 [`docs/install-android-tv.md`](../docs/install-android-tv.md)。

## 说明

旧 tvOS 目录 `LeiShiKaraoke/`（若存在）仅作参考，本目录为当前主交付。
