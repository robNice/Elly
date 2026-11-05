package de.robnice.elly.wear.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import de.robnice.elly.wear.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Light along the edge of the round display, seen from the corner of the eye while looking at the TV:
// a pulse when a command went out, a running arc while the TV takes its time, and a segment under the
// finger or at the crown while the volume changes. Angles are in degrees, 0 at three o'clock, clockwise.
@Stable
class LightRingState(private val scope: CoroutineScope, private val animated: Boolean) {

    val pulse = Animatable(0f)
    var pulseColor by mutableStateOf(Color.Transparent)
        private set

    var busy by mutableStateOf(false)

    // The running arc goes on in the direction of the last volume step and starts where it ended, so it reads
    // as the gesture going on while the TV catches up.
    var busyClockwise by mutableStateOf(true)
        private set
    var busyStart by mutableStateOf(BUSY_DEFAULT_START)
        private set

    val segment = Animatable(0f)
    var segmentAngle by mutableStateOf(0f)
        private set

    fun pulse(color: Color) {
        pulseColor = color
        scope.launch {
            pulse.snapTo(1f)
            if (animated) pulse.animateTo(0f, tween(PULSE_MILLIS, easing = FastOutSlowInEasing)) else hold(pulse)
        }
    }

    // The finger moves along the ring; null when it was lifted.
    fun track(angle: Float?) {
        scope.launch {
            if (angle == null) {
                fade(segment)
            } else {
                segmentAngle = angle
                if (segment.value < SEGMENT_TRACK) segment.snapTo(SEGMENT_TRACK)
            }
        }
    }

    fun step(louder: Boolean) {
        followGesture(louder)
        scope.launch {
            segment.snapTo(1f)
            if (animated) segment.animateTo(SEGMENT_TRACK, tween(STEP_MILLIS))
        }
    }

    // The crown sits at three o'clock; the segment leans up for louder and down for quieter.
    fun crownStep(louder: Boolean) {
        segmentAngle = if (louder) -CROWN_LEAN_DEGREES else CROWN_LEAN_DEGREES
        followGesture(louder)
        scope.launch {
            segment.snapTo(1f)
            fade(segment)
        }
    }

    // Other keys keep the last direction; only a new volume step turns it.
    private fun followGesture(louder: Boolean) {
        busyClockwise = louder
        busyStart = segmentAngle
    }

    private suspend fun fade(value: Animatable<Float, *>) {
        if (animated) value.animateTo(0f, tween(FADE_MILLIS)) else hold(value)
    }

    private suspend fun hold(value: Animatable<Float, *>) {
        delay(STATIC_MILLIS)
        value.snapTo(0f)
    }
}

@Composable
fun rememberLightRingState(): LightRingState {
    val scope = rememberCoroutineScope()
    return remember { LightRingState(scope, ValueAnimator.areAnimatorsEnabled()) }
}

@Composable
fun LightRing(state: LightRingState, color: Color, modifier: Modifier = Modifier) {
    val width = dimensionResource(R.dimen.light_ring_width)
    val glow = dimensionResource(R.dimen.light_ring_glow)
    val turn = if (state.busy && ValueAnimator.areAnimatorsEnabled()) {
        rememberInfiniteTransition(label = "busyRing").animateFloat(
            initialValue = 0f,
            targetValue = FULL_CIRCLE,
            animationSpec = infiniteRepeatable(tween(BUSY_ROUND_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "busyRing"
        ).value
    } else {
        0f
    }
    val head = state.busyStart + if (state.busyClockwise) turn else -turn
    // The arc trails behind its head, so it lies on the side it comes from.
    val rotation = if (state.busyClockwise) head - BUSY_SWEEP_DEGREES else head
    Canvas(modifier.fillMaxSize()) {
        val pulse = state.pulse.value
        if (pulse > 0f) {
            ring(state.pulseColor.copy(alpha = GLOW_ALPHA * pulse), glow.toPx(), 0f, FULL_CIRCLE)
            ring(state.pulseColor.copy(alpha = pulse), width.toPx(), 0f, FULL_CIRCLE)
        }
        if (state.busy) ring(color.copy(alpha = BUSY_ALPHA), width.toPx(), rotation, BUSY_SWEEP_DEGREES)
        val segment = state.segment.value
        if (segment > 0f) {
            val start = state.segmentAngle - SEGMENT_SWEEP_DEGREES / 2
            ring(color.copy(alpha = GLOW_ALPHA * segment), glow.toPx(), start, SEGMENT_SWEEP_DEGREES)
            ring(color.copy(alpha = segment), width.toPx(), start, SEGMENT_SWEEP_DEGREES)
        }
    }
}

private fun DrawScope.ring(color: Color, strokeWidth: Float, startAngle: Float, sweep: Float) {
    val inset = strokeWidth / 2
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        style = Stroke(strokeWidth, cap = if (sweep < FULL_CIRCLE) StrokeCap.Round else StrokeCap.Butt)
    )
}

private const val FULL_CIRCLE = 360f
private const val PULSE_MILLIS = 600
private const val STEP_MILLIS = 250
private const val FADE_MILLIS = 400
private const val STATIC_MILLIS = 300L
private const val BUSY_ROUND_MILLIS = 1_200
private const val BUSY_SWEEP_DEGREES = 70f
private const val BUSY_DEFAULT_START = -90f
private const val BUSY_ALPHA = 0.8f
private const val SEGMENT_SWEEP_DEGREES = 36f
private const val SEGMENT_TRACK = 0.5f
private const val CROWN_LEAN_DEGREES = 18f
private const val GLOW_ALPHA = 0.25f
