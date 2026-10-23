package de.robnice.elly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import de.robnice.elly.R
import de.robnice.elly.data.model.CellShape
import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.ElementOutline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// A low light source above the top left corner of the phone. Keys cast long shadows
// towards the bottom right and the background is lit from that corner.
val LocalLightEffect = compositionLocalOf { false }

// Drawn below all elements, so no shadow falls onto a key. Each shadow is the area the key sweeps
// along the light direction, filled with one gradient that fades out; the layer is blurred so the
// sides fade out too.
@Composable
fun ShadowLayer(elements: List<CustomElement>, cells: CellShape, cellHeight: Dp) {
    if (!LocalLightEffect.current) return
    val color = colorResource(R.color.light_shadow)
    val softness = dimensionResource(R.dimen.light_shadow_softness)
    val before = dimensionResource(R.dimen.light_shadow_margin_before)
    val after = dimensionResource(R.dimen.light_shadow_margin_after)
    // The blurred layer cuts off at its own bounds, so it reaches beyond the grid, mostly to the
    // right and bottom where the shadows fall.
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val start = before.roundToPx()
                val extra = start + after.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth + extra, constraints.maxHeight + extra))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-start, -start) }
            }
            // Soft light darkens mid tones most and bright areas only a little, so shadows on a
            // light sky do not look like dirt.
            .graphicsLayer { blendMode = BlendMode.Softlight }
            .blur(softness, BlurredEdgeTreatment.Unbounded)
    ) {
        val unit = cellHeight.toPx()
        val start = before.toPx()
        translate(start, start) {
            elements.forEach { element ->
                val outline = element.outline(cells)
                drawSweptShadow(
                    ElementOutline(outline.left * unit, outline.top * unit, outline.right * unit, outline.bottom * unit, outline.radius * unit),
                    SHADOW_LENGTH_PER_HEIGHT,
                    color
                )
            }
        }
    }
}

// The OK key stands a little above the ring of the directional pad, so its shadow is short and stays on the ring.
@Composable
fun OkKeyShadow(okSize: Dp, rounding: Float, ringShape: Shape) {
    if (!LocalLightEffect.current) return
    val color = colorResource(R.color.light_ok_shadow)
    Canvas(modifier = Modifier.fillMaxSize().clip(ringShape)) {
        val half = okSize.toPx() / 2
        drawSweptShadow(
            ElementOutline(center.x - half, center.y - half, center.x + half, center.y + half, rounding * half),
            OK_SHADOW_LENGTH_PER_HEIGHT,
            color
        )
    }
}

// The area a rounded rectangle (in pixels) sweeps along the light. Taller elements throw longer
// shadows, so flat ones fade out sooner; the fade eases out instead of running linearly.
private fun DrawScope.drawSweptShadow(outline: ElementOutline, lengthPerHeight: Float, color: Color) {
    val reach = (outline.bottom - outline.top) * lengthPerHeight
    val shift = Offset(reach * SHADOW_DIRECTION_X, reach * SHADOW_DIRECTION_Y)
    val points = outlinePoints(outline)
    val hull = convexHull(points + points.map { it + shift })
    val path = Path().apply {
        moveTo(hull.first().x, hull.first().y)
        hull.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    val centerX = (outline.left + outline.right) / 2
    val centerY = (outline.top + outline.bottom) / 2
    val fade = SHADOW_FADE.map { (position, strength) -> position to color.copy(alpha = color.alpha * strength) }.toTypedArray()
    // Lengthwise the shadow is darkest right below the key and fades with the distance below it;
    // across it is strongest in the middle and fades out towards both sides.
    val across = Offset(SHADOW_DIRECTION_Y, -SHADOW_DIRECTION_X)
    val spread = hull.map { (it.x - centerX) * across.x + (it.y - centerY) * across.y }
    val center = Offset(centerX, centerY)
    val sides = Brush.linearGradient(
        *SHADOW_SIDES,
        start = center + across * spread.min(),
        end = center + across * spread.max()
    )
    val bounds = Rect(hull.minOf { it.x }, hull.minOf { it.y }, hull.maxOf { it.x }, hull.maxOf { it.y })
    drawContext.canvas.saveLayer(bounds, Paint())
    drawPath(path, Brush.linearGradient(*fade, start = Offset(centerX, outline.bottom), end = Offset(centerX, outline.bottom + shift.y)))
    drawPath(path, sides, blendMode = BlendMode.DstIn)
    drawContext.canvas.restore()
}

@Composable
fun LightOverlay(modifier: Modifier = Modifier.fillMaxSize()) {
    if (!LocalLightEffect.current) return
    val glow = colorResource(R.color.light_glow)
    Canvas(modifier = modifier) {
        val radius = max(size.width, size.height) * GLOW_RADIUS
        val center = Offset(-size.width * GLOW_OUTSIDE, -size.height * GLOW_OUTSIDE)
        drawRect(Brush.radialGradient(listOf(glow, Color.Transparent), center, radius))
    }
}

private fun outlinePoints(outline: ElementOutline): List<Offset> {
    val r = outline.radius
    val corners = listOf(
        Offset(outline.right - r, outline.top + r) to -PI / 2,
        Offset(outline.right - r, outline.bottom - r) to 0.0,
        Offset(outline.left + r, outline.bottom - r) to PI / 2,
        Offset(outline.left + r, outline.top + r) to PI
    )
    return corners.flatMap { (center, start) ->
        (0..CORNER_POINTS).map { i ->
            val angle = start + PI / 2 * i / CORNER_POINTS
            Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
        }
    }
}

// Andrew's monotone chain.
private fun convexHull(points: List<Offset>): List<Offset> {
    val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))
    fun cross(o: Offset, a: Offset, b: Offset) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    fun half(input: List<Offset>): List<Offset> {
        val chain = mutableListOf<Offset>()
        input.forEach { point ->
            while (chain.size >= 2 && cross(chain[chain.size - 2], chain.last(), point) <= 0f) chain.removeAt(chain.lastIndex)
            chain.add(point)
        }
        return chain.dropLast(1)
    }
    return half(sorted) + half(sorted.reversed())
}

private const val CORNER_POINTS = 8
private val SHADOW_SIDES = arrayOf(
    0f to Color.Black.copy(alpha = 0.35f),
    0.3f to Color.Black.copy(alpha = 0.95f),
    0.5f to Color.Black,
    0.7f to Color.Black.copy(alpha = 0.95f),
    1f to Color.Black.copy(alpha = 0.35f)
)
private const val SHADOW_LENGTH_PER_HEIGHT = 4f
private const val OK_SHADOW_LENGTH_PER_HEIGHT = 0.35f
private val SHADOW_FADE = listOf(0f to 1f, 0.2f to 0.5f, 0.45f to 0.2f, 0.7f to 0.06f, 1f to 0f)
private const val SHADOW_DIRECTION_X = 0.42f
private const val SHADOW_DIRECTION_Y = 0.91f
private const val GLOW_RADIUS = 1.15f
private const val GLOW_OUTSIDE = 0.08f
