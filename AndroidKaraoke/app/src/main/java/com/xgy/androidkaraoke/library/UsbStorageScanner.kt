package com.xgy.androidkaraoke.library

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File

/**
 * 扫描可移除存储（U 盘）。兼容 Android 10 / API 29（反射 getPath）。
 */
class UsbStorageScanner(private val context: Context) {
    data class VolumeInfo(
        val uuid: String,
        val label: String,
        val rootPath: String,
        val mounted: Boolean,
    )

    fun listRemovableVolumes(): List<VolumeInfo> {
        val sm = context.getSystemService(StorageManager::class.java) ?: return emptyList()
        return sm.storageVolumes.mapNotNull { vol ->
            if (!vol.isRemovable) return@mapNotNull null
            val path = vol.pathCompat() ?: return@mapNotNull null
            if (!path.exists()) return@mapNotNull null
            val uuid = vol.uuid?.takeIf { it.isNotBlank() }
                ?: ("path-" + path.absolutePath.hashCode())
            val label = vol.getDescription(context)?.takeIf { it.isNotBlank() }
                ?: path.name.ifBlank { "U 盘" }
            val mounted = if (Build.VERSION.SDK_INT >= 30) {
                val state = vol.state
                state == android.os.Environment.MEDIA_MOUNTED ||
                    state == android.os.Environment.MEDIA_MOUNTED_READ_ONLY
            } else {
                path.canRead()
            }
            VolumeInfo(uuid = uuid, label = label, rootPath = path.absolutePath, mounted = mounted)
        }
    }

    fun syncUsbSources(store: LibrarySourceStore): List<LibrarySource> {
        val volumes = listRemovableVolumes()
        val existing = store.load().toMutableList()
        val usbIds = volumes.map { "usb-${it.uuid}" }.toSet()

        // 更新/添加已挂载
        volumes.forEach { vol ->
            val id = "usb-${vol.uuid}"
            val idx = existing.indexOfFirst { it.id == id }
            val source = LibrarySource(
                id = id,
                kind = LibrarySourceKind.Usb,
                displayName = "U 盘 · ${vol.label}",
                enabled = if (idx >= 0) existing[idx].enabled else true,
                localRootPath = vol.rootPath,
                volumeUuid = vol.uuid,
                relativeMusicPath = existing.getOrNull(idx)?.relativeMusicPath
                    ?: guessMusicSubPath(vol.rootPath),
            )
            if (idx >= 0) existing[idx] = source else existing.add(source)
        }

        // 已拔出的 USB：保留条目但标记路径失效，由 UI 提示
        existing.replaceAll { src ->
            if (src.kind == LibrarySourceKind.Usb && src.id !in usbIds) {
                src.copy(localRootPath = null) // 拔出
            } else {
                src
            }
        }

        store.save(existing)
        return existing
    }

    private fun guessMusicSubPath(root: String): String {
        val candidates = listOf("music", "Music", "Karaoke", "karaoke", "KTV", "歌曲", "曲库")
        for (name in candidates) {
            val f = File(root, name)
            if (f.isDirectory) return name
        }
        return ""
    }
}

@Suppress("DEPRECATION")
fun StorageVolume.pathCompat(): File? {
    if (Build.VERSION.SDK_INT >= 30) {
        return directory
    }
    return try {
        val m = StorageVolume::class.java.getMethod("getPathFile")
        m.invoke(this) as? File
    } catch (_: Exception) {
        try {
            val m = StorageVolume::class.java.getMethod("getPath")
            val p = m.invoke(this) as? String
            p?.let { File(it) }
        } catch (_: Exception) {
            null
        }
    }
}
