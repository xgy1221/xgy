package com.xgy.androidkaraoke.lyrics

/**
 * 标准 / 增强型 LRC 解析。
 *
 * 行：`[mm:ss.xx]歌词` 或 `[mm:ss.xxx]歌词`
 * 字进度（可选）：`[00:12.00]<00:12.00>你<00:12.40>好`
 */
data class LyricWord(
    val timeMs: Long,
    val text: String,
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
)

data class LyricsDocument(
    val lines: List<LyricLine>,
    val title: String? = null,
    val artist: String? = null,
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    /** 当前应高亮的行；position 在首行之前返回 -1。 */
    fun lineIndexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (positionMs < lines.first().timeMs) return -1
        var lo = 0
        var hi = lines.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (lines[mid].timeMs <= positionMs) lo = mid else hi = mid - 1
        }
        return lo
    }

    /**
     * 当前行内进度 0..1（「唱到哪」）。
     * 有逐字时间戳时按字插值；否则按本行起点 → 下一行起点线性估算。
     */
    fun lineProgressAt(positionMs: Long): Float {
        val idx = lineIndexAt(positionMs)
        if (idx < 0) return 0f
        val line = lines[idx]
        val words = line.words
        if (words.size >= 2) {
            if (positionMs <= words.first().timeMs) return 0f
            if (positionMs >= words.last().timeMs) return 1f
            var w = 0
            while (w < words.lastIndex && words[w + 1].timeMs <= positionMs) w++
            val a = words[w]
            val b = words.getOrNull(w + 1) ?: return 1f
            val span = (b.timeMs - a.timeMs).coerceAtLeast(1)
            val local = ((positionMs - a.timeMs).toFloat() / span).coerceIn(0f, 1f)
            val base = w.toFloat() / (words.size - 1)
            val step = 1f / (words.size - 1)
            return (base + step * local).coerceIn(0f, 1f)
        }
        val start = line.timeMs
        val end = lines.getOrNull(idx + 1)?.timeMs ?: (start + 4_000L)
        val span = (end - start).coerceAtLeast(1)
        return ((positionMs - start).toFloat() / span).coerceIn(0f, 1f)
    }
}

object LrcParser {
    private val meta = Regex("""^\[(ti|ar|al|by|offset):\s*(.*?)\s*]$""", RegexOption.IGNORE_CASE)
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")

    fun parse(raw: String): LyricsDocument {
        if (raw.isBlank()) return LyricsDocument(emptyList())
        var title: String? = null
        var artist: String? = null
        var offsetMs = 0L
        val collected = mutableListOf<LyricLine>()

        for (original in raw.lineSequence()) {
            var line = original.trim()
            if (line.isEmpty()) continue
            // UTF-8 BOM
            if (line.startsWith("\uFEFF")) line = line.removePrefix("\uFEFF")

            val metaMatch = meta.matchEntire(line)
            if (metaMatch != null) {
                when (metaMatch.groupValues[1].lowercase()) {
                    "ti" -> title = metaMatch.groupValues[2].ifBlank { null }
                    "ar" -> artist = metaMatch.groupValues[2].ifBlank { null }
                    "offset" -> offsetMs = metaMatch.groupValues[2].toLongOrNull() ?: 0L
                }
                continue
            }

            val times = mutableListOf<Long>()
            var cursor = 0
            while (true) {
                val m = timeTag.find(line, cursor) ?: break
                if (m.range.first != cursor) break
                times += parseTimeMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                cursor = m.range.last + 1
            }
            if (times.isEmpty()) continue
            val body = line.substring(cursor).trim()
            if (body.isEmpty()) continue
            val (plain, words) = parseBody(body, times.first())
            if (plain.isEmpty()) continue
            for (t in times) {
                val shifted = (t + offsetMs).coerceAtLeast(0L)
                val shiftedWords = if (words.isEmpty()) emptyList() else {
                    val delta = shifted - times.first()
                    words.map { it.copy(timeMs = (it.timeMs + delta).coerceAtLeast(0L)) }
                }
                collected += LyricLine(timeMs = shifted, text = plain, words = shiftedWords)
            }
        }

        val lines = collected.sortedWith(compareBy({ it.timeMs }, { it.text }))
            .distinctBy { it.timeMs to it.text }
        return LyricsDocument(lines = lines, title = title, artist = artist)
    }

    private fun parseBody(body: String, lineTimeMs: Long): Pair<String, List<LyricWord>> {
        if (!body.contains('<')) return body to emptyList()
        val words = mutableListOf<LyricWord>()
        var plain = StringBuilder()
        var idx = 0
        var lastTime = lineTimeMs
        while (idx < body.length) {
            val m = wordTag.find(body, idx)
            if (m == null) {
                val rest = body.substring(idx)
                if (rest.isNotEmpty()) {
                    plain.append(rest)
                    words += LyricWord(lastTime, rest)
                }
                break
            }
            if (m.range.first > idx) {
                val chunk = body.substring(idx, m.range.first)
                plain.append(chunk)
                words += LyricWord(lastTime, chunk)
            }
            lastTime = parseTimeMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
            idx = m.range.last + 1
        }
        val text = plain.toString().trim()
        return text to words.filter { it.text.isNotEmpty() }
    }

    private fun parseTimeMs(min: String, sec: String, frac: String?): Long {
        val minutes = min.toLongOrNull() ?: 0L
        val seconds = sec.toLongOrNull() ?: 0L
        val millis = when {
            frac.isNullOrBlank() -> 0L
            frac.length == 1 -> frac.toLong() * 100L
            frac.length == 2 -> frac.toLong() * 10L
            else -> frac.take(3).padEnd(3, '0').toLong()
        }
        return minutes * 60_000L + seconds * 1_000L + millis
    }
}
