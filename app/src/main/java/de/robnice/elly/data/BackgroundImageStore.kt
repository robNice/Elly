package de.robnice.elly.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.roundToInt

// Copies of the pictures chosen as screen backgrounds, scaled down to screen size.
class BackgroundImageStore(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    suspend fun import(uri: Uri): String? = save(ImageDecoder.createSource(resolver, uri))

    // Pictures from a backup file go through the same decoding, so only real images are kept.
    suspend fun import(bytes: ByteArray): String? = save(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))

    suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(path).readBytes() }.getOrNull()
    }

    private suspend fun save(source: ImageDecoder.Source): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
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
