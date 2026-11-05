package de.robnice.elly.wear.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.wear.R
import kotlin.math.abs

// Taps anywhere on the page count for the pad: the segment the tap lies in, measured from the pad's centre, so the
// touch targets reach to the edge of the display and stay large on small watches. Keys placed on top of this
// layer take their taps first.
fun Modifier.directionTaps(centerY: () -> Float, centerRadius: () -> Float, onKey: (RemoteKey) -> Unit): Modifier =
    pointerInput(Unit) {
        detectTapGestures { position ->
            val offset = position - Offset(size.width / 2f, size.height / 2f + centerY())
            onKey(keyAt(offset, centerRadius()))
        }
    }

// The visible round pad: four segments and OK in the middle. Each direction also gets a 48 dp node for
// accessibility services; the taps themselves come from directionTaps.
@Composable
fun DirectionalPad(centerSize: Dp, onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    val gap = dimensionResource(R.dimen.dpad_gap)
    val iconSize = dimensionResource(R.dimen.dpad_icon_size)
    val iconInset = dimensionResource(R.dimen.dpad_icon_inset)
    val segmentColor = MaterialTheme.colorScheme.surfaceContainer
    val centerColor = MaterialTheme.colorScheme.primaryContainer
    val dividerColor = MaterialTheme.colorScheme.background

    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val iconDistance = maxWidth / 2 - iconInset - iconSize / 2
        Canvas(Modifier.fillMaxSize()) {
            val middle = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f
            val reach = outer * DIAGONAL
            val center = centerSize.toPx() / 2
            drawCircle(segmentColor, outer, middle)
            drawLine(dividerColor, middle + Offset(-reach, -reach), middle + Offset(reach, reach), gap.toPx())
            drawLine(dividerColor, middle + Offset(-reach, reach), middle + Offset(reach, -reach), gap.toPx())
            drawCircle(dividerColor, center + gap.toPx(), middle)
            drawCircle(centerColor, center, middle)
            drawCircle(dividerColor, outer, middle, style = Stroke(gap.toPx()))
        }
        Direction.entries.forEach { direction ->
            val label = stringResource(direction.label)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .absoluteOffset(iconDistance * direction.dx, iconDistance * direction.dy)
                    .size(ACCESSIBLE_TARGET)
                    .semantics {
                        role = Role.Button
                        contentDescription = label
                        onClick { onKey(direction.key); true }
                    }
            ) {
                Icon(
                    painter = painterResource(direction.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        val okLabel = stringResource(R.string.key_ok)
        Text(
            text = okLabel,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(centerSize)
                .semantics {
                    role = Role.Button
                    contentDescription = okLabel
                    onClick { onKey(RemoteKey.OK); true }
                }
        )
    }
}

private fun keyAt(offset: Offset, centerRadius: Float): RemoteKey = when {
    offset.getDistance() <= centerRadius -> RemoteKey.OK
    abs(offset.x) > abs(offset.y) -> if (offset.x > 0) RemoteKey.RIGHT else RemoteKey.LEFT
    else -> if (offset.y > 0) RemoteKey.DOWN else RemoteKey.UP
}

private enum class Direction(val key: RemoteKey, val icon: Int, val label: Int, val dx: Float, val dy: Float) {
    UP(RemoteKey.UP, R.drawable.ic_chevron_up, R.string.key_up, 0f, -1f),
    DOWN(RemoteKey.DOWN, R.drawable.ic_chevron_down, R.string.key_down, 0f, 1f),
    LEFT(RemoteKey.LEFT, R.drawable.ic_chevron_left, R.string.key_left, -1f, 0f),
    RIGHT(RemoteKey.RIGHT, R.drawable.ic_chevron_right, R.string.key_right, 1f, 0f)
}

private val ACCESSIBLE_TARGET = 48.dp
private const val DIAGONAL = 0.7071f
