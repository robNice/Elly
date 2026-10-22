package de.robnice.elgy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

// Copies of the pictures chosen as screen backgrounds, scaled down to screen size.
class BackgroundImageStore(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIZE.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            directory.mkdirs()
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    suspend fun deleteUnused(used: Set<String>) = withContext(Dispatchers.IO) {
        directory.listFiles()?.filter { it.absolutePath !in used }?.forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY = "backgrounds"
        const val MAX_SIZE = 2400
        const val QUALITY = 88
    }
}
