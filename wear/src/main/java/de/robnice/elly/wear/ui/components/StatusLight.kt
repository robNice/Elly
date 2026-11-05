package de.robnice.elly.wear.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.wear.R

@Composable
fun ConnectionState.color(): Color = colorResource(
    when (this) {
        ConnectionState.CONNECTED -> R.color.status_connected
        ConnectionState.DISCONNECTED -> R.color.status_disconnected
        ConnectionState.CHECKING -> R.color.status_checking
        ConnectionState.DEMO -> R.color.status_demo
    }
)

// Same light as on the phone: the colour shows the connection, and it flashes each time a command goes out.
// sentCount only has to change for a flash.
@Composable
fun StatusLight(state: ConnectionState, sentCount: Int, modifier: Modifier = Modifier) {
    val color = state.color()
    val description = stringResource(
        when (state) {
            ConnectionState.CONNECTED -> R.string.status_connected
            ConnectionState.DISCONNECTED -> R.string.status_disconnected
            ConnectionState.CHECKING -> R.string.status_checking
            ConnectionState.DEMO -> R.string.status_demo
        }
    )
    val flash = remember { Animatable(0f) }
    LaunchedEffect(sentCount) {
        if (sentCount == 0) return@LaunchedEffect
        flash.snapTo(1f)
        flash.animateTo(0f, tween(FLASH_MILLIS, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier
            .size(dimensionResource(R.dimen.status_light_size))
            .semantics { contentDescription = description }
            .drawBehind {
                val glow = flash.value
                val radius = size.minDimension / 2
                if (glow > 0f) drawCircle(color.copy(alpha = FLASH_ALPHA * glow), radius * (1f + FLASH_GROWTH * glow))
                drawCircle(color.copy(alpha = GLOW_ALPHA), radius)
                drawCircle(color, radius * CORE_RATIO)
            }
    )
}

private const val FLASH_MILLIS = 700
private const val FLASH_ALPHA = 0.6f
private const val FLASH_GROWTH = 0.6f
private const val GLOW_ALPHA = 0.3f
private const val CORE_RATIO = 0.6f
