package com.xgy.androidkaraoke.nas

/** 扁平原文件名解析：`丑八怪-薛之谦.mp4` → 歌名 / 歌手 */
object KaraokeFileNames {
    fun parseTitleArtist(fileName: String): Pair<String, String> {
        val base = fileName.substringBeforeLast('.').trim()
        if (base.isEmpty()) return "未命名" to "NAS"
        val parts = base.split('-', '－', '—', limit = 2).map { it.trim() }.filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> parts[0] to parts[1]
            else -> base to "NAS"
        }
    }
}
