package com.xgy.androidkaraoke.recommend

import android.content.Context
import org.json.JSONObject

/**
 * 常唱偏好：由已点 / 收藏 / 播完累计歌手与曲风权重。
 * 不依赖「当前正在播放」作为主信号。
 */
class ListeningPreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun artistWeights(): Map<String, Float> = readMap(KEY_ARTISTS)
    fun genreWeights(): Map<String, Float> = readMap(KEY_GENRES)
    fun favoriteIds(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()
    fun historySongIds(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: return emptyList()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun recordQueued(artist: String, genres: List<String>, songId: String) {
        bumpArtist(artist, 2f)
        bumpGenres(genres, 2f)
        appendHistory(songId)
    }

    fun recordFavorite(artist: String, genres: List<String>, songId: String, favorited: Boolean) {
        val favs = favoriteIds().toMutableSet()
        if (favorited) {
            favs.add(songId)
            bumpArtist(artist, 3f)
            bumpGenres(genres, 3f)
        } else {
            favs.remove(songId)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favs).apply()
    }

    fun recordPlayCompleted(artist: String, genres: List<String>, songId: String) {
        bumpArtist(artist, 4f)
        bumpGenres(genres, 3.5f)
        appendHistory(songId)
    }

    fun isFavorite(songId: String): Boolean = songId in favoriteIds()

    private fun bumpArtist(artist: String, delta: Float) {
        if (artist.isBlank()) return
        val map = artistWeights().toMutableMap()
        map[artist] = (map[artist] ?: 0f) + delta
        writeMap(KEY_ARTISTS, map)
    }

    private fun bumpGenres(genres: List<String>, delta: Float) {
        if (genres.isEmpty()) return
        val map = genreWeights().toMutableMap()
        genres.forEach { g ->
            val key = g.trim()
            if (key.isNotEmpty()) map[key] = (map[key] ?: 0f) + delta
        }
        writeMap(KEY_GENRES, map)
    }

    private fun appendHistory(songId: String) {
        val list = historySongIds().toMutableList()
        list.remove(songId)
        list.add(0, songId)
        while (list.size > 80) list.removeLast()
        prefs.edit().putString(KEY_HISTORY, list.joinToString(",")).apply()
    }

    private fun readMap(key: String): Map<String, Float> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k -> put(k, obj.getDouble(k).toFloat()) }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeMap(key: String, map: Map<String, Float>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v.toDouble()) }
        prefs.edit().putString(key, obj.toString()).apply()
    }

    companion object {
        private const val PREFS = "listening_prefs"
        private const val KEY_ARTISTS = "artists"
        private const val KEY_GENRES = "genres"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HISTORY = "history_ids"
    }
}
