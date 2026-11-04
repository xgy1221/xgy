package com.xgy.androidkaraoke.library

import com.xgy.androidkaraoke.lyrics.LyricsFiles
import com.xgy.androidkaraoke.model.MediaSource
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.nas.KaraokeFileNames
import com.xgy.androidkaraoke.nas.NasConfig
import com.xgy.androidkaraoke.nas.TrackRole
import com.xgy.androidkaraoke.nas.TrackRoles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URI

/**
 * 本地/U 盘目录浏览：与 NAS 相同的文件夹三轨约定 + library.json + `.lrc`。
 * 原唱 = 完整混音；伴唱 = 纯伴奏；忽略 Demucs vocals 干声。
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
            var mvName: String? = null
            var original: String? = null
            var originalName: String? = null
            var acc: String? = null
            var accName: String? = null
            val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
            files.forEach { f ->
                val uri = f.toURI().toString()
                when (TrackRoles.classify(f.name)) {
                    TrackRole.Lyrics, TrackRole.Ignore -> Unit
                    TrackRole.Mv -> {
                        val next = TrackRoles.pickPreferred(mv, mvName, uri, f.name)
                        if (next != mv) {
                            mv = next
                            mvName = f.name
                        }
                    }
                    TrackRole.Original -> {
                        val next = TrackRoles.pickPreferred(original, originalName, uri, f.name)
                        if (next != original) {
                            original = next
                            originalName = f.name
                        }
                    }
                    TrackRole.Accompaniment -> {
                        val next = TrackRoles.pickPreferred(acc, accName, uri, f.name)
                        if (next != acc) {
                            acc = next
                            accName = f.name
                        }
                    }
                    TrackRole.Other -> {
                        if (f.extension.lowercase() in videoExt && mv == null) {
                            mv = uri
                            mvName = f.name
                        }
                    }
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
            val role = TrackRoles.classify(file.name)
            if (role == TrackRole.Ignore) continue
            val uri = file.toURI().toString()
            val parsed = KaraokeFileNames.parseTitleArtist(file.name)
            val prefer = LyricsFiles.preferredNames(parsed.first, parsed.second, file.name)
            val lyricsName = LyricsFiles.pickLyricsName(files.map { it.name }, prefer)
            val lyricsUrl = files.firstOrNull { it.name.equals(lyricsName, ignoreCase = true) }
                ?.toURI()?.toString()
            val isMv = kind == LibraryEntryKind.Video || role == TrackRole.Mv
            songs += Song(
                id = file.absolutePath,
                title = parsed.first,
                artist = if (parsed.second != "NAS") parsed.second else "U盘",
                source = MediaSource.Usb,
                relativePath = file.absolutePath,
                mvUrl = if (isMv && role != TrackRole.Accompaniment && role != TrackRole.Original) uri else null,
                originalUrl = when {
                    role == TrackRole.Original -> uri
                    !isMv && role == TrackRole.Other -> uri
                    else -> null
                },
                accompanimentUrl = if (role == TrackRole.Accompaniment) uri else null,
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

    /** @return Pair(正文, lyricsUrl) */
    suspend fun loadLyricsText(song: Song): Pair<String, String>? = withContext(Dispatchers.IO) {
        val resolved = song.lyricsUrl?.takeIf { it.isNotBlank() } ?: discoverLyricsUrl(song)
            ?: return@withContext null
        try {
            val text = when {
                resolved.startsWith("file:") -> File(URI(resolved)).takeIf { it.isFile }?.readText(Charsets.UTF_8)
                resolved.startsWith("/") -> File(resolved).takeIf { it.isFile }?.readText(Charsets.UTF_8)
                else -> null
            } ?: return@withContext null
            if (text.isBlank()) null else text to resolved
        } catch (_: Exception) {
            null
        }
    }

    suspend fun discoverLyricsUrl(song: Song): String? = withContext(Dispatchers.IO) {
        val rel = song.relativePath ?: return@withContext null
        val base = File(rel)
        val dir = when {
            base.isDirectory -> base
            base.isFile -> base.parentFile
            else -> base.parentFile
        } ?: return@withContext null
        val files = dir.listFiles()?.filter { it.isFile } ?: return@withContext null
        val prefer = LyricsFiles.preferredNames(song.title, song.artist, dir.name)
        val name = LyricsFiles.pickLyricsName(files.map { it.name }, prefer) ?: return@withContext null
        files.firstOrNull { it.name.equals(name, ignoreCase = true) }?.toURI()?.toString()
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
                            originalUrl = abs("original") ?: abs("originalUrl") ?: abs("fullmix") ?: abs("url"),
                            accompanimentUrl = abs("accompaniment") ?: abs("accompanimentUrl") ?: abs("instrumental"),
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
