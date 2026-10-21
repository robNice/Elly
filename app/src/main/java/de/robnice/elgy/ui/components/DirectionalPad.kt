package de.robnice.elgy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import de.robnice.elgy.R
import de.robnice.elgy.remote.RemoteKey

private const val OK_SIZE_FRACTION = 0.42f
private const val ARROW_SIZE_FRACTION = 0.26f
private const val ARROW_OFFSET_FRACTION = 0.355f

@Composable
fun DirectionalPad(onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier, capped: Boolean = true) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val fitted = min(maxWidth, maxHeight)
        val padSize = if (capped) min(fitted, dimensionResource(R.dimen.dpad_max_size)) else fitted
        val ringPalette = KeyStyle.DARK.palette().copy(
            top = colorResource(R.color.dpad_ring_top),
            bottom = colorResource(R.color.dpad_ring_bottom)
        )

        Box(
            modifier = Modifier
                .size(padSize)
                .keyBody(CircleShape, ringPalette, pressed = false),
            contentAlignment = Alignment.Center
        ) {
            val arrowSize = padSize * ARROW_SIZE_FRACTION
            val offset = padSize * ARROW_OFFSET_FRACTION
            PadArrow(R.drawable.ic_chevron_up, R.string.key_up, arrowSize, 0.dp(), -offset, ringPalette.content) { onKey(RemoteKey.UP) }
            PadArrow(R.drawable.ic_chevron_down, R.string.key_down, arrowSize, 0.dp(), offset, ringPalette.content) { onKey(RemoteKey.DOWN) }
            PadArrow(R.drawable.ic_chevron_left, R.string.key_left, arrowSize, -offset, 0.dp(), ringPalette.content) { onKey(RemoteKey.LEFT) }
            PadArrow(R.drawable.ic_chevron_right, R.string.key_right, arrowSize, offset, 0.dp(), ringPalette.content) { onKey(RemoteKey.RIGHT) }

            RemoteKeyButton(
                contentDescription = stringResource(R.string.key_ok),
                onPress = { onKey(RemoteKey.OK) },
                modifier = Modifier.size(padSize * OK_SIZE_FRACTION)
            ) { color ->
                KeyLabel(text = stringResource(R.string.key_ok), color = color, fontSize = 20, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PadArrow(
    @DrawableRes icon: Int,
    description: Int,
    size: Dp,
    x: Dp,
    y: Dp,
    tint: Color,
    onPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(size)
            .keyPress(interactionSource, stringResource(description), repeatOnHold = true, onPress = onPress),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            KeyIcon(icon = icon, tint = if (pressed) tint.copy(alpha = PRESSED_ARROW_ALPHA) else tint)
        }
    }
}

private const val PRESSED_ARROW_ALPHA = 0.5f

private fun Int.dp(): Dp = Dp(toFloat())
