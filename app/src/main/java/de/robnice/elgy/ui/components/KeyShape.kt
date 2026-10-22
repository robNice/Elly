package de.robnice.elgy.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import de.robnice.elgy.data.model.KeyRounding
import kotlin.math.roundToInt

val LocalKeyRounding = compositionLocalOf { KeyRounding.DEFAULT }

val LocalDpadRounding = compositionLocalOf { KeyRounding.DEFAULT }

fun keyShape(rounding: Float): Shape = RoundedCornerShape(percent = (rounding * 50).roundToInt())

// The halves of a rocker are shorter than wide, so their corners follow the rocker's width.
fun rockerHalfShape(rounding: Float, top: Boolean): Shape {
    val corner = WidthCornerSize(rounding)
    return if (top) {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
    } else {
        RoundedCornerShape(topStart = ZeroCornerSize, topEnd = ZeroCornerSize, bottomEnd = corner, bottomStart = corner)
    }
}

private class WidthCornerSize(private val rounding: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = shapeSize.width * rounding / 2
}
