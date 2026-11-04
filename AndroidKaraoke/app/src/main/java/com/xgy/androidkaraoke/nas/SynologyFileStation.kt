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
 * Base URL 形如 `https://nas.xgy1221.top:9527`（DSM 入口，不是目录直链）。
 * 曲库路径为 File Station 路径，如 `/volume1/music`（不是 HTTP path 拼在 URL 后）。
 */
class SynologyFileStation(
    private val baseUrl: String,
    private val username: String,
    private val password: String,
) {
    @Volatile
    private var sid: String? = null

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
                else -> NasError.Server(code, "DSM 登录失败（$code）")
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

    suspend fun list(folderPath: String): List<NasEntry> = withContext(Dispatchers.IO) {
        val path = normalizeFsPath(folderPath)
        val session = ensureSession()
        val params = linkedMapOf(
            "api" to "SYNO.FileStation.List",
            "version" to "2",
            "method" to "list",
            "folder_path" to path,
            "additional" to "[\"type\",\"size\",\"time\"]",
            "_sid" to session,
        )
        var json = getJson("$base/webapi/entry.cgi", params)
        if (isSessionExpired(json)) {
            invalidateSession()
            params["_sid"] = login()
            json = getJson("$base/webapi/entry.cgi", params)
        }
        if (!json.optBoolean("success")) {
            val code = json.optJSONObject("error")?.optInt("code") ?: -1
            throw when (code) {
                408, 119 -> {
                    invalidateSession()
                    NasError.Unauthorized
                }
                400, 401 -> NasError.Unauthorized
                402, 403 -> NasError.Forbidden
                404, 414, 418 -> NasError.NotFound
                else -> NasError.Server(code, "无法列出目录（File Station $code）")
            }
        }
        val files = json.optJSONObject("data")?.optJSONArray("files")
            ?: throw NasError.EmptyListing
        if (files.length() == 0) throw NasError.EmptyListing
        buildList {
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name")
                val fp = f.optString("path").ifBlank { "$path/$name" }
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

    fun downloadUrl(filePath: String): String {
        val session = sid.orEmpty()
        val enc = URLEncoder.encode(normalizeFsPath(filePath), "UTF-8")
        return "$base/webapi/entry.cgi?api=SYNO.FileStation.Download" +
            "&version=2&method=download&mode=open&path=$enc&_sid=$session"
    }

    companion object {
        /** 探测该 Base URL 是否为 Synology DSM（公开，无需账号）。 */
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

        fun normalizeFsPath(path: String): String {
            var p = path.trim().ifEmpty { "/" }
            if (!p.startsWith("/")) p = "/$p"
            while (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
            return p
        }

        private fun classifyFile(name: String): NasEntryKind {
            val lower = name.lowercase()
            if (lower.endsWith(".json") || lower.endsWith(".m3u") || lower.endsWith(".m3u8")) {
                return NasEntryKind.Manifest
            }
            val ext = lower.substringAfterLast('.', "")
            return when (ext) {
                "mp4", "m4v", "mov", "mkv", "webm" -> NasEntryKind.Video
                "mp3", "m4a", "aac", "flac", "wav", "aiff", "alac", "ogg" -> NasEntryKind.Audio
                else -> NasEntryKind.Other
            }
        }
    }

    private val base get() = baseUrl.trim().trimEnd('/')

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
