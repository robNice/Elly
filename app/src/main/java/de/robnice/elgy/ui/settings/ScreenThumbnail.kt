package de.robnice.elgy.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import de.robnice.elgy.R
import de.robnice.elgy.data.model.RemoteScreen

private const val THUMBNAIL_ASPECT_RATIO = 0.5f
private const val DISABLED_ALPHA = 0.3f

private data class ThumbnailColors(
    val key: Color,
    val keyHighlight: Color,
    val power: Color,
    val light: Color,
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color
)

@Composable
fun ScreenThumbnail(screen: RemoteScreen, enabled: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.screen_thumbnail_corner))
    val colors = ThumbnailColors(
        key = colorResource(R.color.key_body_top),
        keyHighlight = colorResource(R.color.dpad_ring_top),
        power = colorResource(R.color.key_red_top),
        light = colorResource(R.color.key_light_top),
        red = colorResource(R.color.key_color_red),
        green = colorResource(R.color.key_color_green),
        yellow = colorResource(R.color.key_color_yellow),
        blue = colorResource(R.color.key_color_blue)
    )
    Box(
        modifier = modifier
            .aspectRatio(THUMBNAIL_ASPECT_RATIO)
            .background(colorResource(R.color.remote_background), shape)
            .border(dimensionResource(R.dimen.key_rim_width), colorResource(R.color.pointer_area_border), shape)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(dimensionResource(R.dimen.screen_thumbnail_padding))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                RemoteScreen.NAVIGATION -> drawNavigation(colors)
                RemoteScreen.NUMBERS -> drawNumbers(colors)
                RemoteScreen.MEDIA -> drawMedia(colors)
            }
        }
    }
}

private fun DrawScope.circle(color: Color, x: Float, y: Float, radius: Float) {
    drawCircle(color, radius * size.width, Offset(x * size.width, y * size.height))
}

private fun DrawScope.pill(color: Color, x: Float, y: Float, width: Float, height: Float) {
    val w = width * size.width
    val h = height * size.height
    drawRoundRect(
        color = color,
        topLeft = Offset(x * size.width - w / 2f, y * size.height - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(minOf(w, h) / 2f)
    )
}

private fun DrawScope.drawNavigation(c: ThumbnailColors) {
    listOf(0.14f, 0.38f, 0.62f, 0.86f).forEachIndexed { index, x ->
        circle(if (index == 0) c.power else c.key, x, 0.08f, 0.09f)
    }
    pill(c.key, 0.2f, 0.28f, 0.2f, 0.2f)
    circle(c.key, 0.5f, 0.28f, 0.08f)
    pill(c.key, 0.8f, 0.28f, 0.2f, 0.2f)
    circle(c.keyHighlight, 0.5f, 0.6f, 0.4f)
    circle(c.key, 0.5f, 0.6f, 0.16f)
    circle(c.key, 0.32f, 0.9f, 0.09f)
    circle(c.key, 0.68f, 0.9f, 0.09f)
}

private fun DrawScope.drawNumbers(c: ThumbnailColors) {
    circle(c.key, 0.32f, 0.08f, 0.09f)
    circle(c.key, 0.68f, 0.08f, 0.09f)
    listOf(0.24f, 0.38f, 0.52f, 0.66f).forEach { y ->
        listOf(0.17f, 0.5f, 0.83f).forEach { x -> pill(c.key, x, y, 0.28f, 0.07f) }
    }
    circle(c.key, 0.32f, 0.86f, 0.09f)
    circle(c.key, 0.68f, 0.86f, 0.09f)
}

private fun DrawScope.drawMedia(c: ThumbnailColors) {
    listOf(0.17f, 0.39f, 0.61f, 0.83f).forEach { x -> circle(c.key, x, 0.07f, 0.08f) }
    circle(c.keyHighlight, 0.5f, 0.33f, 0.37f)
    circle(c.key, 0.5f, 0.33f, 0.15f)
    circle(c.key, 0.5f, 0.55f, 0.08f)
    listOf(c.red to 0.14f, c.green to 0.38f, c.yellow to 0.62f, c.blue to 0.86f).forEach { (color, x) ->
        pill(color, x, 0.72f, 0.18f, 0.035f)
    }
    listOf(0.82f, 0.92f).forEach { y ->
        listOf(0.17f, 0.5f, 0.83f).forEach { x -> pill(c.light, x, y, 0.28f, 0.06f) }
    }
}
