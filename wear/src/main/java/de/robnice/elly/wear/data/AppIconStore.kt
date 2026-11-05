package de.robnice.elly.wear.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

// Icons of the apps that keys on the watch open; they come from the phone together with the layout.
class AppIconStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, DIR)

    fun save(appId: String, bytes: ByteArray) {
        dir.mkdirs()
        file(appId).writeBytes(bytes)
    }

    fun bytes(appId: String): ByteArray? = file(appId).takeIf { it.isFile }?.readBytes()

    fun bitmap(appId: String): Bitmap? = bytes(appId)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    // Icons of apps that no key opens any more are removed.
    fun retain(appIds: Set<String>) {
        val keep = appIds.map(::fileName).toSet()
        dir.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    private fun file(appId: String) = File(dir, fileName(appId))

    // App IDs are reverse domain names, but are not trusted as file names.
    private fun fileName(appId: String) = appId.replace(Regex("[^A-Za-z0-9._-]"), "_") + EXTENSION

    private companion object {
        const val DIR = "app_icons"
        const val EXTENSION = ".png"
    }
}
