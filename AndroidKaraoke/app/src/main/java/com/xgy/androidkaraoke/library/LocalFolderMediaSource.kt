package com.xgy.androidkaraoke.library

import com.xgy.androidkaraoke.lyrics.LyricsFiles
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.nas.KaraokeFileNames
import com.xgy.androidkaraoke.nas.NasConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URI

/**
 * 本地/U 盘目录浏览：与 NAS 相同的文件夹三轨约定 + library.json + `.lrc`。
 */
class LocalFolderMediaSource {
    private val audioExt = NasConfig.defaultAudioExtensions.toSet()
    private val videoExt = setOf("mp4", "m4v", "mov", "mkv", "webm")

    suspend fun listDirectory(rootPath: String, relativeOrAbs: String): List<LibraryEntry> =
        withContext(Dispatchers.IO) {
            val dir = resolveDir(rootPath, relativeOrAbs)
                ?: throw LocalLibraryError.NotFound("找不到目录：$relativeOrAbs")
            if (!dir.canRead()) throw LocalLibraryError.NoPermission("无法读取该目录（请检查存储权限）")
            val children = dir.listFiles() ?: emptyArray()
            if (children.isEmpty()) throw LocalLibraryError.Empty("目录为空")
            children
                .filter { !it.name.startsWith(".") }
                .map { file ->
                    val kind = classify(file)
                    LibraryEntry(
                        id = file.absolutePath,
                        name = file.name,
                        path = file.absolutePath,
                        kind = kind,
                        playableUri = if (kind == LibraryEntryKind.Audio || kind == LibraryEntryKind.Video) {
                            file.toURI().toString()
                        } else if (kind == LibraryEntryKind.Lyrics) {
                            file.toURI().toString()
                        } else {
                            null
                        },
                        sourceId = "",
                    )
                }
                .sortedWith(
                    compareBy<LibraryEntry> { it.kind != LibraryEntryKind.Folder }
                        .thenBy { it.name.lowercase() },
                )
        }

    suspend fun songFromFolder(folderPath: String, folderName: String): Song? =
        withContext(Dispatchers.IO) {
            val dir = File(folderPath)
            if (!dir.isDirectory) return@withContext null
            var mv: String? = null
            var original: String? = null
            var acc: String? = null
            val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
            files.forEach { f ->
                val lower = f.name.lowercase()
                val ext = f.extension.lowercase()
                val uri = f.toURI().toString()
                when {
                    lower.endsWith(".lrc") -> Unit
                    lower.startsWith("mv") || lower.contains(".mv.") ||
                        (ext in videoExt && mv == null &&
                            !lower.contains("original") && !lower.contains("伴唱") &&
                            !lower.contains("accompaniment")) ->
                        mv = uri
                    lower.contains("original") || lower.contains("原唱") || lower.startsWith("voc") ->
                        original = uri
                    lower.contains("accompaniment") || lower.contains("伴唱") ||
                        lower.contains("inst") || lower.startsWith("acc") ->
                        acc = uri
                }
            }
            if (mv == null && original == null && acc == null) return@withContext null
            if (original != null && original == mv) original = null
            if (acc != null && (acc == mv || acc == original)) acc = null
            val parsed = KaraokeFileNames.parseTitleArtist("$folderName.mp4")
            val prefer = LyricsFiles.preferredNames(
                title = if (parsed.second != "NAS") parsed.first else folderName,
                artist = parsed.second.takeIf { it != "NAS" },
                folderOrBase = folderName,
            )
            val lyricsName = LyricsFiles.pickLyricsName(files.map { it.name }, prefer)
            val lyricsUrl = files.firstOrNull { it.name.equals(lyricsName, ignoreCase = true) }
                ?.toURI()?.toString()
            Song(
                id = folderPath,
                title = if (parsed.second != "NAS") parsed.first else folderName.trim('/'),
                artist = if (parsed.second != "NAS") parsed.second else "U盘",
                source = MediaSource.Usb,
                relativePath = folderPath,
                mvUrl = mv,
                originalUrl = original,
                accompanimentUrl = acc,
                lyricsUrl = lyricsUrl,
            )
        }

    suspend fun loadLibraryJson(dirPath: String): List<Song> = withContext(Dispatchers.IO) {
        val candidates = listOf("library.json", "songs.json")
        for (name in candidates) {
            val f = File(dirPath, name)
            if (!f.isFile) continue
            val body = runCatching { f.readText(Charsets.UTF_8) }.getOrNull() ?: continue
            decodeJson(body, dirPath)?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
        }
        emptyList()
    }

