package de.robnice.elly.wear.ui.pages

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.components.RoundPage
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.floatResource

// The TV pointer follows the arm while a finger rests on the display; a short tap clicks and the crown scrolls.
// Moving only while touched keeps the pointer still between aims and the gyroscope off most of the time.
@Composable
fun PointerPage(
    active: Boolean,
    onHold: (Boolean) -> Unit,
    onClick: () -> Unit,
    onScroll: (Int) -> Unit,
    slotKey: SlotKey
) {
    val keySize = floatResource(R.dimen.key_size)
    val keyX = floatResource(R.dimen.nav_key_x)
    val keyY = floatResource(R.dimen.nav_key_y)
    var holding by remember { mutableStateOf(false) }
    val hold by rememberUpdatedState(onHold)
    val click by rememberUpdatedState(onClick)
    val focus = remember { FocusRequester() }
    var scrolled by remember { mutableStateOf(0f) }
    // Only the page in view takes the crown; the pager composes its neighbours as well.
    LaunchedEffect(active) {
        if (active) focus.requestFocus()
    }
    RoundPage {
        Box(
            Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    scrolled += event.verticalScrollPixels
                    val steps = (scrolled / SCROLL_PIXELS_PER_STEP).toInt()
                    if (steps != 0) {
                        scrolled -= steps * SCROLL_PIXELS_PER_STEP
                        onScroll(steps)
                    }
                    true
                }
                .focusRequester(focus)
                .focusable()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        holding = true
                        hold(true)
                        var up = down
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            up = change
                            if (!change.pressed) break
                        }
                        holding = false
                        hold(false)
                        val moved = (up.position - down.position).getDistance()
                        val tap = up.uptimeMillis - down.uptimeMillis < TAP_MILLIS && moved < viewConfiguration.touchSlop
                        if (tap && !up.isConsumed) click()
                    }
                }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.no_tv_spacing)),
            modifier = Modifier
                .at(0f, floatResource(R.dimen.pointer_hint_y))
                .wide(floatResource(R.dimen.message_width))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pointer),
                contentDescription = null,
                tint = if (holding) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.key_icon_size))
            )
            Text(
                text = stringResource(R.string.pointer_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        slotKey(WatchSlot.POINTER_LEFT, false, Modifier.at(-keyX, keyY).sized(keySize))
        slotKey(WatchSlot.POINTER_RIGHT, false, Modifier.at(keyX, keyY).sized(keySize))
    }
}

private const val TAP_MILLIS = 300L
private const val SCROLL_PIXELS_PER_STEP = 40f
