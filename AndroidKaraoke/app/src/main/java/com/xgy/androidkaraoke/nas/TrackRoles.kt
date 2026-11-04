package com.xgy.androidkaraoke.nas

/**
 * 曲包音轨角色约定：
 * - **original / 原唱** = 完整混音（源 MV 音轨，人声+伴奏）
 * - **accompaniment / 伴唱** = 纯伴奏（无人声）
 * - Demucs 分离出的 **vocals 干声绝不是原唱**，扫描时忽略。
 */
enum class TrackRole {
    Mv,
    Original,
    Accompaniment,
    Lyrics,
    /** Demucs vocals 等不应进播放轨 */
    Ignore,
    Other,
}

object TrackRoles {
    fun classify(fileName: String): TrackRole {
        val lower = fileName.lowercase().substringAfterLast('/').trim()
        if (lower.isEmpty()) return TrackRole.Other
        if (lower.endsWith(".lrc")) return TrackRole.Lyrics

        if (
            lower.startsWith("mv") ||
            lower.contains(".mv.") ||
            lower.contains("-mv.") ||
            lower.contains("_mv.")
        ) {
            return TrackRole.Mv
        }

        // 伴奏先于 vocals 判断（Demucs 文件名 no_vocals 含 vocals 字样）
        if (
            lower.contains("accompaniment") ||
            lower.contains("instrumental") ||
            lower.contains("伴唱") ||
            lower.contains("伴奏") ||
            lower.startsWith("acc.") ||
            lower.startsWith("acc-") ||
            lower.startsWith("acc_") ||
            lower.startsWith("inst.") ||
            lower.startsWith("inst-") ||
            lower.startsWith("inst_") ||
            lower.startsWith("no_vocals") ||
            lower.startsWith("no-vocals") ||
            lower.contains("no_vocals") ||
            lower.contains("no-vocals")
        ) {
            return TrackRole.Accompaniment
        }

        // 干声 / 分离人声：禁止当原唱
        if (
            lower.contains("vocals") ||
            lower.startsWith("vocal.") ||
            lower.startsWith("vocal-") ||
            lower.startsWith("vocal_") ||
            lower.contains("人声干") ||
            lower.contains("干声") ||
            lower.contains("stem-vocal") ||
            lower.contains("stem_vocal")
        ) {
            return TrackRole.Ignore
        }

        if (
            lower.contains("original") ||
            lower.contains("原唱") ||
            lower.contains("原曲") ||
            lower.contains("混音") ||
            lower.contains("fullmix") ||
            lower.contains("full_mix") ||
            lower.contains("full-mix")
        ) {
            return TrackRole.Original
        }

        return TrackRole.Other
    }
}
