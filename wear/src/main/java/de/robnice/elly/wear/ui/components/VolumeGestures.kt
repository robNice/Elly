package de.robnice.elly.wear.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

// Turning the crown or bezel clockwise raises the volume; onStep(true) means one step louder.
// The crown only reaches the focused element, so the caller has to request focus through focusRequester.
// onSpeed gets how fast it turns, in volume steps per second.
fun Modifier.crownVolume(focusRequester: FocusRequester, onSpeed: (Float) -> Unit, onStep: (Boolean) -> Unit): Modifier {
    val steps = StepCounter(CROWN_STEP_PIXELS, onStep)
    val speed = SpeedMeter(CROWN_STEP_PIXELS)
    return onRotaryScrollEvent { event ->
        onSpeed(speed.add(event.verticalScrollPixels, event.uptimeMillis))
        steps.add(event.verticalScrollPixels)
        true
    }
        .focusRequester(focusRequester)
        .focusable()
}

// Circling along the edge of the display changes the volume, clockwise louder. The ring starts at a share of the
// radius rather than a fixed width, because a bumper case or a raised bezel keeps the finger off the very edge.
// A touch in the ring that first moves along the edge is taken in the initial pass, before the pager sees it.
// One that moves towards the centre is left alone, so paging and swipe to dismiss from the left edge keep working.
// onTrack follows the finger's angle while circling and gets null when it is lifted; onSpeed gets how fast the
// finger circles, in volume steps per second.
fun Modifier.ringVolume(
    innerRadius: Float,
    onTrack: (Float?) -> Unit,
    onSpeed: (Float) -> Unit,
    onStep: (Boolean) -> Unit
): Modifier = pointerInput(innerRadius) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = min(size.width, size.height) / 2f
        val fromCenter = down.position - center
        if (fromCenter.getDistance() < outer * innerRadius) return@awaitEachGesture
        val radial = fromCenter / fromCenter.getDistance()
        val steps = StepCounter(RING_STEP_DEGREES, onStep)
        val speed = SpeedMeter(RING_STEP_DEGREES)
        var lastAngle = angle(fromCenter)
        var circling = false
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            if (!circling) {
                if (!change.pressed) break
                val move = change.position - down.position
                if (move.getDistance() < viewConfiguration.touchSlop) continue
                val along = abs(move.x * radial.y - move.y * radial.x)
                val across = abs(move.x * radial.x + move.y * radial.y)
                if (across > along) return@awaitEachGesture
                circling = true
            }
            change.consume()
            if (!change.pressed) break
            val current = angle(change.position - center)
            onTrack(current)
            val turned = normalized(current - lastAngle)
            onSpeed(speed.add(turned, change.uptimeMillis))
            steps.add(turned)
            lastAngle = current
        }
        if (circling) onTrack(null)
    }
}

private class StepCounter(private val step: Float, private val onStep: (Boolean) -> Unit) {

    private var pending = 0f

    fun add(amount: Float) {
        pending += amount
        while (pending >= step) {
            onStep(true)
            pending -= step
        }
        while (pending <= -step) {
            onStep(false)
            pending += step
        }
    }
}

// The first movement after a pause counts as slow, so the time since the last one is capped.
private class SpeedMeter(private val step: Float) {

    private var lastTime = 0L

    fun add(amount: Float, time: Long): Float {
        val elapsed = (time - lastTime).coerceIn(MIN_ELAPSED_MILLIS, MAX_ELAPSED_MILLIS)
        lastTime = time
        return abs(amount) / step * 1_000f / elapsed
    }
}

private fun angle(offset: Offset): Float = Math.toDegrees(atan2(offset.y, offset.x).toDouble()).toFloat()

private fun normalized(degrees: Float): Float = when {
    degrees > 180f -> degrees - 360f
    degrees < -180f -> degrees + 360f
    else -> degrees
}

private const val CROWN_STEP_PIXELS = 60f
private const val RING_STEP_DEGREES = 20f
private const val MIN_ELAPSED_MILLIS = 4L
private const val MAX_ELAPSED_MILLIS = 150L
