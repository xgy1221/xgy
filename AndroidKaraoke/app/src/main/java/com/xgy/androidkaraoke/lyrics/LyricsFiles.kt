package com.xgy.androidkaraoke.lyrics

/**
 * 曲包 / 扁平文件旁的 LRC 命名约定。
 * 优先：`lyrics.lrc` → `歌名-歌手.lrc` → 同名 `.lrc` → 目录内任意 `.lrc`。
 */
object LyricsFiles {
    fun preferredNames(title: String? = null, artist: String? = null, folderOrBase: String? = null): List<String> {
        val names = linkedSetOf<String>()
        names += "lyrics.lrc"
        val base = folderOrBase?.substringAfterLast('/')?.substringBeforeLast('.')?.trim().orEmpty()
        if (base.isNotEmpty()) names += "$base.lrc"
        val t = title?.trim().orEmpty()
        val a = artist?.trim().orEmpty()
        if (t.isNotEmpty() && a.isNotEmpty()) names += "$t-$a.lrc"
        if (t.isNotEmpty()) names += "$t.lrc"
        return names.toList()
    }

    fun pickLyricsName(available: Collection<String>, prefer: List<String>): String? {
        val lrcs = available.filter { it.lowercase().endsWith(".lrc") }
        if (lrcs.isEmpty()) return null
        for (want in prefer) {
            lrcs.firstOrNull { it.equals(want, ignoreCase = true) }?.let { return it }
        }
        return lrcs.firstOrNull { it.equals("lyrics.lrc", ignoreCase = true) } ?: lrcs.firstOrNull()
    }

    fun isLyricsFile(name: String): Boolean = name.lowercase().endsWith(".lrc")
}
