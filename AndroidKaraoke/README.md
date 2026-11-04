# AndroidKaraoke

面向 **Sony KD-65A9F（Android 10 / API 29）** 的苹果风 Android TV 点歌 App。

- `minSdk 28` / Compose for TV `tv-material:1.0.0` · `0.3.0`
- **无强制登录**；曲库在**设置 → 曲库来源**（无 NAS 顶栏 Tab）
- 多来源：**NAS**（可添加多个）+ **U 盘**（扫描 / 拔插提示）
- 曲目三件套：MV + 原唱 + 伴唱；播放器可切换（U 盘与 NAS 统一浏览）
- 扫码点歌：电视局域网 HTTP `:8765`，手机浏览器点歌入队
- NAS 预填 `https://nas.xgy1221.top` + `/volume1/music`

## 构建

```bash
./gradlew :app:assembleDebug
```

可下载副本见仓库根 `dist/androidkaraoke-debug.apk`。安装说明：`docs/install-android-tv.md`。
