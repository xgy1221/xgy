package com.xgy.androidkaraoke.library

import android.content.Context

class LibrarySourceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<LibrarySource> =
        LibrarySource.listFromJson(prefs.getString(KEY_SOURCES, null))

    fun save(sources: List<LibrarySource>) {
        prefs.edit().putString(KEY_SOURCES, LibrarySource.listToJson(sources)).apply()
    }

    fun upsert(source: LibrarySource) {
        val list = load().toMutableList()
        val idx = list.indexOfFirst { it.id == source.id }
        if (idx >= 0) list[idx] = source else list.add(source)
        save(list)
    }

    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        save(load().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun activeSourceId(): String? = prefs.getString(KEY_ACTIVE, null)
        ?: load().firstOrNull { it.enabled }?.id

    fun setActiveSourceId(id: String) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    companion object {
        private const val PREFS = "library_sources"
        private const val KEY_SOURCES = "sources"
        private const val KEY_ACTIVE = "active_id"
    }
}
