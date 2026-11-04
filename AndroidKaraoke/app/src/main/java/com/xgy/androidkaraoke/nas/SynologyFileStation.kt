package com.xgy.androidkaraoke.nas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Synology DSM File Station WebAPI。
 *
 * Base URL 形如 `https://nas.xgy1221.top:9527`（DSM 入口）。
 * **folder_path 必须用共享路径**，如 `/music`，不是 Linux 绝对路径 `/volume1/music`。
 */
class SynologyFileStation(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
) {
    @Volatile
    private var sid: String? = null

    /** real_path(`/volume1/music`) → share path(`/music`) */
    private val realPathToShare = mutableMapOf<String, String>()

    suspend fun ensureSession(): String = withContext(Dispatchers.IO) {
        sid?.let { return@withContext it }
        login()
    }

    suspend fun login(): String = withContext(Dispatchers.IO) {
        if (username.isBlank()) throw NasError.Unauthorized
        val params = linkedMapOf(
            "api" to "SYNO.API.Auth",
            "version" to "6",
            "method" to "login",
            "account" to username,
            "passwd" to password,
            "session" to "FileStation",
            "format" to "sid",
        )
        val json = postForm("$base/webapi/entry.cgi", params)
        if (!json.optBoolean("success")) {
            val code = json.optJSONObject("error")?.optInt("code") ?: -1
            throw when (code) {
                400, 401, 407, 408, 409, 410 -> NasError.Unauthorized
                else -> NasError.Server(code, "DSM 登录失败（错误码 $code）")
            }
        }
        val newSid = json.optJSONObject("data")?.optString("sid").orEmpty()
        if (newSid.isBlank()) throw NasError.Unauthorized
        sid = newSid
        newSid
    }

    fun invalidateSession() {
        sid = null
    }

    /**
     * 列出共享文件夹（连接成功后的第一步）。
     * 返回 path 形如 `/music`，并缓存 real_path 映射。
     */
    suspend fun listShares(): List<NasEntry> = withContext(Dispatchers.IO) {
        val session = ensureSession()
        val params = linkedMapOf(
            "api" to "SYNO.FileStation.List",
            "version" to "2",
            "method" to "list_share",
            "additional" to "[\"real_path\",\"owner\",\"time\",\"perm\"]",
            "_sid" to session,
        )
        val json = requestWithRetry(params)
        if (!json.optBoolean("success")) {
            throw mapListError(json, context = "列出共享文件夹")
        }
        val shares = json.optJSONObject("data")?.optJSONArray("shares")
            ?: throw NasError.EmptyListing
        if (shares.length() == 0) throw NasError.EmptyListing
        realPathToShare.clear()
        buildList {
            for (i in 0 until shares.length()) {
                val s = shares.getJSONObject(i)
                val name = s.optString("name")
                if (name.isBlank()) continue
                val sharePath = normalizeFsPath(s.optString("path").ifBlank { "/$name" })
                val real = s.optJSONObject("additional")?.optString("real_path").orEmpty()
                if (real.isNotBlank()) {
                    realPathToShare[normalizeFsPath(real)] = sharePath
                }
                // 也登记裸卷路径前缀映射：/volume1/music → /music
                add(
                    NasEntry(
                        id = sharePath,
                        name = name,
                        path = sharePath,
                        kind = NasEntryKind.Folder,
                        absoluteUrl = null,
                    ),
                )
            }
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun list(folderPath: String): List<NasEntry> = withContext(Dispatchers.IO) {
        val mapped = toSharePath(folderPath)
        // 根 / 或空：列共享，不要对 /volume1 调 list
        if (mapped == "/" || mapped.isBlank()) {
            return@withContext listShares()
        }
        val session = ensureSession()
        val params = linkedMapOf(
            "api" to "SYNO.FileStation.List",
            "version" to "2",
            "method" to "list",
            "folder_path" to mapped,
            "additional" to "[\"type\",\"size\",\"time\",\"real_path\"]",
            "_sid" to session,
        )
        val json = requestWithRetry(params)
        if (!json.optBoolean("success")) {
            // 若仍是 volume 路径失败，尝试再映射一次后报真实错误码
            throw mapListError(json, context = "列出 $mapped")
        }
        val files = json.optJSONObject("data")?.optJSONArray("files")
            ?: throw NasError.EmptyListing
        if (files.length() == 0) throw NasError.EmptyListing
        buildList {
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name")
                if (name == "#recycle" || name.startsWith(".")) continue
                val fp = normalizeFsPath(f.optString("path").ifBlank { "$mapped/$name" })
                val isDir = f.optBoolean("isdir")
                val kind = when {
                    isDir -> NasEntryKind.Folder
                    else -> classifyFile(name)
                }
                add(
                    NasEntry(
                        id = fp,
                        name = name,
                        path = fp,
                        kind = kind,
                        absoluteUrl = if (isDir) null else downloadUrl(fp),
                    ),
                )
            }
        }
    }

    /**
     * 把 `/volume1/music` 或 `/volume2/foo/bar` 映射为共享路径 `/music`、`/foo/bar`。
     * 已是 `/music` 则原样返回。
     */
    fun toSharePath(raw: String): String {
        val path = normalizeFsPath(raw)
        if (path == "/") return "/"
        // 裸卷路径 /volume1 → 共享列表根
        if (BARE_VOLUME.matches(path)) return "/"
        realPathToShare[path]?.let { return it }
        // /volume1/music/subdir → 找最长 real_path 前缀
        realPathToShare.entries
            .filter { path == it.key || path.startsWith(it.key + "/") }
            .maxByOrNull { it.key.length }
            ?.let { (real, share) ->
                val rest = path.removePrefix(real)
                return normalizeFsPath(share + rest)
            }
        // 无缓存时：剥离 /volumeN 前缀（群晖惯例）/volume1/music → /music
        val stripped = VOLUME_PREFIX.matchEntire(path)?.groupValues?.getOrNull(1)
        if (stripped != null) {
            return normalizeFsPath(stripped)
        }
        return path
    }

    fun downloadUrl(filePath: String): String {
        val session = sid.orEmpty()
        val sharePath = toSharePath(filePath)
        val enc = URLEncoder.encode(sharePath, "UTF-8")
        return "$base/webapi/entry.cgi?api=SYNO.FileStation.Download" +
            "&version=2&method=download&mode=open&path=$enc&_sid=$session"
    }

    /** 当前 SID（可能为空）；播放前应先 ensureSession。 */
    fun currentSid(): String? = sid

    companion object {
        private val VOLUME_PREFIX = Regex("^/volume\\d+(/.*)$")
        private val BARE_VOLUME = Regex("^/volume\\d+$")

        /**
         * 从 File Station Download URL 取出 `path=`（已 URL 解码的共享路径）。
         * 非 Download URL 返回 null。
         */
        fun extractDownloadPath(url: String): String? {
            if (!url.contains("SYNO.FileStation.Download", ignoreCase = true)) return null
            val query = url.substringAfter('?', missingDelimiterValue = "")
            if (query.isEmpty()) return null
            for (part in query.split('&')) {
                val eq = part.indexOf('=')
                if (eq <= 0) continue
                val key = part.substring(0, eq)
                if (key != "path") continue
                val raw = part.substring(eq + 1)
                return try {
                    java.net.URLDecoder.decode(raw, "UTF-8")
                } catch (_: Exception) {
                    raw
                }
            }
            return null
        }

        fun isFileStationDownloadUrl(url: String): Boolean =
            url.contains("SYNO.FileStation.Download", ignoreCase = true)

        suspend fun isSynologyDsm(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
            try {
                val base = baseUrl.trim().trimEnd('/')
                val url = "$base/webapi/query.cgi?api=SYNO.API.Info&version=1&method=query&query=SYNO.FileStation.List"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    instanceFollowRedirects = true
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) return@withContext false
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    body.contains("SYNO.FileStation.List") && body.contains("\"success\":true")
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                false
            }
        }

        /** 保存前规范化：volume 绝对路径 → 共享路径。 */
        fun normalizeLibraryPathForSave(raw: String): String {
            val path = normalizeFsPath(raw)
            val stripped = VOLUME_PREFIX.matchEntire(path)?.groupValues?.getOrNull(1)
            return if (stripped != null) normalizeFsPath(stripped) else path
        }

        fun normalizeFsPath(path: String): String {
            var p = path.trim().ifEmpty { "/" }
            if (!p.startsWith("/")) p = "/$p"
            while (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
            return p
        }

        fun classifyFile(name: String): NasEntryKind {
            val lower = name.lowercase()
            if (lower.endsWith(".json") || lower.endsWith(".m3u") || lower.endsWith(".m3u8")) {
                return NasEntryKind.Manifest
            }
            val ext = lower.substringAfterLast('.', "")
            return when (ext) {
                "mp4", "m4v", "mov", "mkv", "webm" -> NasEntryKind.Video
                "mp3", "m4a", "aac", "flac", "wav", "aiff", "alac", "ogg" -> NasEntryKind.Audio
                "lrc" -> NasEntryKind.Lyrics
                else -> NasEntryKind.Other
            }
        }
    }

    private val base get() = baseUrl.trim().trimEnd('/')

    private suspend fun requestWithRetry(params: MutableMap<String, String>): JSONObject {
        var json = getJson("$base/webapi/entry.cgi", params)
        if (isSessionExpired(json)) {
            invalidateSession()
            params["_sid"] = login()
            json = getJson("$base/webapi/entry.cgi", params)
        }
        return json
    }

    private fun mapListError(json: JSONObject, context: String): NasError {
        val code = json.optJSONObject("error")?.optInt("code") ?: -1
        return when (code) {
            408, 119 -> {
                invalidateSession()
                NasError.Unauthorized
            }
            400, 401 -> NasError.Unauthorized
            402, 403 -> NasError.Forbidden
            404, 414, 418 -> NasError.Server(
                code,
                "$context 失败（错误码 $code）：路径不是共享路径。请选共享如 /music，不要用 /volume1/…",
            )
            else -> NasError.Server(code, "$context 失败（File Station 错误码 $code）")
        }
    }

    private fun isSessionExpired(json: JSONObject): Boolean {
        if (json.optBoolean("success")) return false
        val code = json.optJSONObject("error")?.optInt("code") ?: return false
        return code == 119 || code == 106 || code == 107
    }

    private fun getJson(url: String, params: Map<String, String>): JSONObject {
        val q = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val conn = (URL("$url?$q").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        return readJson(conn)
    }

    private fun postForm(url: String, params: Map<String, String>): JSONObject {
        val body = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        DataOutputStream(conn.outputStream).use { it.write(bytes) }
        return readJson(conn)
    }

    private fun readJson(conn: HttpURLConnection): JSONObject {
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code == 401 || code == 403) throw NasError.Unauthorized
            return try {
                JSONObject(text)
            } catch (_: Exception) {
                throw NasError.Transport("DSM 返回非 JSON（HTTP $code）")
            }
        } catch (e: NasError) {
            throw e
        } catch (e: Exception) {
            throw NasError.Transport(e.message ?: e.javaClass.simpleName)
        } finally {
            conn.disconnect()
        }
    }
}
