package com.xgy.androidkaraoke.nas

import com.xgy.androidkaraoke.library.MediaFile
import com.xgy.androidkaraoke.model.LibrarySource
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayDeque
import java.util.Base64

class LibraryScanException(message: String) : Exception(message)

/**
 * 优先走群晖 File Station。读不到时再尝试网页目录列表。
 * /volume1/music 是磁盘路径，不是网页路径。
 */
class NasLibraryClient {
    fun collect(
        library: LibrarySource,
        password: String,
        onProgress: (String) -> Unit,
    ): List<MediaFile> {
        val errors = mutableListOf<String>()
        try {
            val files = synology(library, password, onProgress)
            if (files.isNotEmpty()) return files
            errors += "群晖这个目录里没有音视频"
        } catch (e: Exception) {
            errors += e.message ?: "群晖接口失败"
        }
        try {
            val files = htmlWalk(library, password, onProgress)
            if (files.isNotEmpty()) return files
            errors += "网页目录里没有音视频"
        } catch (e: Exception) {
            errors += e.message ?: "网页目录失败"
        }
        throw LibraryScanException(errors.distinct().joinToString("；"))
    }

    private fun synology(
        library: LibrarySource,
        password: String,
        onProgress: (String) -> Unit,
    ): List<MediaFile> {
        if (library.username.isBlank()) {
            throw LibraryScanException("请在设置里填写 NAS 账号。点歌和手机扫码不用登录，这只是用来读曲库")
        }
        val base = library.normalizedBaseUrl
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            throw LibraryScanException("曲库地址要以 http:// 或 https:// 开头")
        }
        val sid = login(base, library.username, password)
        val root = library.normalizedPath
        val pending = ArrayDeque<String>()
        pending.add(root)
        val found = mutableListOf<MediaFile>()
        var folders = 0
        while (pending.isNotEmpty() && found.size < MAX_FILES && folders < MAX_FOLDERS) {
            val folder = pending.removeFirst()
            folders++
            onProgress("正在扫描 $folder（已找到 ${found.size} 个文件）")
            val files = listFolder(base, sid, folder)
            for (i in 0 until files.length()) {
                val item = files.getJSONObject(i)
                val name = item.optString("name")
                if (name.isBlank() || name.startsWith(".") || name.startsWith("@") || name.startsWith("#")) continue
                val path = item.optString("path").ifBlank { "$folder/$name" }
                if (item.optBoolean("isdir")) {
                    pending.add(path)
                } else if (SongGrouperBridge.isMedia(name)) {
                    val relative = path.removePrefix(root).trim('/')
                    found += MediaFile(relative.ifBlank { name }, downloadUrl(base, sid, path))
                }
            }
        }
        return found
    }

    private fun login(base: String, user: String, pass: String): String {
        var last = "无法连接 NAS"
        for (version in listOf(6, 3)) {
            val body = form(
                "api" to "SYNO.API.Auth",
                "version" to version.toString(),
                "method" to "login",
                "account" to user,
                "passwd" to pass,
                "session" to "FileStation",
                "format" to "sid",
            )
            val text = post("${webApi(base)}/auth.cgi", body)
            val json = runCatching { JSONObject(text) }.getOrNull()
                ?: throw LibraryScanException("NAS 没有返回群晖接口，请确认地址是群晖的 https 入口")
            if (json.optBoolean("success")) {
                return json.getJSONObject("data").getString("sid")
            }
            val code = json.optJSONObject("error")?.optInt("code") ?: 0
            last = authMessage(code)
        }
        throw LibraryScanException(last)
    }

    private fun listFolder(base: String, sid: String, folder: String): org.json.JSONArray {
        val url = "${webApi(base)}/entry.cgi?api=SYNO.FileStation.List&version=2&method=list" +
            "&folder_path=${enc(folder)}&limit=1000&_sid=${enc(sid)}"
        val json = JSONObject(get(url))
        if (!json.optBoolean("success")) {
            val code = json.optJSONObject("error")?.optInt("code") ?: 0
            val hint = if (code == 408 || code == 418) "找不到目录 $folder" else "读取目录失败（$code）"
            throw LibraryScanException(hint)
        }
        return json.getJSONObject("data").getJSONArray("files")
    }

    private fun downloadUrl(base: String, sid: String, path: String): String {
        return "${webApi(base)}/entry.cgi?api=SYNO.FileStation.Download&version=2&method=download" +
            "&path=${enc(path)}&mode=open&_sid=${enc(sid)}"
    }

    private fun htmlWalk(
        library: LibrarySource,
        password: String,
        onProgress: (String) -> Unit,
    ): List<MediaFile> {
        val base = library.normalizedBaseUrl
        val root = library.normalizedPath
        val start = joinUrl(base, root)
        val pending = ArrayDeque<Pair<String, String>>()
        pending.add(start to "")
        val found = mutableListOf<MediaFile>()
        val seen = mutableSetOf<String>()
        var pages = 0
        while (pending.isNotEmpty() && found.size < MAX_FILES && pages < MAX_FOLDERS) {
            val (pageUrl, relativeDir) = pending.removeFirst()
            if (!seen.add(pageUrl)) continue
            pages++
            onProgress("正在读取目录 $relativeDir")
            val html = get(pageUrl, library.username, password)
            val hrefs = HREF.findAll(html).map { it.groupValues[1] }.toList()
            if (hrefs.isEmpty() && pages == 1 && !html.trimStart().startsWith("{")) {
                throw LibraryScanException("这个地址不是曲库目录")
            }
            for (href in hrefs) {
                if (href.startsWith("#") || href.startsWith("javascript:", true)) continue
                if (href == "../" || href == "./" || href == "/") continue
                val absolute = try {
                    URL(URL(pageUrl), href).toString()
                } catch (_: Exception) {
                    continue
                }
                val name = absolute.substringAfterLast('/').substringBefore('?').trim('/')
                if (name.isBlank() || name == ".." || name.startsWith(".")) continue
                val childRel = if (relativeDir.isBlank()) name else "$relativeDir/$name"
                if (href.endsWith("/")) {
                    pending.add(absolute to childRel)
                } else if (SongGrouperBridge.isMedia(name)) {
                    found += MediaFile(childRel, absolute)
                }
            }
        }
        return found
    }

    private fun webApi(base: String): String {
        val trimmed = base.trimEnd('/')
        return if (trimmed.endsWith("/webapi")) trimmed else "$trimmed/webapi"
    }

    private fun joinUrl(base: String, path: String): String {
        val rel = path.trim('/')
        val root = base.trimEnd('/')
        return if (rel.isEmpty()) "$root/" else "$root/$rel/"
    }

    private fun authMessage(code: Int) = when (code) {
        400 -> "NAS 账号或密码不正确"
        401 -> "这个 NAS 账号已停用"
        406, 407 -> "群晖开了两步验证。请单独建一个没开二次验证的只读账号给点歌用"
        else -> "群晖登录失败（$code）"
    }

    private fun get(url: String, user: String = "", pass: String = ""): String = exchange(url, "GET", null, user, pass)

    private fun post(url: String, body: String): String = exchange(url, "POST", body, "", "")

    private fun exchange(url: String, method: String, body: String?, user: String, pass: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 40_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json,text/html,*/*")
            HomeNasTls.apply(this)
            if (user.isNotEmpty() || pass.isNotEmpty()) {
                val token = Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
                setRequestProperty("Authorization", "Basic $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() } }.orEmpty()
            if (code !in 200..299 && !text.trimStart().startsWith("{")) {
                throw LibraryScanException("NAS 返回 $code")
            }
            return text
        } catch (e: LibraryScanException) {
            throw e
        } catch (_: SocketTimeoutException) {
            throw LibraryScanException("连接 NAS 超时，请确认电视和 NAS 网络通")
        } catch (e: Exception) {
            throw LibraryScanException("无法连接 NAS：${e.message ?: e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun form(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

    companion object {
        private const val MAX_FILES = 4000
        private const val MAX_FOLDERS = 800
        private val HREF = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    }
}

internal object SongGrouperBridge {
    fun isMedia(name: String): Boolean = com.xgy.androidkaraoke.library.SongGrouper.roleOf(name) != null
}
