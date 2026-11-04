package com.xgy.androidkaraoke.library

import com.xgy.androidkaraoke.BrandConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class LibrarySourceKind { Nas, Usb }

/**
 * 曲库来源：NAS 或本机/U 盘路径。可启用/禁用；多来源并存。
 */
data class LibrarySource(
    val id: String,
    val kind: LibrarySourceKind,
    val displayName: String,
    val enabled: Boolean = true,
    /** NAS */
    val nasBaseUrl: String? = null,
    val nasLibraryPath: String? = null,
    /** USB / 本地根路径（绝对路径） */
    val localRootPath: String? = null,
    val volumeUuid: String? = null,
    /** 相对根的曲库子路径，如 music 或 Karaoke */
    val relativeMusicPath: String = "",
) {
    val browseRoot: String
        get() = when (kind) {
            LibrarySourceKind.Nas -> {
                var p = (nasLibraryPath ?: BrandConfig.defaultNasLibraryPath).trim()
                if (p.isEmpty()) p = "/"
                if (!p.startsWith("/")) p = "/$p"
                while (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
                p
            }
            LibrarySourceKind.Usb -> {
                val root = localRootPath?.trimEnd('/')
                if (root.isNullOrEmpty()) {
                    ""
                } else {
                    val rel = relativeMusicPath.trim().trim('/')
                    if (rel.isEmpty()) root else "$root/$rel"
                }
            }
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("displayName", displayName)
        .put("enabled", enabled)
        .put("nasBaseUrl", nasBaseUrl)
        .put("nasLibraryPath", nasLibraryPath)
        .put("localRootPath", localRootPath)
        .put("volumeUuid", volumeUuid)
        .put("relativeMusicPath", relativeMusicPath)

    companion object {
        fun defaultNas(): LibrarySource = LibrarySource(
            id = "nas-default",
            kind = LibrarySourceKind.Nas,
            displayName = "NAS 默认",
            enabled = true,
            nasBaseUrl = BrandConfig.defaultNasBaseUrl,
            nasLibraryPath = BrandConfig.defaultNasLibraryPath,
        )

        fun fromJson(obj: JSONObject): LibrarySource = LibrarySource(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            kind = runCatching { LibrarySourceKind.valueOf(obj.getString("kind")) }
                .getOrDefault(LibrarySourceKind.Nas),
            displayName = obj.optString("displayName", "曲库"),
            enabled = obj.optBoolean("enabled", true),
            nasBaseUrl = obj.optString("nasBaseUrl").takeIf { it.isNotBlank() },
            nasLibraryPath = obj.optString("nasLibraryPath").takeIf { it.isNotBlank() },
            localRootPath = obj.optString("localRootPath").takeIf { it.isNotBlank() },
            volumeUuid = obj.optString("volumeUuid").takeIf { it.isNotBlank() },
            relativeMusicPath = obj.optString("relativeMusicPath", ""),
        )

        fun listFromJson(raw: String?): List<LibrarySource> {
            if (raw.isNullOrBlank()) return listOf(defaultNas())
            return try {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) add(fromJson(arr.getJSONObject(i)))
                }.ifEmpty { listOf(defaultNas()) }
            } catch (_: Exception) {
                listOf(defaultNas())
            }
        }

        fun listToJson(list: List<LibrarySource>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}

enum class LibraryEntryKind { Folder, Audio, Video, Manifest, Lyrics, Other }

data class LibraryEntry(
    val id: String,
    val name: String,
    val path: String,
    val kind: LibraryEntryKind,
    val playableUri: String? = null,
    val sourceId: String,
)
