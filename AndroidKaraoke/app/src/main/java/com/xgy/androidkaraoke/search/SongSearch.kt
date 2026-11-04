package com.xgy.androidkaraoke.search

import com.xgy.androidkaraoke.model.Song

/**
 * 歌名 / 歌星 / 首拼匹配（TV 强搜索）。
 * 首拼：拉丁取首字母；汉字用内置常用表 + 拼音近似。
 */
object SongSearch {
    fun matches(song: Song, rawQuery: String): Boolean {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return true
        val title = song.title
        val artist = song.artist
        if (title.contains(q, ignoreCase = true) || artist.contains(q, ignoreCase = true)) return true
        val compact = q.replace(" ", "")
        if (title.replace(" ", "").contains(compact, ignoreCase = true)) return true
        if (artist.replace(" ", "").contains(compact, ignoreCase = true)) return true
        val titlePy = initials(title)
        val artistPy = initials(artist)
        return titlePy.contains(compact) ||
            artistPy.contains(compact) ||
            titlePy.startsWith(compact) ||
            artistPy.startsWith(compact)
    }

    fun filter(songs: List<Song>, query: String, limit: Int = 80): List<Song> {
        val q = query.trim()
        if (q.isEmpty()) return songs.take(limit)
        return songs.asSequence()
            .filter { matches(it, q) }
            .sortedByDescending { score(it, q) }
            .take(limit)
            .toList()
    }

    fun initials(text: String): String = buildString {
        text.forEach { c ->
            when {
                c.isWhitespace() || c == '·' || c == '-' || c == '_' -> Unit
                c.code < 128 -> {
                    if (c.isLetterOrDigit()) append(c.lowercaseChar())
                }
                else -> append(chineseInitial(c))
            }
        }
    }.lowercase()

    private fun score(song: Song, raw: String): Int {
        val q = raw.trim().lowercase()
        var s = 0
        if (song.title.equals(raw.trim(), ignoreCase = true)) s += 100
        if (song.title.startsWith(q, ignoreCase = true)) s += 40
        if (song.artist.startsWith(q, ignoreCase = true)) s += 30
        if (initials(song.title).startsWith(q.replace(" ", ""))) s += 50
        if (song.title.contains(q, ignoreCase = true)) s += 20
        if (song.artist.contains(q, ignoreCase = true)) s += 15
        s += song.hotScore / 10
        return s
    }

    private fun chineseInitial(c: Char): Char {
        COMMON[c]?.let { return it }
        // 粗略按 Unicode 区位近似（覆盖不足时仍可用歌名原文匹配）
        val code = c.code
        if (code in 0x4E00..0x9FFF) {
            val bucket = (code - 0x4E00) % 26
            return ('a' + bucket)
        }
        return '#'
    }

    /** 点歌高频字 → 首拼（可继续扩充） */
    private val COMMON: Map<Char, Char> = mapOf(
        '小' to 'x', '半' to 'b', '茶' to 'c', '花' to 'h', '开' to 'k', '了' to 'l',
        '咏' to 'y', '春' to 'c', '失' to 's', '眠' to 'm', '奢' to 's', '香' to 'x',
        '夫' to 'f', '人' to 'r', '西' to 'x', '海' to 'h', '情' to 'q', '歌' to 'g',
        '刀' to 'd', '郎' to 'l', '那' to 'n', '英' to 'y', '陈' to 'c', '奕' to 'y',
        '迅' to 'x', '蔡' to 'c', '依' to 'y', '林' to 'l', '王' to 'w', '琪' to 'q',
        '卓' to 'z', '婷' to 't', '蒋' to 'j', '大' to 'd', '为' to 'w', '爱' to 'a',
        '你' to 'n', '我' to 'w', '的' to 'd', '是' to 's', '不' to 'b', '一' to 'y',
        '天' to 't', '地' to 'd', '月' to 'y', '光' to 'g', '风' to 'f', '雨' to 'y',
        '梦' to 'm', '想' to 'x', '心' to 'x', '里' to 'l', '说' to 's', '话' to 'h',
        '朋' to 'p', '友' to 'y', '家' to 'j', '乡' to 'x', '母' to 'm', '亲' to 'q',
        '男' to 'n', '女' to 'n', '孩' to 'h', '老' to 'l', '少' to 's', '年' to 'n',
        '红' to 'h', '色' to 's', '蓝' to 'l', '绿' to 'l', '白' to 'b', '黑' to 'h',
        '北' to 'b', '南' to 'n', '东' to 'd', '走' to 'z', '来' to 'l', '去' to 'q',
        '唱' to 'c', '跳' to 't', '舞' to 'w', '乐' to 'l', '曲' to 'q', '音' to 'y',
        '七' to 'q', '朵' to 'd', '组' to 'z', '合' to 'h', '凤' to 'f', '凰' to 'h',
        '传' to 'c', '奇' to 'q', '粒' to 'l', '睿' to 'r', '舒' to 's', '妤' to 'y',
        '民' to 'm', '谣' to 'y', '流' to 'l', '行' to 'x', '古' to 'g', '风' to 'f',
        '热' to 'r', '门' to 'm', '榜' to 'b', '首' to 's', '推' to 't', '荐' to 'j',
    )
}
