package com.xgy.androidkaraoke.library

import com.xgy.androidkaraoke.model.Song
import java.security.MessageDigest

data class MediaFile(
    val relativePath: String,
    val url: String,
)

/**
 * 一首歌 = 同一目录（或同一文件名主干）下的 MV + 原唱音源 + 伴唱音源。
 *
 * 推荐目录：
 * 歌手/歌名/歌名.mp4
 * 歌手/歌名/歌名-原唱.mp3
 * 歌手/歌名/歌名-伴唱.mp3
 */
object SongGrouper {
    private val videoExt = setOf("mp4", "mkv", "m4v", "mov", "ts", "webm", "avi")
    private val audioExt = setOf("mp3", "flac", "m4a", "aac", "wav", "ogg", "ape", "wma")
    private val generic = setOf(
        "music", "songs", "song", "mp3", "audio", "video", "mv", "karaoke",
        "曲库", "音乐", "歌曲", "伴奏", "volume1", "volume2", "homes",
    )
    private val originalWords = listOf("原唱", "原声", "原曲", "original", "vocal")
    private val accompanyWords = listOf("伴唱", "伴奏带", "伴奏", "karaoke", "offvocal", "inst", "消音版", "消音")

    fun group(files: List<MediaFile>): List<Song> {
        val media = files.map { it.copy(relativePath = it.relativePath.trim().trim('/')) }
            .filter { roleOf(it.name) != null }
        if (media.isEmpty()) return emptyList()

        return media.groupBy { parentOf(it.relativePath) }
            .flatMap { (dir, items) -> songsInDirectory(dir, items) }
            .sortedWith(compareBy({ it.artist }, { it.title }))
    }

    private fun songsInDirectory(dir: String, items: List<MediaFile>): List<Song> {
        val clusters = items.groupBy { stemOf(it.name).ifBlank { it.name.substringBeforeLast('.') } }
        val dirs = dir.split('/').filter { it.isNotBlank() && !generic(it) }
        val folderTitle = dirs.lastOrNull()
        val artistFromPath = dirs.dropLast(1).lastOrNull()
        val collection = if (dirs.size >= 3) dirs.first() else null
        val single = clusters.size == 1
        return clusters.mapNotNull { (stem, cluster) ->
            var title = if (single && !folderTitle.isNullOrBlank()) folderTitle else stem
            var artist = artistFromPath ?: "未知歌手"
            if (artist == "未知歌手") {
                val bits = title.split('-', '—', '－').map { it.trim() }.filter { it.isNotEmpty() }
                if (bits.size >= 2) {
                    title = bits[0]
                    artist = bits[1]
                }
            }
            var mv: String? = null
            var original: String? = null
            var accompany: String? = null
            for (file in cluster) {
                when (roleOf(file.name)) {
                    Role.Mv -> if (mv == null) mv = file.url
                    Role.Original -> if (original == null) original = file.url
                    Role.Accompany -> if (accompany == null) accompany = file.url
                    Role.Audio -> when {
                        accompany == null -> accompany = file.url
                        original == null -> original = file.url
                    }
                    null -> Unit
                }
            }
            if (mv == null && original == null && accompany == null) return@mapNotNull null
            Song(
                id = sha1("$dir/$stem"),
                title = title.ifBlank { "未命名" },
                artist = artist.ifBlank { "未知歌手" },
                playlist = collection ?: "我的曲库",
                mvUrl = mv,
                originalUrl = original,
                accompanyUrl = accompany,
                relativePath = dir.ifBlank { null },
            )
        }
    }

    private val MediaFile.name: String get() = relativePath.substringAfterLast('/')

    private fun parentOf(relativePath: String): String {
        val norm = relativePath.trim('/')
        val index = norm.lastIndexOf('/')
        return if (index < 0) "" else norm.substring(0, index)
    }

    private fun generic(name: String) = generic.contains(name.lowercase())

    internal fun stemOf(fileName: String): String {
        var stem = fileName.substringBeforeLast('.')
        val tokens = (originalWords + accompanyWords + listOf("mv", "hd"))
            .sortedByDescending { it.length }
        for (token in tokens) {
            stem = stem.replace(token, "", ignoreCase = true)
        }
        return stem.trim().trim('-', '_', ' ', '.', '(', ')', '（', '）', '·')
    }

    internal fun roleOf(fileName: String): Role? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty() || (ext !in videoExt && ext !in audioExt)) return null
        val lower = fileName.lowercase()
        val original = originalWords.any { lower.contains(it) }
        val accompany = accompanyWords.any { lower.contains(it) }
        val video = ext in videoExt
        return when {
            accompany && !video -> Role.Accompany
            original && !video -> Role.Original
            video && accompany -> Role.Accompany
            video && original -> Role.Original
            video -> Role.Mv
            else -> Role.Audio
        }
    }

    private fun sha1(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    enum class Role { Mv, Original, Accompany, Audio }
}
