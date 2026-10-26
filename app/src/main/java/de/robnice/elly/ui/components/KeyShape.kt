package de.robnice.elly.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import de.robnice.elly.data.model.KeyRounding
import kotlin.math.roundToInt

val LocalKeyRounding = compositionLocalOf { KeyRounding.DEFAULT }

val LocalDpadRounding = compositionLocalOf { KeyRounding.DEFAULT }

fun keyShape(rounding: Float): Shape = RoundedCornerShape(percent = (rounding * 50).roundToInt())

// The halves of a rocker are shorter than the rocker is wide (or, lying, high), so their corners follow
// the rocker's narrow side. first is the top half, or the left one of a horizontal rocker.
fun rockerHalfShape(rounding: Float, first: Boolean, horizontal: Boolean = false): Shape {
    val corner = NarrowSideCornerSize(rounding, horizontal)
    return when {
        horizontal && first -> RoundedCornerShape(topStart = corner, topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize, bottomStart = corner)
        horizontal -> RoundedCornerShape(topStart = ZeroCornerSize, topEnd = corner, bottomEnd = corner, bottomStart = ZeroCornerSize)
        first -> RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
        else -> RoundedCornerShape(topStart = ZeroCornerSize, topEnd = ZeroCornerSize, bottomEnd = corner, bottomStart = corner)
    }
}

private class NarrowSideCornerSize(private val rounding: Float, private val horizontal: Boolean) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        (if (horizontal) shapeSize.height else shapeSize.width) * rounding / 2
}