    suspend fun listSongs(dirPath: String): List<Song> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        val children = dir.listFiles()?.filter { !it.name.startsWith(".") } ?: return@withContext emptyList()
        val songs = mutableListOf<Song>()
        val files = children.filter { it.isFile }
        for (file in files) {
            val kind = classify(file)
            if (kind !in setOf(LibraryEntryKind.Audio, LibraryEntryKind.Video)) continue
            val lower = file.name.lowercase()
            val uri = file.toURI().toString()
            val isMv = kind == LibraryEntryKind.Video || lower.contains("mv")
            val isOriginal = lower.contains("original") || lower.contains("原唱")
            val isAcc = lower.contains("accompaniment") || lower.contains("伴唱")
            val parsed = KaraokeFileNames.parseTitleArtist(file.name)
            val prefer = LyricsFiles.preferredNames(parsed.first, parsed.second, file.name)
            val lyricsName = LyricsFiles.pickLyricsName(files.map { it.name }, prefer)
            val lyricsUrl = files.firstOrNull { it.name.equals(lyricsName, ignoreCase = true) }
                ?.toURI()?.toString()
            songs += Song(
                id = file.absolutePath,
                title = parsed.first,
                artist = if (parsed.second != "NAS") parsed.second else "U盘",
                source = MediaSource.Usb,
                relativePath = file.absolutePath,
                mvUrl = if (isMv) uri else null,
                originalUrl = if (isOriginal || (!isMv && !isAcc)) uri else null,
                accompanimentUrl = if (isAcc) uri else null,
                lyricsUrl = lyricsUrl,
            )
        }
        for (sub in children.filter { it.isDirectory }) {
            songFromFolder(sub.absolutePath, sub.name)
                ?.takeIf { it.anyPlayableUrl() != null }
                ?.let { songs += it }
        }
        songs
    }

    suspend fun loadLyricsText(song: Song): String? = withContext(Dispatchers.IO) {
        val url = song.lyricsUrl ?: return@withContext null
        try {
            when {
                url.startsWith("file:") -> File(URI(url)).takeIf { it.isFile }?.readText(Charsets.UTF_8)
                url.startsWith("/") -> File(url).takeIf { it.isFile }?.readText(Charsets.UTF_8)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDir(rootPath: String, relativeOrAbs: String): File? {
        val raw = relativeOrAbs.trim().ifEmpty { rootPath }
        val f = File(raw)
        return when {
            f.isAbsolute && f.exists() -> f
            else -> File(rootPath, raw.trimStart('/')).takeIf { it.exists() }
        }
    }

    private fun classify(file: File): LibraryEntryKind {
        if (file.isDirectory) return LibraryEntryKind.Folder
        val lower = file.name.lowercase()
        if (lower.endsWith(".json") || lower.endsWith(".m3u") || lower.endsWith(".m3u8")) {
            return LibraryEntryKind.Manifest
        }
        if (lower.endsWith(".lrc")) return LibraryEntryKind.Lyrics
        val ext = file.extension.lowercase()
        if (ext in audioExt) {
            return if (ext in listOf("mp4", "m4v", "mov", "mkv")) LibraryEntryKind.Video
            else LibraryEntryKind.Audio
        }
        return LibraryEntryKind.Other
    }

    private fun decodeJson(body: String, baseDir: String): List<Song>? {
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
                    fun abs(key: String): String? {
                        val raw = item.optString(key).takeIf { it.isNotBlank() } ?: return null
                        if (raw.startsWith("http") || raw.startsWith("file:")) return raw
                        val f = File(raw)
                        val resolved = if (f.isAbsolute) f else File(baseDir, raw)
                        return if (resolved.exists()) resolved.toURI().toString() else null
                    }
                    add(
                        Song(
                            id = item.optString("id").ifBlank { "usb-$i-$title" },
                            title = title,
                            artist = item.optString("artist", "U盘"),
                            durationSec = item.optLong("duration", 240),
                            source = MediaSource.Usb,
                            mvUrl = abs("mv") ?: abs("mvUrl"),
                            originalUrl = abs("original") ?: abs("originalUrl") ?: abs("url"),
                            accompanimentUrl = abs("accompaniment") ?: abs("accompanimentUrl"),
                            lyricsUrl = abs("lyrics") ?: abs("lyricsUrl") ?: abs("lrc"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

sealed class LocalLibraryError(message: String) : Exception(message) {
    class NotFound(msg: String) : LocalLibraryError(msg)
    class NoPermission(msg: String) : LocalLibraryError(msg)
    class Empty(msg: String) : LocalLibraryError(msg)
}
