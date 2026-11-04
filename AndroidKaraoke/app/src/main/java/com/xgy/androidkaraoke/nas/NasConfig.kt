package com.xgy.androidkaraoke.nas

import android.content.Context
import com.xgy.androidkaraoke.BrandConfig

enum class NasProtocolKind(val displayName: String) {
    Https("HTTPS"),
    Http("HTTP"),
    WebDav("WebDAV"),
}

data class NasConfig(
    val baseUrlString: String,
    val libraryPath: String,
    val protocolKind: NasProtocolKind = NasProtocolKind.Https,
    val audioExtensions: List<String> = defaultAudioExtensions,
) {
    val normalizedBaseUrl: String
        get() = baseUrlString.trim().trimEnd('/')

    val normalizedPath: String
        get() {
            var p = libraryPath.trim()
            if (p.isEmpty()) p = "/"
            if (!p.startsWith("/")) p = "/$p"
            while (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
            return p
        }

    fun urlForRelativePath(path: String): String? {
        val base = normalizedBaseUrl.ifEmpty { return null }
        val trimmed = path.trimStart('/')
        return "$base/$trimmed"
    }

    companion object {
        val defaultAudioExtensions = listOf(
            "mp3", "m4a", "aac", "flac", "wav", "aiff", "alac",
            "mp4", "m4v", "mov", "mkv",
        )

        val default = NasConfig(
            baseUrlString = BrandConfig.defaultNasBaseUrl,
            libraryPath = BrandConfig.defaultNasLibraryPath,
        )
    }
}

class NasConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): NasConfig {
        val base = prefs.getString(KEY_BASE, null)?.takeIf { it.isNotBlank() }
            ?: BrandConfig.defaultNasBaseUrl
        val path = prefs.getString(KEY_PATH, null)?.takeIf { it.isNotBlank() }
            ?: BrandConfig.defaultNasLibraryPath
        val proto = prefs.getString(KEY_PROTO, null)
            ?.let { runCatching { NasProtocolKind.valueOf(it) }.getOrNull() }
            ?: NasProtocolKind.Https
        return NasConfig(base, path, proto)
    }

    fun save(config: NasConfig) {
        prefs.edit()
            .putString(KEY_BASE, config.normalizedBaseUrl)
            .putString(KEY_PATH, config.normalizedPath)
            .putString(KEY_PROTO, config.protocolKind.name)
            .apply()
    }

    companion object {
        private const val PREFS = "nas_config"
        private const val KEY_BASE = "nas.baseURL"
        private const val KEY_PATH = "nas.libraryPath"
        private const val KEY_PROTO = "nas.protocol"
    }
}
