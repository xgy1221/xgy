package com.xgy.androidkaraoke.nas

import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

sealed class NasError(message: String) : Exception(message) {
    data object NotConfigured : NasError("请先在设置中填写 NAS 地址")
    data object InvalidUrl : NasError("NAS 地址无效，请检查 Base URL 与路径")
    data object Unauthorized : NasError("认证失败：请在设置中填写正确的账号和密码")
    data object Forbidden : NasError("没有权限访问该目录")
    data object NotFound : NasError(
        "找不到该路径。群晖请用共享路径如 /music（不要用 /volume1/music）；并确认账号密码正确。",
    )
    data object Timeout : NasError("连接超时，请检查网络或 NAS 是否在线")
    class Server(code: Int, msg: String) : NasError("服务器错误（$code）：$msg")
    class Transport(msg: String) : NasError("无法连接 NAS：$msg")
    data object EmptyListing : NasError("目录为空，或服务器未开启目录列表")
    data object UnsupportedContent : NasError("无法解析目录页（可尝试放置 library.json 清单）")
    data object DsmNeedsCredentials : NasError(
        "当前地址是群晖 DSM 入口。请填写 NAS 账号密码后重试；曲库路径填 File Station 路径（如 /volume1/music），不要当成网页路径硬拼。",
    )
}

enum class NasEntryKind { Folder, Audio, Video, Manifest, Other }

data class NasEntry(
    val id: String,
    val name: String,
    val path: String,
    val kind: NasEntryKind,
    val absoluteUrl: String? = null,
)

