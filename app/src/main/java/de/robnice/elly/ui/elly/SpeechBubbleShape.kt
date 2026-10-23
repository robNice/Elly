package de.robnice.elly.ui.elly

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

enum class TailSide { TOP, START }

// The tail points to Elly: up from the top center, or left from the upper start edge.
class SpeechBubbleShape(
    private val cornerRadius: Dp,
    private val tailSize: Dp,
    private val tailSide: TailSide
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val corner = with(density) { cornerRadius.toPx() }
        val tail = with(density) { tailSize.toPx() }
        val path = Path()
        when (tailSide) {
            TailSide.TOP -> {
                path.addRoundRect(RoundRect(0f, tail, size.width, size.height, CornerRadius(corner)))
                path.moveTo(size.width / 2 - tail, tail + 1f)
                path.lineTo(size.width / 2, 0f)
                path.lineTo(size.width / 2 + tail, tail + 1f)
            }
            TailSide.START -> {
                val tipY = corner + tail
                path.addRoundRect(RoundRect(tail, 0f, size.width, size.height, CornerRadius(corner)))
                path.moveTo(tail + 1f, tipY - tail)
                path.lineTo(0f, tipY)
                path.lineTo(tail + 1f, tipY + tail)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
