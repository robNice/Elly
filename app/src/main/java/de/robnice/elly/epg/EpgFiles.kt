package de.robnice.elly.epg

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// Guide data and channel logos on the phone, one folder per TV (or the demo).
class EpgFiles(context: Context) {

    private val root = File(context.applicationContext.filesDir, ROOT_DIR)

    suspend fun load(owner: String): EpgCache = withContext(Dispatchers.IO) {
        val file = cacheFile(owner)
        if (!file.isFile) return@withContext EpgCache()
        runCatching {
            GZIPInputStream(file.inputStream()).bufferedReader(Charsets.UTF_8).use { EpgCacheJson.decode(it.readText()) }
        }.getOrDefault(EpgCache())
    }

    // Written to a temporary file first, so an interrupted save never leaves a broken cache.
    suspend fun save(owner: String, cache: EpgCache) = withContext(Dispatchers.IO) {
        val file = cacheFile(owner)
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + TEMP_SUFFIX)
        runCatching {
            GZIPOutputStream(temp.outputStream()).bufferedWriter(Charsets.UTF_8).use { it.write(EpgCacheJson.encode(cache)) }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    suspend fun logos(owner: String, channels: List<EpgChannel>): Map<String, String> = withContext(Dispatchers.IO) {
        channels.mapNotNull { channel ->
            logoFile(owner, channel.id).takeIf { it.isFile && it.length() > 0 }?.let { channel.id to it.absolutePath }
        }.toMap()
    }

    suspend fun storeLogo(owner: String, channelId: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            val file = logoFile(owner, channelId)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
    }

    suspend fun removeOwner(owner: String) = withContext(Dispatchers.IO) {
        File(root, safeName(owner)).deleteRecursively()
    }

    private fun cacheFile(owner: String) = File(File(root, safeName(owner)), CACHE_FILE)

    private fun logoFile(owner: String, channelId: String) =
        File(File(File(root, safeName(owner)), LOGO_DIR), safeName(channelId) + LOGO_SUFFIX)

    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val ROOT_DIR = "epg"
        const val CACHE_FILE = "guide.json.gz"
        const val TEMP_SUFFIX = ".tmp"
        const val LOGO_DIR = "logos"
        const val LOGO_SUFFIX = ".img"
    }
}