class HttpsNasMediaSource(
    private val configStore: NasConfigStore,
    private val credentials: SecureCredentialStore,
) {
    @Volatile
    var config: NasConfig = configStore.load()

    private val synologyMode = AtomicReference<Boolean?>(null)
    private var fileStation: SynologyFileStation? = null

    fun reloadConfig() {
        config = configStore.load()
        synologyMode.set(null)
        fileStation = null
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        val entries = listDirectory(config.normalizedPath)
        "已连接，共 ${entries.size} 项"
    }

    /** 仅验证登录可达（群晖：登录 + 列共享）。 */
    suspend fun probeConnection(atPath: String = "/"): String = withContext(Dispatchers.IO) {
        ensureBackend()
        if (synologyMode.get() == true) {
            fs().ensureSession()
            val shares = try {
                fs().listShares()
            } catch (_: NasError.EmptyListing) {
                emptyList()
            }
            return@withContext if (shares.isEmpty()) {
                "已连接群晖 File Station（暂无共享文件夹）"
            } else {
                "已连接群晖 · ${shares.size} 个共享"
            }
        }
        val url = makeHttpUrl(atPath, directory = true)
        try {
            fetchText(url)
            "连接成功"
        } catch (_: NasError.EmptyListing) {
            "连接成功"
        } catch (_: NasError.UnsupportedContent) {
            "连接成功"
        }
    }

    /** 目录选择器用：群晖根下列共享；其余列子文件夹。 */
    suspend fun listFolders(atPath: String): List<NasEntry> {
        ensureBackend()
        if (synologyMode.get() == true) {
            val mapped = fs().toSharePath(atPath)
            return if (mapped == "/" || mapped.isBlank()) {
                fs().listShares()
            } else {
                try {
                    fs().list(mapped).filter { it.kind == NasEntryKind.Folder }
                } catch (_: NasError.EmptyListing) {
                    emptyList()
                }
            }
        }
        return try {
            listDirectory(atPath).filter { it.kind == NasEntryKind.Folder }
        } catch (_: NasError.EmptyListing) {
            emptyList()
        }
    }

    suspend fun listDirectory(atPath: String): List<NasEntry> = withContext(Dispatchers.IO) {
        ensureBackend()
        if (synologyMode.get() == true) {
            val mapped = fs().toSharePath(atPath)
            return@withContext fs().list(mapped).sortedWith(
                compareBy<NasEntry> { it.kind != NasEntryKind.Folder }
                    .thenBy { it.name.lowercase() },
            )
        }
        val url = makeHttpUrl(atPath, directory = true)
        val data = fetchText(url)
        // 误把 DSM 登录页当目录
        if (looksLikeDsmHtml(data)) {
            synologyMode.set(true)
            rebuildFileStation()
            val mapped = fs().toSharePath(atPath)
            return@withContext fs().list(mapped).sortedWith(
                compareBy<NasEntry> { it.kind != NasEntryKind.Folder }
                    .thenBy { it.name.lowercase() },
            )
        }
        val entries = parseListing(data, url, normalizePath(atPath))
        if (entries.isEmpty()) throw NasError.EmptyListing
        entries.sortedWith(
            compareBy<NasEntry> { it.kind != NasEntryKind.Folder }
                .thenBy { it.name.lowercase() },
        )
    }

    suspend fun listSongs(under: String): List<Song> =
        listDirectory(under).mapNotNull { entry ->
            if (entry.kind == NasEntryKind.Audio || entry.kind == NasEntryKind.Video) {
                songFrom(entry)
            } else {
                null
            }
        }

    suspend fun loadLibraryJson(atPath: String = config.normalizedPath): List<Song> =
        withContext(Dispatchers.IO) {
            ensureBackend()
            if (synologyMode.get() == true) {
                val dir = fs().toSharePath(atPath)
                for (name in listOf("library.json", "songs.json")) {
                    val filePath = "$dir/$name"
                    try {
                        val url = fs().downloadUrl(filePath)
                        val body = fetchText(url, allowRedirect = true)
                        decodeJsonManifest(body)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
                    } catch (_: Exception) {
                    }
                }
                return@withContext emptyList()
            }
            val candidates = listOf(
                "${normalizePath(atPath).trimEnd('/')}/library.json",
                "${normalizePath(atPath).trimEnd('/')}/songs.json",
            )
            for (rel in candidates) {
                try {
                    val url = makeHttpUrl(rel, directory = false)
                    val body = fetchText(url)
                    val songs = decodeJsonManifest(body)
                    if (!songs.isNullOrEmpty()) return@withContext songs
                } catch (_: Exception) {
                }
            }
            try {
                val url = makeHttpUrl(atPath, directory = true)
                val body = fetchText(url)
                decodeJsonManifest(body)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
            } catch (_: Exception) {
            }
            emptyList()
        }

    private fun songFrom(entry: NasEntry): Song {
        val parsed = KaraokeFileNames.parseTitleArtist(entry.name)
        val url = entry.absoluteUrl
        val lower = entry.name.lowercase()
        val isExplicitOriginal = lower.contains("original") || lower.contains("原唱") || lower.startsWith("voc")
        val isExplicitAcc = lower.contains("accompaniment") || lower.contains("伴唱") ||
            lower.contains("inst") || lower.startsWith("acc")
        val isExplicitMv = lower.startsWith("mv") || lower.contains(".mv.")
        // 扁平原 MP4：只挂 MV，不假装「有原唱/有伴唱」
        val flatKaraokeVideo = entry.kind == NasEntryKind.Video &&
            !isExplicitOriginal && !isExplicitAcc && !isExplicitMv && url != null
        return Song(
            id = entry.id,
            title = parsed.first,
            artist = parsed.second,
            source = MediaSource.Nas,
            relativePath = entry.path,
            mvUrl = when {
                flatKaraokeVideo || isExplicitMv || (entry.kind == NasEntryKind.Video && !isExplicitOriginal && !isExplicitAcc) -> url
                else -> null
            },
            originalUrl = when {
                isExplicitOriginal -> url
                entry.kind == NasEntryKind.Audio && !isExplicitAcc -> url
                else -> null
            },
            accompanimentUrl = when {
                isExplicitAcc -> url
                else -> null
            },
        )
    }

    suspend fun songFromFolder(folderPath: String, folderName: String): Song? {
        val entries = listDirectory(folderPath)
        var mv: String? = null
        var original: String? = null
        var acc: String? = null
        for (e in entries) {
            val lower = e.name.lowercase()
            val url = e.absoluteUrl ?: continue
            when {
                lower.startsWith("mv") || lower.contains(".mv.") ||
                    (e.kind == NasEntryKind.Video && mv == null &&
                        !lower.contains("original") && !lower.contains("原唱") &&
                        !lower.contains("accompaniment") && !lower.contains("伴唱")) -> mv = url
                lower.contains("original") || lower.contains("原唱") || lower.startsWith("voc") ->
                    original = url
                lower.contains("accompaniment") || lower.contains("伴唱") ||
                    lower.contains("inst") || lower.startsWith("acc") -> acc = url
            }
        }
        if (mv == null && original == null && acc == null) return null
        // 同一 URL 不重复挂到多轨，避免 UI 误报「有原唱/有伴唱」
        if (original != null && original == mv) original = null
        if (acc != null && (acc == mv || acc == original)) acc = null
        return Song(
            id = folderPath,
            title = folderName.trim('/'),
            artist = "NAS",
            source = MediaSource.Nas,
            relativePath = folderPath,
            mvUrl = mv,
            originalUrl = original,
            accompanimentUrl = acc,
        )
    }

    /**
     * 播放前刷新群晖 Download URL 的 `_sid`（索引时烘焙的 SID 会过期）。
     * 非群晖 / 非 NAS 原样返回。
     */
    suspend fun resolveForPlayback(song: Song): Song = withContext(Dispatchers.IO) {
        if (song.source != MediaSource.Nas) return@withContext song
        ensureBackend()
        if (synologyMode.get() != true) return@withContext song
        val station = fs()
        station.ensureSession()
        fun refresh(url: String?): String? {
            if (url.isNullOrBlank()) return null
            val path = SynologyFileStation.extractDownloadPath(url)
                ?: url.takeIf { it.startsWith("/") && !it.startsWith("http") }
            return if (path != null) station.downloadUrl(path) else url
        }
        val pathHint = song.relativePath?.takeIf {
            it.startsWith("/") && !it.startsWith("http")
        }
        // 扁平单文件：只用 relativePath 重建 MV
        if (pathHint != null &&
            song.originalUrl.isNullOrBlank() &&
            song.accompanimentUrl.isNullOrBlank()
        ) {
            return@withContext song.copy(
                mvUrl = station.downloadUrl(pathHint),
                originalUrl = null,
                accompanimentUrl = null,
            )
        }
        song.copy(
            mvUrl = refresh(song.mvUrl) ?: pathHint?.let { station.downloadUrl(it) },
            originalUrl = refresh(song.originalUrl),
            accompanimentUrl = refresh(song.accompanimentUrl),
        )
    }

    /** 强制重新登录群晖（Source error 重试前调用）。 */
    suspend fun forceRelogin() = withContext(Dispatchers.IO) {
        ensureBackend()
        if (synologyMode.get() == true) {
            fs().invalidateSession()
            fs().login()
        }
    }

    private suspend fun ensureBackend() {
        if (synologyMode.get() != null && (synologyMode.get() == false || fileStation != null)) return
        val base = config.normalizedBaseUrl
        if (base.isBlank()) throw NasError.NotConfigured
        val isSyno = SynologyFileStation.isSynologyDsm(base)
        synologyMode.set(isSyno)
        if (isSyno) {
            if (credentials.username().isBlank()) throw NasError.DsmNeedsCredentials
            rebuildFileStation()
        }
    }

    private fun rebuildFileStation() {
        fileStation = SynologyFileStation(
            baseUrl = config.normalizedBaseUrl,
            username = credentials.username(),
            password = credentials.password(),
        )
    }

    private fun fs(): SynologyFileStation =
        fileStation ?: run {
            rebuildFileStation()
            fileStation!!
        }

    private fun makeHttpUrl(path: String, directory: Boolean): String {
        val base = config.normalizedBaseUrl
        if (base.isBlank()) throw NasError.NotConfigured
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            throw NasError.InvalidUrl
        }
        val rel = normalizePath(path).trim('/')
        // 分段编码路径，避免中文目录 404
        val encoded = if (rel.isEmpty()) {
            ""
        } else {
            rel.split('/').joinToString("/") { segment ->
                java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
        }
        var url = if (encoded.isEmpty()) "$base/" else "$base/$encoded"
        if (directory && !url.endsWith("/")) url += "/"
        return url
    }

    private fun normalizePath(path: String): String {
        var p = path.trim()
        if (p.isEmpty()) return config.normalizedPath
        if (!p.startsWith("/")) p = "/$p"
        return p
    }

    private fun looksLikeDsmHtml(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("synology") ||
            lower.contains("webman/") ||
            lower.contains("syno.api") ||
            (lower.contains("<title>") && lower.contains("dsm"))
    }

    private fun fetchText(urlString: String, allowRedirect: Boolean = true): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Accept", "text/html,application/json,*/*")
            instanceFollowRedirects = allowRedirect
            basicAuthHeader()?.let { setRequestProperty("Authorization", it) }
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            when (code) {
                in 200..299 -> return body
                401 -> throw NasError.Unauthorized
                403 -> throw NasError.Forbidden
                404 -> throw NasError.NotFound
                else -> throw NasError.Server(code, connection.responseMessage ?: "错误")
            }
        } catch (e: NasError) {
            throw e
        } catch (_: SocketTimeoutException) {
            throw NasError.Timeout
        } catch (e: Exception) {
            throw NasError.Transport(e.message ?: e.javaClass.simpleName)
        } finally {
            connection.disconnect()
        }
    }

    private fun basicAuthHeader(): String? {
        val user = credentials.username()
        val pass = credentials.password()
        if (user.isEmpty() && pass.isEmpty()) return null
        val token = Base64.getEncoder().encodeToString("$user:$pass".toByteArray(Charsets.UTF_8))
        return "Basic $token"
    }

    private fun parseListing(body: String, pageUrl: String, parentPath: String): List<NasEntry> {
        decodeJsonManifest(body)?.let { songs ->
            return songs.map {
                NasEntry(
                    id = it.id,
                    name = it.title,
                    path = it.relativePath ?: parentPath,
                    kind = NasEntryKind.Audio,
                    absoluteUrl = it.anyPlayableUrl(),
                )
            }
        }

        val hrefPattern = Pattern.compile("""href\s*=\s*["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
        val matcher = hrefPattern.matcher(body)
        val seen = mutableSetOf<String>()
        val result = mutableListOf<NasEntry>()
        while (matcher.find()) {
            val href = matcher.group(1) ?: continue
            if (href.startsWith("#") || href.startsWith("?") || href.startsWith("javascript:", true)) continue
            if (href == "../" || href == "./" || href == "/") continue
            val absolute = resolveUrl(pageUrl, href) ?: continue
            val name = absolute.substringAfterLast('/').substringBefore('?')
            if (name == ".." || name.isEmpty()) continue
            if (!seen.add(absolute)) continue
            val childPath = pathFromAbsolute(absolute) ?: "$parentPath/$name"
            val kind = classify(name, href)
            result += NasEntry(
                id = absolute,
                name = name.trim('/'),
                path = childPath,
                kind = kind,
                absoluteUrl = absolute,
            )
        }
        return result
    }

    private fun resolveUrl(base: String, href: String): String? = try {
        URL(URL(base), href).toString()
    } catch (_: Exception) {
        null
    }

    private fun pathFromAbsolute(absolute: String): String? = try {
        val path = URL(absolute).path
        val basePath = URL(config.normalizedBaseUrl).path.trim('/')
        if (basePath.isNotEmpty() && path.startsWith("/$basePath")) {
            val rest = path.removePrefix("/$basePath")
            if (rest.startsWith("/")) rest else "/$rest"
        } else {
            path.ifEmpty { "/" }
        }
    } catch (_: Exception) {
        null
    }

    private fun classify(name: String, href: String): NasEntryKind {
        val lower = name.lowercase()
        if (href.endsWith("/")) return NasEntryKind.Folder
        if (lower.endsWith(".json") || lower == "library.json" || lower.endsWith(".m3u") || lower.endsWith(".m3u8")) {
            return NasEntryKind.Manifest
        }
        val ext = lower.substringAfterLast('.', missingDelimiterValue = "")
        if (ext in config.audioExtensions) {
            return if (ext in listOf("mp4", "m4v", "mov", "mkv")) NasEntryKind.Video else NasEntryKind.Audio
        }
        return NasEntryKind.Other
    }

    private fun decodeJsonManifest(body: String): List<Song>? {
        return try {
            val root = JSONObject(body)
            val arr = when {
                root.has("songs") -> root.getJSONArray("songs")
                root.has("tracks") -> root.getJSONArray("tracks")
                else -> return null
            }
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val title = item.optString("title").ifBlank { return@buildList }
                    val artist = item.optString("artist", "未知歌手")
                    val rel = item.optString("path").takeIf { it.isNotBlank() }
                    fun abs(key: String): String? {
                        val raw = item.optString(key).takeIf { it.isNotBlank() } ?: return null
                        return if (raw.startsWith("http")) raw else config.urlForRelativePath(raw)
                    }
                    val mv = abs("mv") ?: abs("mvUrl") ?: abs("video")
                    val original = abs("original") ?: abs("originalUrl") ?: abs("vocal")
                    val acc = abs("accompaniment") ?: abs("accompanimentUrl") ?: abs("instrumental")
                    val legacy = abs("url") ?: rel?.let { config.urlForRelativePath(it) }
                    add(
                        Song(
                            id = item.optString("id").ifBlank { "manifest-$i-$title" },
                            title = title,
                            artist = artist,
                            durationSec = item.optLong("duration", 240),
                            source = MediaSource.Nas,
                            relativePath = rel,
                            mvUrl = mv ?: if (legacy != null && legacy.contains(".mp4")) legacy else null,
                            originalUrl = original ?: legacy,
                            accompanimentUrl = acc,
                        ),
                    )
                }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
