package com.xgy.androidkaraoke.recommend

import com.xgy.androidkaraoke.model.Song

/**
 * 按常唱偏好排序：同歌星 > 相近曲风/标签 > 热门兜底。
 * [nowPlayingId] 仅作极弱降权（避免总推当前这首），不作主关联依据。
 */
object RecommendationEngine {
    fun rank(
        candidates: List<Song>,
        artistWeights: Map<String, Float>,
        genreWeights: Map<String, Float>,
        excludeIds: Set<String> = emptySet(),
        nowPlayingId: String? = null,
        limit: Int = 12,
    ): List<Song> {
        if (candidates.isEmpty()) return emptyList()
        val hasPreference = artistWeights.isNotEmpty() || genreWeights.isNotEmpty()
        return candidates
            .asSequence()
            .filter { it.id !in excludeIds }
            .map { song ->
                song to score(song, artistWeights, genreWeights, hasPreference, nowPlayingId)
            }
            .sortedWith(
                compareByDescending<Pair<Song, Float>> { it.second }
                    .thenByDescending { it.first.hotScore }
                    .thenBy { it.first.title },
            )
            .map { it.first }
            .take(limit)
            .toList()
    }

    fun score(
        song: Song,
        artistWeights: Map<String, Float>,
        genreWeights: Map<String, Float>,
        hasPreference: Boolean = artistWeights.isNotEmpty() || genreWeights.isNotEmpty(),
        nowPlayingId: String? = null,
    ): Float {
        var s = 0f
        s += (artistWeights[song.artist] ?: 0f) * 3f
        song.styleTags.forEach { tag ->
            s += (genreWeights[tag] ?: 0f) * 2f
        }
        // 热门兜底：无偏好时完全靠热门；有偏好时仅小幅加权
        s += if (hasPreference) song.hotScore * 0.15f else song.hotScore * 1.2f
        // 当前播放仅弱信号：略降权，避免「猜你想唱」围着这一首打转
        if (nowPlayingId != null && song.id == nowPlayingId) {
            s -= 8f
        }
        return s
    }
}
