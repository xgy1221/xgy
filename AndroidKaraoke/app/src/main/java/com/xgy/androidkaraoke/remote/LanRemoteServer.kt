package com.xgy.androidkaraoke.remote

import com.xgy.androidkaraoke.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

interface PhoneRemote {
    fun searchSongs(query: String, artist: String): List<Song>
    fun singers(): List<String>
    fun queueSnapshot(): List<Song>
    fun nowPlaying(): Song?
    fun order(id: String, singNow: Boolean): String
    fun songCount(): Int
}

class LanRemoteServer {
    @Volatile
    var pageUrl: String? = null
        private set

    @Volatile
    private var handler: PhoneRemote? = null

    private var serverSocket: ServerSocket? = null

    fun start(remote: PhoneRemote): String? {
        handler = remote
        if (serverSocket != null) return pageUrl
        for (port in 8765..8775) {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress("0.0.0.0", port))
                serverSocket = socket
                Thread({
                    while (!socket.isClosed) {
                        val client = try {
                            socket.accept()
                        } catch (_: Exception) {
                            break
                        }
                        Thread { handle(client) }.apply { isDaemon = true }.start()
                    }
                }, "karaoke-remote").apply { isDaemon = true }.start()
                val host = lanIpv4()
                pageUrl = host?.let { "http://$it:$port/" }
                return pageUrl
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 8_000
            val input = client.getInputStream()
            val header = ByteArrayOutputStream()
            while (header.size() < 16_384) {
                val b = input.read()
                if (b < 0) return
                header.write(b)
                val bytes = header.toByteArray()
                if (bytes.size >= 4 &&
                    bytes[bytes.size - 4] == '\r'.code.toByte() &&
                    bytes[bytes.size - 3] == '\n'.code.toByte() &&
                    bytes[bytes.size - 2] == '\r'.code.toByte() &&
                    bytes[bytes.size - 1] == '\n'.code.toByte()
                ) {
                    break
                }
            }
            val headText = header.toString(StandardCharsets.UTF_8)
            val lines = headText.split("\r\n")
            val request = lines.firstOrNull().orEmpty().split(" ")
            val method = request.getOrElse(0) { "GET" }
            val target = request.getOrElse(1) { "/" }
            val contentLength = lines.firstNotNullOfOrNull { line ->
                if (line.startsWith("Content-Length:", true)) line.substringAfter(":").trim().toIntOrNull() else null
            } ?: 0
            val bodyBytes = ByteArray(contentLength.coerceAtMost(64_000))
            var read = 0
            while (read < bodyBytes.size) {
                val n = input.read(bodyBytes, read, bodyBytes.size - read)
                if (n < 0) break
                read += n
            }
            val body = String(bodyBytes, 0, read, StandardCharsets.UTF_8)
            val response = route(method, target, body)
            client.getOutputStream().use { it.write(response) }
        }
    }

    private fun route(method: String, target: String, body: String): ByteArray {
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "")
        val remote = handler
        if (remote == null) return http(503, "text/plain", "点歌服务还没准备好".toByteArray())
        return when {
            method == "GET" && (path == "/" || path == "/index.html") -> http(200, "text/html", PHONE_PAGE.toByteArray())
            method == "GET" && path == "/api/songs" -> {
                val params = parseQuery(query)
                val songs = remote.searchSongs(params["q"].orEmpty(), params["artist"].orEmpty())
                http(200, "application/json", songsJson(songs, remote.songCount()).toByteArray())
            }
            method == "GET" && path == "/api/singers" -> {
                val array = JSONArray()
                remote.singers().forEach { array.put(it) }
                http(200, "application/json", JSONObject().put("singers", array).toString().toByteArray())
            }
            method == "GET" && path == "/api/state" -> {
                val now = remote.nowPlaying()
                val queue = JSONArray()
                remote.queueSnapshot().forEach { queue.put(songJson(it)) }
                val root = JSONObject()
                    .put("now", if (now == null) JSONObject.NULL else songJson(now))
                    .put("queue", queue)
                    .put("total", remote.songCount())
                http(200, "application/json", root.toString().toByteArray())
            }
            method == "POST" && path == "/api/order" -> {
                val json = runCatching { JSONObject(body) }.getOrNull()
                val id = json?.optString("id").orEmpty()
                val action = json?.optString("action").orEmpty()
                if (id.isBlank()) return http(400, "application/json", """{"ok":false,"message":"缺少歌曲"}""".toByteArray())
                val message = remote.order(id, action == "now")
                http(200, "application/json", JSONObject().put("ok", true).put("message", message).toString().toByteArray())
            }
            else -> http(404, "text/plain", "not found".toByteArray())
        }
    }

    private fun songsJson(songs: List<Song>, total: Int): String {
        val array = JSONArray()
        songs.take(80).forEach { array.put(songJson(it)) }
        return JSONObject().put("total", total).put("songs", array).toString()
    }

    private fun songJson(song: Song): JSONObject = JSONObject()
        .put("id", song.id)
        .put("title", song.title)
        .put("artist", song.artist)
        .put("mv", song.hasMv)
        .put("original", song.hasOriginal)
        .put("accompany", song.hasAccompaniment)

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val key = part.substringBefore('=')
            val value = URLDecoder.decode(part.substringAfter('=', ""), Charsets.UTF_8)
            if (key.isBlank()) null else key to value
        }.toMap()
    }

    private fun http(status: Int, type: String, body: ByteArray): ByteArray {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "Error"
        }
        val head = "HTTP/1.1 $status $reason\r\n" +
            "Content-Type: $type; charset=utf-8\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Cache-Control: no-store\r\n\r\n"
        return head.toByteArray(StandardCharsets.UTF_8) + body
    }

    companion object {
        fun lanIpv4(): String? {
            val addresses = NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { nic -> nic.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .filter { it != "127.0.0.1" && !it.startsWith("169.254.") }
            return addresses.firstOrNull { it.startsWith("192.168.") }
                ?: addresses.firstOrNull { it.startsWith("10.") }
                ?: addresses.firstOrNull { address ->
                    val second = address.removePrefix("172.").substringBefore('.').toIntOrNull()
                    address.startsWith("172.") && second != null && second in 16..31
                }
        }

        private val PHONE_PAGE = """
            <!doctype html>
            <html lang="zh-CN">
            <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width,initial-scale=1"/>
            <title>点歌</title>
            <style>
            body{margin:0;background:#141414;color:#fff;font-family:-apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif}
            header{position:sticky;top:0;background:#1c1c1c;padding:12px}
            input{width:100%;box-sizing:border-box;border:0;border-radius:12px;padding:14px 16px;font-size:16px;background:#2a2a2a;color:#fff}
            .tabs{display:flex;gap:8px;margin-top:10px}
            .tabs button{flex:1;border:0;border-radius:10px;padding:10px;background:#2a2a2a;color:#ddd;font-size:15px}
            .tabs button.on{background:#fff;color:#111}
            main{padding:8px 12px 84px}
            .row{display:flex;gap:8px;align-items:center;padding:12px 0;border-bottom:1px solid #2a2a2a}
            .meta{flex:1;min-width:0}
            .title{font-size:17px;font-weight:600}
            .artist{color:#aaa;font-size:13px;margin-top:2px}
            .tag{display:inline-block;margin-right:6px;color:#f3b24a;font-size:11px}
            button.now,button.add{border:0;border-radius:999px;padding:8px 12px;font-size:14px;white-space:nowrap}
            button.now{background:#e23d3d;color:#fff}
            button.add{background:#fff;color:#111}
            footer{position:fixed;left:0;right:0;bottom:0;background:#111;padding:10px 14px;color:#ddd;font-size:14px}
            .hint{color:#888;padding:18px 4px;line-height:1.5}
            </style>
            </head>
            <body>
            <header>
              <input id="q" placeholder="搜索歌曲 / 歌星" />
              <div class="tabs">
                <button id="tab-songs" class="on">点歌</button>
                <button id="tab-queue">已点</button>
              </div>
            </header>
            <main id="list"></main>
            <footer id="now">和电视连同一个 Wi-Fi，不用登录</footer>
            <script>
            let tab='songs';
            const list=document.getElementById('list');
            function row(song){
              const tags=[];
              if(song.mv) tags.push('MV');
              if(song.original) tags.push('原唱');
              if(song.accompany) tags.push('伴唱');
              return '<div class="row"><div class="meta"><div class="title">'+esc(song.title)+'</div><div class="artist">'+esc(song.artist)+'</div><div>'+tags.map(t=>'<span class="tag">'+t+'</span>').join('')+'</div></div>'
                +'<button class="now" onclick="order(\''+song.id+'\',\'now\')">立即唱</button>'
                +'<button class="add" onclick="order(\''+song.id+'\',\'queue\')">点歌</button></div>';
            }
            function esc(s){return (s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
            async function loadSongs(){
              const q=document.getElementById('q').value||'';
              const res=await fetch('/api/songs?q='+encodeURIComponent(q));
              const data=await res.json();
              if(!data.songs.length){
                list.innerHTML='<div class="hint">'+(data.total? '没有匹配的歌':'曲库还是空的。请先在电视的设置里填写 NAS 账号并扫描')+'</div>';
                return;
              }
              list.innerHTML='<div class="hint">曲库 '+data.total+' 首'+(q? '':'，输入歌名或歌手缩小范围')+'</div>'+data.songs.map(row).join('');
            }
            async function loadState(){
              const res=await fetch('/api/state');
              const data=await res.json();
              const now=data.now&&data.now.title?('正在播放：'+data.now.title+' · '+data.now.artist):'还没有在播';
              document.getElementById('now').textContent=now+'  · 已点 '+(data.queue?data.queue.length:0);
              if(tab==='queue'){
                const items=[];
                if(data.now&&data.now.title) items.push(data.now);
                (data.queue||[]).forEach(s=>items.push(s));
                list.innerHTML=items.length?items.map(row).join(''):'<div class="hint">还没有点歌</div>';
              }
            }
            async function order(id,action){
              await fetch('/api/order',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({id:id,action:action})});
              loadState();
            }
            document.getElementById('q').addEventListener('input',()=>{ if(tab==='songs') loadSongs(); });
            document.getElementById('tab-songs').onclick=()=>{tab='songs'; document.getElementById('tab-songs').className='on'; document.getElementById('tab-queue').className=''; loadSongs();};
            document.getElementById('tab-queue').onclick=()=>{tab='queue'; document.getElementById('tab-queue').className='on'; document.getElementById('tab-songs').className=''; loadState();};
            loadSongs(); loadState(); setInterval(loadState, 2000);
            </script>
            </body></html>
        """.trimIndent()
    }
}
