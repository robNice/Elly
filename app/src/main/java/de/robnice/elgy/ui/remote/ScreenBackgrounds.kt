package de.robnice.elgy.ui.remote

import android.graphics.ImageDecoder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import de.robnice.elgy.R
import de.robnice.elgy.data.model.AppBackground
import de.robnice.elgy.data.model.ScreenBackground
import de.robnice.elgy.ui.components.currentPaletteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

val LocalAppBackground = compositionLocalOf { AppBackground() }

object ScreenBackgrounds {

    @DrawableRes
    fun image(background: ScreenBackground): Int? = when (background) {
        ScreenBackground.NONE, ScreenBackground.PALETTE, ScreenBackground.CUSTOM -> null
        ScreenBackground.HILLS -> R.drawable.bg_hills
        ScreenBackground.OCEAN -> R.drawable.bg_ocean
        ScreenBackground.MEADOW -> R.drawable.bg_meadow
        ScreenBackground.SKY -> R.drawable.bg_sky
        ScreenBackground.AURORA -> R.drawable.bg_aurora
        ScreenBackground.GRAPHITE -> R.drawable.bg_graphite
        ScreenBackground.MOUNTAINS -> R.drawable.bg_mountains
        ScreenBackground.FOREST -> R.drawable.bg_forest
        ScreenBackground.SEA -> R.drawable.bg_sea
    }

    // Small copies for previews, so the full images are only decoded for the remote and the editor.
    @DrawableRes
    fun thumbnail(background: ScreenBackground): Int? = when (background) {
        ScreenBackground.NONE, ScreenBackground.PALETTE, ScreenBackground.CUSTOM -> null
        ScreenBackground.HILLS -> R.drawable.bg_hills_thumb
        ScreenBackground.OCEAN -> R.drawable.bg_ocean_thumb
        ScreenBackground.MEADOW -> R.drawable.bg_meadow_thumb
        ScreenBackground.SKY -> R.drawable.bg_sky_thumb
        ScreenBackground.AURORA -> R.drawable.bg_aurora_thumb
        ScreenBackground.GRAPHITE -> R.drawable.bg_graphite_thumb
        ScreenBackground.MOUNTAINS -> R.drawable.bg_mountains_thumb
        ScreenBackground.FOREST -> R.drawable.bg_forest_thumb
        ScreenBackground.SEA -> R.drawable.bg_sea_thumb
    }

    @StringRes
    fun title(background: ScreenBackground): Int = when (background) {
        ScreenBackground.NONE -> R.string.background_none
        ScreenBackground.PALETTE -> R.string.background_palette
        ScreenBackground.CUSTOM -> R.string.background_custom
        ScreenBackground.HILLS -> R.string.background_hills
        ScreenBackground.OCEAN -> R.string.background_ocean
        ScreenBackground.MEADOW -> R.string.background_meadow
        ScreenBackground.SKY -> R.string.background_sky
        ScreenBackground.AURORA -> R.string.background_aurora
        ScreenBackground.GRAPHITE -> R.string.background_graphite
        ScreenBackground.MOUNTAINS -> R.string.background_mountains
        ScreenBackground.FOREST -> R.string.background_forest
        ScreenBackground.SEA -> R.string.background_sea
    }
}

// Fills the given area; pictures keep their proportions and are cut off at the sides or at top and bottom.
@Composable
fun ScreenBackgroundImage(
    background: ScreenBackground,
    imagePath: String?,
    modifier: Modifier = Modifier.fillMaxSize(),
    thumbnail: Boolean = false
) {
    when (background) {
        ScreenBackground.NONE -> Unit
        ScreenBackground.PALETTE -> {
            val colors = currentPaletteColors()
            Box(modifier = modifier.background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))))
        }
        ScreenBackground.CUSTOM -> {
            val bitmap = rememberBackgroundBitmap(imagePath, if (thumbnail) THUMBNAIL_SIZE else FULL_SIZE) ?: return
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
        }
        else -> {
            val image = (if (thumbnail) ScreenBackgrounds.thumbnail(background) else ScreenBackgrounds.image(background)) ?: return
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun rememberBackgroundBitmap(path: String?, maxSize: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxSize) {
        value = path?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(it))) { decoder, info, _ ->
                        val scale = maxSize.toFloat() / maxOf(info.size.width, info.size.height)
                        if (scale < 1f) {
                            decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                        }
                    }.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

private const val FULL_SIZE = 2400
private const val THUMBNAIL_SIZE = 480
