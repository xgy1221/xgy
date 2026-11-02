package com.xgy.androidkaraoke.library

import android.content.Context
import com.xgy.androidkaraoke.model.LibrarySource
import com.xgy.androidkaraoke.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "song-cache.json")

    fun loadLibraries(): List<LibrarySource> {
        val raw = prefs.getString(KEY_LIBS, null) ?: return listOf(LibrarySource.homeDefault())
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return listOf(LibrarySource.homeDefault())
        val items = buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    LibrarySource(
                        id = obj.optString("id").ifBlank { "lib-$i" },
                        name = obj.optString("name").ifBlank { "曲库" },
                        baseUrl = obj.optString("baseUrl"),
                        path = obj.optString("path"),
                        username = obj.optString("username"),
                    ),
                )
            }
        }
        return items.ifEmpty { listOf(LibrarySource.homeDefault()) }
    }

    fun saveLibraries(items: List<LibrarySource>) {
        val array = JSONArray()
        items.forEach { lib ->
            array.put(
                JSONObject()
                    .put("id", lib.id)
                    .put("name", lib.name)
                    .put("baseUrl", lib.normalizedBaseUrl)
                    .put("path", lib.normalizedPath)
                    .put("username", lib.username),
            )
        }
        prefs.edit().putString(KEY_LIBS, array.toString()).apply()
    }

    fun loadSongs(): List<Song> {
        if (!cacheFile.exists()) return emptyList()
        val root = runCatching { JSONObject(cacheFile.readText()) }.getOrNull() ?: return emptyList()
        val array = root.optJSONArray("songs") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val title = obj.optString("title")
                if (title.isBlank()) continue
                add(
                    Song(
                        id = obj.optString("id").ifBlank { "cached-$i" },
                        title = title,
                        artist = obj.optString("artist").ifBlank { "未知歌手" },
                        playlist = obj.optString("playlist").ifBlank { "我的曲库" },
                        mvUrl = obj.optString("mv").takeIf { it.isNotBlank() },
                        originalUrl = obj.optString("original").takeIf { it.isNotBlank() },
                        accompanyUrl = obj.optString("accompany").takeIf { it.isNotBlank() },
                        relativePath = obj.optString("path").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    fun saveSongs(songs: List<Song>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject()
                    .put("id", song.id)
                    .put("title", song.title)
                    .put("artist", song.artist)
                    .put("playlist", song.playlist)
                    .put("mv", song.mvUrl.orEmpty())
                    .put("original", song.originalUrl.orEmpty())
                    .put("accompany", song.accompanyUrl.orEmpty())
                    .put("path", song.relativePath.orEmpty()),
            )
        }
        cacheFile.writeText(JSONObject().put("songs", array).toString())
    }

    companion object {
        private const val PREFS = "libraries"
        private const val KEY_LIBS = "json"
    }
}
