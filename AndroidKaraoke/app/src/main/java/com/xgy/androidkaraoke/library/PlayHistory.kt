package com.xgy.androidkaraoke.library

import android.content.Context
import org.json.JSONObject

class PlayHistory(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun record(id: String) {
        val root = read()
        val item = root.optJSONObject(id) ?: JSONObject()
        item.put("count", item.optInt("count") + 1)
        item.put("last", System.currentTimeMillis())
        root.put(id, item)
        write(root)
    }

    fun count(id: String): Int = read().optJSONObject(id)?.optInt("count") ?: 0

    fun lastPlayed(id: String): Long = read().optJSONObject(id)?.optLong("last") ?: 0L

    fun isFavorite(id: String): Boolean = favorites().contains(id)

    fun toggleFavorite(id: String): Boolean {
        val set = favorites().toMutableSet()
        val now = if (set.contains(id)) {
            set.remove(id)
            false
        } else {
            set.add(id)
            true
        }
        prefs.edit().putStringSet(KEY_FAV, set).apply()
        return now
    }

    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAV, emptySet()).orEmpty()

    fun queueIds(): List<String> = prefs.getString(KEY_QUEUE, "").orEmpty().split('\n').filter { it.isNotBlank() }

    fun saveQueue(ids: List<String>) {
        prefs.edit().putString(KEY_QUEUE, ids.joinToString("\n")).apply()
    }

    private fun read(): JSONObject =
        runCatching { JSONObject(prefs.getString(KEY_COUNTS, "{}").orEmpty().ifBlank { "{}" }) }
            .getOrDefault(JSONObject())

    private fun write(root: JSONObject) {
        prefs.edit().putString(KEY_COUNTS, root.toString()).apply()
    }

    companion object {
        private const val PREFS = "play_history"
        private const val KEY_COUNTS = "counts"
        private const val KEY_FAV = "favorites"
        private const val KEY_QUEUE = "queue"
    }
}
