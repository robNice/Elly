package de.robnice.elgy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.robnice.elgy.R

private const val PRESSED_SCALE = 0.96f
private const val HIGHLIGHT_RADIUS = 0.75f

@Composable
fun RemoteKeyButton(
    contentDescription: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    style: KeyStyle = KeyStyle.DARK,
    repeatOnHold: Boolean = false,
    content: @Composable (contentColor: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val palette = style.palette()

    Box(
        modifier = modifier
            .scale(if (pressed) PRESSED_SCALE else 1f)
            .keyBody(shape, palette, pressed)
            .keyPress(interactionSource, contentDescription, repeatOnHold, onPress),
        contentAlignment = Alignment.Center
    ) {
        content(palette.content)
    }
}

@Composable
fun Modifier.keyBody(shape: Shape, palette: KeyPalette, pressed: Boolean): Modifier {
    val elevation = dimensionResource(if (pressed) R.dimen.key_elevation_pressed else R.dimen.key_elevation)
    val rimWidth = dimensionResource(R.dimen.key_rim_width)
    // With the light effect the keys get their shadow from the light instead of all around.
    val lit = LocalLightEffect.current
    return this
        .then(if (lit) Modifier else Modifier.shadow(elevation, shape, clip = false))
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline,
                Brush.verticalGradient(
                    if (pressed) listOf(palette.bottom, palette.top) else listOf(palette.top, palette.bottom)
                )
            )
            if (pressed) {
                drawOutline(outline, palette.pressedOverlay)
            } else {
                drawOutline(
                    outline,
                    Brush.radialGradient(
                        colors = listOf(palette.highlight, Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = maxOf(size.width, size.height) * HIGHLIGHT_RADIUS
                    )
                )
            }
        }
        .border(
            width = rimWidth,
            brush = Brush.verticalGradient(listOf(palette.rimLight, palette.rimDark)),
            shape = shape
        )
}

@Composable
fun KeyIcon(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(dimensionResource(R.dimen.key_icon_size))
    )
}

@Composable
fun KeyLabel(text: String, color: Color, fontSize: Int = 15, fontWeight: FontWeight = FontWeight.SemiBold) {
    Text(
        text = text,
        color = color,
        maxLines = 1,
        style = TextStyle(fontSize = fontSize.sp, fontWeight = fontWeight)
    )
}
