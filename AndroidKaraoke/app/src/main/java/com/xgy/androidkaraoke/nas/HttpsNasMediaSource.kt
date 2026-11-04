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
        "找不到该路径。若 Base URL 是群晖 DSM（如 :9527），请用 File Station 路径如 /volume1/music，并填写账号密码；直链 HTTP 目录请改用 WebDAV/文件服务地址。",
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

    /** 仅验证 URL/认证可达；目录空也算成功（用于「先连接再选目录」）。 */
    suspend fun probeConnection(atPath: String = "/"): String = withContext(Dispatchers.IO) {
        ensureBackend()
        if (synologyMode.get() == true) {
            fs().ensureSession()
            // 再尝试列一下目标路径（空也算通）
            try {
                listDirectory(atPath)
            } catch (_: NasError.EmptyListing) {
            } catch (_: NasError.NotFound) {
                // 根可能不可列，登录成功即可
            }
            return@withContext "已连接群晖 File Station"
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

    suspend fun listFolders(atPath: String): List<NasEntry> = try {
        listDirectory(atPath).filter { it.kind == NasEntryKind.Folder }
    } catch (_: NasError.EmptyListing) {
        emptyList()
    }

    suspend fun listDirectory(atPath: String): List<NasEntry> = withContext(Dispatchers.IO) {
        ensureBackend()
        if (synologyMode.get() == true) {
            return@withContext fs().list(atPath).sortedWith(
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
            return@withContext fs().list(atPath).sortedWith(
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
                // File Station：尝试下载 library.json / songs.json
                val dir = SynologyFileStation.normalizeFsPath(atPath)
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
        val baseName = entry.name.substringBeforeLast('.')
        val parts = baseName.split('-', limit = 2).map { it.trim() }
        val title = parts.firstOrNull().orEmpty().ifEmpty { baseName }
        val artist = if (parts.size > 1) parts[1] else "未知歌手"
        val url = entry.absoluteUrl
        val lower = entry.name.lowercase()
        val isMv = entry.kind == NasEntryKind.Video || lower.contains("mv")
        val isOriginal = lower.contains("original") || lower.contains("原唱")
        val isAcc = lower.contains("accompaniment") || lower.contains("伴唱") || lower.contains("inst")
        return Song(
            id = entry.id,
            title = title,
            artist = artist,
            source = MediaSource.Nas,
            relativePath = entry.path,
            mvUrl = if (isMv || (!isOriginal && !isAcc && entry.kind == NasEntryKind.Video)) url else null,
            originalUrl = if (isOriginal) url else if (!isMv && !isAcc) url else null,
            accompanimentUrl = if (isAcc) url else null,
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
                    (e.kind == NasEntryKind.Video && mv == null) -> mv = url
                lower.contains("original") || lower.contains("原唱") || lower.startsWith("voc") ->
                    original = url
                lower.contains("accompaniment") || lower.contains("伴唱") ||
                    lower.contains("inst") || lower.startsWith("acc") -> acc = url
            }
        }
        if (mv == null && original == null && acc == null) return null
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
