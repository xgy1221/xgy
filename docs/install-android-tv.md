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

1. 打开应用（**无需登录**）；首次会请求存储权限（用于读 U 盘）。
2. 顶栏 **设置 → 添加曲库**：
   - **添加曲库（NAS）**：填 URL（预填 `https://nas.xgy1221.top:9527`）与账号密码 → **连接并选择目录** → 浏览文件夹 → **选为曲库根目录**。
   - **扫描 U 盘**：插入后扫描；可编辑子路径 → **浏览**。
   - 多来源可启用/禁用；U 盘拔出有提示。
3. 顶栏 **搜索**：歌名 / 歌星 / 首拼，点歌入队。
4. **已点**：删除 / 置顶 / 清空；正在播高亮。
5. 曲库统一 **MV + 原唱 + 伴唱**；顶栏 **扫码点歌** 手机同 Wi‑Fi 即可点。
6. 分类仅 **随心唱 / 歌星 / 热门**（无「我的」、无儿童/排行榜/玩法）。
