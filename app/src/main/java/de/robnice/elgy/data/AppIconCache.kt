package de.robnice.elgy.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppIconCache(context: Context) {

    private val root = File(context.applicationContext.filesDir, ICON_DIR)

    suspend fun existing(owner: String, appId: String): String? = withContext(Dispatchers.IO) {
        fileFor(owner, appId).takeIf { it.isFile && it.length() > 0 }?.absolutePath
    }

    suspend fun store(owner: String, appId: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(owner, appId)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    suspend fun removeOwner(owner: String) = withContext(Dispatchers.IO) {
        File(root, safeName(owner)).deleteRecursively()
    }

    private fun fileFor(owner: String, appId: String): File =
        File(File(root, safeName(owner)), safeName(appId) + ICON_SUFFIX)

    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val ICON_DIR = "app_icons"
        const val ICON_SUFFIX = ".img"
    }
}
