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
import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.remote.RemoteKey

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
fun ScreenThumbnail(layout: CustomScreen, enabled: Boolean, modifier: Modifier = Modifier) {
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
            drawLayout(colors, layout)
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

private fun DrawScope.drawLayout(c: ThumbnailColors, screen: CustomScreen) {
    if (screen.elements.isEmpty()) {
        pill(c.key, 0.5f, 0.5f, 0.36f, 0.035f)
        pill(c.key, 0.5f, 0.5f, 0.07f, 0.18f)
        return
    }
    val cellWidth = size.width / CustomScreen.COLUMNS
    val cellHeight = size.height / CustomScreen.ROWS
    val inset = cellWidth * CUSTOM_ELEMENT_INSET
    screen.elements.forEach { element ->
        val color = when (element.type) {
            CustomElementType.DPAD -> c.keyHighlight
            CustomElementType.APP -> c.light
            CustomElementType.KEY -> when (element.key) {
                RemoteKey.POWER -> c.power
                RemoteKey.RED -> c.red
                RemoteKey.GREEN -> c.green
                RemoteKey.YELLOW -> c.yellow
                RemoteKey.BLUE -> c.blue
                else -> c.key
            }
            else -> c.key
        }
        val width = element.width * cellWidth - 2 * inset
        val height = element.height * cellHeight - 2 * inset
        drawRoundRect(
            color = color,
            topLeft = Offset(element.x * cellWidth + inset, element.y * cellHeight + inset),
            size = Size(width, height),
            cornerRadius = CornerRadius(minOf(width, height) / 2f)
        )
    }
}

private const val CUSTOM_ELEMENT_INSET = 0.12f
