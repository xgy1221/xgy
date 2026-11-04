# AndroidKaraoke

面向 **Sony KD-65A9F（Android 10 / API 29）** 的苹果风 Android TV 点歌 App。

- `minSdk 28` / Compose for TV `tv-material:1.0.0` · **0.4.0**
- **无强制登录**；分类仅随心唱/歌星/热门；无顶栏「我的」
- **设置 → 添加曲库**：NAS 先连接再选目录；U 盘可扫描；弹窗防点穿
- 强搜索（歌名/歌星/首拼）、已点删/置顶/清空、无曲库引导
- 曲目三件套：MV + **原唱（完整混音）** + **伴唱（纯伴奏）**；Demucs 人声干声不是原唱；扫码点歌 `:8765`
- NAS 预填 `https://nas.xgy1221.top:9527`

## 构建

```bash
./gradlew :app:assembleDebug
```

可下载副本见仓库根 `dist/androidkaraoke-debug.apk`。安装说明：`docs/install-android-tv.md`。
