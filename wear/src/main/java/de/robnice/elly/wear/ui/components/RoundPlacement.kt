package de.robnice.elly.wear.ui.components

import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min

// Lays out a page of the round display; children are placed relative to the centre in fractions of the diameter.
@Composable
fun RoundPage(modifier: Modifier = Modifier, content: @Composable RoundPageScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        RoundPageScope(this, min(maxWidth, maxHeight)).content()
    }
}

class RoundPageScope(private val box: BoxWithConstraintsScope, val diameter: Dp) {

    // Absolute, so the remote keeps its layout in right-to-left languages.
    fun Modifier.at(x: Float, y: Float): Modifier = with(box) {
        align(Alignment.Center).absoluteOffset(diameter * x, diameter * y)
    }

    fun Modifier.sized(fraction: Float): Modifier = size(diameter * fraction)

    fun Modifier.wide(fraction: Float): Modifier = width(diameter * fraction)
}

@Composable
fun floatResource(@DimenRes id: Int): Float = LocalResources.current.getFloat(id)
