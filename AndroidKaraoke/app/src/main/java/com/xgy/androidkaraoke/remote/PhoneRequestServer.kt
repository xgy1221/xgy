package com.xgy.androidkaraoke.remote

import android.util.Log
import com.xgy.androidkaraoke.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 电视端简易 HTTP：手机浏览器打开二维码链接即可点歌（无需微信开放平台）。
 */
class PhoneRequestServer(
    private val port: Int = DEFAULT_PORT,
    private val indexHtml: String,
    private val catalogProvider: () -> List<Song>,
    private val queueProvider: () -> List<Song>,
    private val onAddSong: (Song) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    val isRunning: Boolean get() = running.get()

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            try {
                ServerSocket(port).use { server ->
                    serverSocket = server
                    server.reuseAddress = true
                    Log.i(TAG, "Phone order server on :$port")
                    while (running.get()) {
                        try {
                            val client = server.accept()
                            executor.execute { handleClient(client) }
                        } catch (e: Exception) {
                            if (running.get()) Log.w(TAG, "accept: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "server failed: ${e.message}")
                running.set(false)
            }
        }
    }

    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    fun publicBaseUrl(): String? {
        val ip = localIpv4() ?: return null
        return "http://$ip:$port/"
    }

    private fun handleClient(socket: Socket) {
        socket.soTimeout = 15_000
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val pathQuery = parts[1]
            val path = pathQuery.substringBefore('?')
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = input.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) {
                    headers[line.substring(0, idx).trim().lowercase()] =
                        line.substring(idx + 1).trim()
                }
            }
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val n = input.read(buf, read, contentLength - read)
                    if (n < 0) break
                    read += n
                }
                String(buf, 0, read)
            } else {
                ""
            }

            when {
                method == "GET" && (path == "/" || path == "/index.html") ->
                    writeResponse(socket, 200, "text/html; charset=utf-8", indexHtml)
                method == "GET" && path == "/api/catalog" ->
                    writeResponse(socket, 200, "application/json; charset=utf-8", catalogJson())
                method == "GET" && path == "/api/queue" ->
                    writeResponse(socket, 200, "application/json; charset=utf-8", queueJson())
                method == "POST" && path == "/api/queue" -> {
                    val song = resolveSong(body, pathQuery.substringAfter('?', missingDelimiterValue = ""))
                    if (song == null) {
                        writeResponse(
                            socket,
                            400,
                            "application/json; charset=utf-8",
                            """{"ok":false,"message":"找不到歌曲"}""",
                        )
                    } else {
                        onAddSong(song)
                        writeResponse(
                            socket,
                            200,
                            "application/json; charset=utf-8",
                            """{"ok":true,"message":"已加入已点：${escapeJson(song.title)}"}""",
                        )
                    }
                }
                else -> writeResponse(socket, 404, "text/plain; charset=utf-8", "Not Found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "client: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveSong(body: String, query: String): Song? {
        val idFromQuery = query.split('&')
            .mapNotNull {
                val kv = it.split('=', limit = 2)
                if (kv.size == 2 && kv[0] == "id") URLDecoder.decode(kv[1], "UTF-8") else null
            }.firstOrNull()
        val idFromBody = try {
            if (body.isBlank()) null else JSONObject(body).optString("id").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
        val id = idFromBody ?: idFromQuery ?: return null
        return catalogProvider().find { it.id == id }
    }

    private fun catalogJson(): String {
        val arr = JSONArray()
        catalogProvider().forEach { song ->
            arr.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("hasMv", song.hasMv)
                    .put("hasOriginal", song.hasOriginal)
                    .put("hasAccompaniment", song.hasAccompaniment),
            )
        }
        return JSONObject().put("songs", arr).toString()
    }

    private fun queueJson(): String {
        val arr = JSONArray()
        queueProvider().forEach { song ->
            arr.put(JSONObject().put("id", song.id).put("title", song.title).put("artist", song.artist))
        }
        return JSONObject().put("queue", arr).toString()
    }

    private fun writeResponse(socket: Socket, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charset.forName("UTF-8"))
        val status = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "OK"
        }
        val header = buildString {
            append("HTTP/1.1 $code $status\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        val out = socket.getOutputStream()
        OutputStreamWriter(out, Charsets.US_ASCII).apply {
            write(header)
            flush()
        }
        out.write(bytes)
        out.flush()
    }

    companion object {
        const val DEFAULT_PORT = 8765
        private const val TAG = "PhoneRequestServer"

        fun localIpv4(): String? {
            return try {
                NetworkInterface.getNetworkInterfaces()?.toList()
                    ?.flatMap { it.inetAddresses.toList() }
                    ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
            } catch (_: Exception) {
                null
            }
        }

        private fun escapeJson(s: String): String =
            s.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
