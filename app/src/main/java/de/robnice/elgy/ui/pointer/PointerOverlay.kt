package de.robnice.elgy.ui.pointer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleStartEffect
import de.robnice.elgy.R
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.TouchStyle
import de.robnice.elgy.sensor.MotionPointerSensor
import de.robnice.elgy.ui.components.KeyIcon
import de.robnice.elgy.ui.components.KeyLabel
import de.robnice.elgy.ui.components.RemoteKeyButton
import de.robnice.elgy.ui.components.rememberKeyHaptic
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val INACTIVITY_TIMEOUT_MILLIS = 5_000L
private const val TOUCH_GAIN = 1.6f
private const val SCROLL_PIXELS_PER_STEP = 40f
private const val BACKGROUND_PAD_ALPHA = 0.06f
private const val MOTION_ACTIVITY_THRESHOLD = 0.5f
private const val NANOS_PER_SECOND = 1_000_000_000f

@Composable
fun PointerOverlay(
    settings: PointerSettings,
    onMove: (Float, Float) -> Unit,
    onScroll: (Int) -> Unit,
    onOk: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastActivity by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val markActivity = { lastActivity = SystemClock.elapsedRealtime() }
    val click = {
        markActivity()
        onOk()
    }
    val currentOnClose by rememberUpdatedState(onClose)

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        while (true) {
            val remaining = lastActivity + INACTIVITY_TIMEOUT_MILLIS - SystemClock.elapsedRealtime()
            if (remaining <= 0) {
                currentOnClose()
                break
            }
            delay(remaining)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.pointer_scrim))
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(dimensionResource(R.dimen.pointer_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.pointer_padding))
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RemoteKeyButton(
                contentDescription = stringResource(R.string.pointer_close),
                onPress = onClose,
                modifier = Modifier.size(dimensionResource(R.dimen.key_size_small))
            ) { color -> KeyIcon(icon = R.drawable.ic_close, tint = color) }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (settings.mode) {
                PointerMode.MOTION -> MotionLayout(settings, onMove, onScroll, click, markActivity)
                PointerMode.TOUCH -> TouchLayout(settings.okButtonPosition, settings.touchStyle, onMove, click, markActivity)
            }
        }
    }
}

@Composable
private fun MotionLayout(
    settings: PointerSettings,
    onMove: (Float, Float) -> Unit,
    onScroll: (Int) -> Unit,
    onOk: () -> Unit,
    markActivity: () -> Unit
) {
    val context = LocalContext.current
    val currentOnMove by rememberUpdatedState(onMove)
    val currentMarkActivity by rememberUpdatedState(markActivity)

    LifecycleStartEffect(settings.motionSensitivity, settings.motionAcceleration) {
        val sensor = MotionPointerSensor(context)
        sensor.start(settings.motionSensitivity, settings.motionAcceleration) { dx, dy ->
            currentOnMove(dx, dy)
            if (abs(dx) + abs(dy) > MOTION_ACTIVITY_THRESHOLD) currentMarkActivity()
        }
        onStopOrDispose { sensor.stop() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Hint(stringResource(R.string.pointer_motion_hint))
        OkButton(onOk, Modifier.size(dimensionResource(R.dimen.pointer_ok_size)))
        ScrollArea(onScroll, markActivity)
    }
}

@Composable
private fun TouchLayout(
    okPosition: OkButtonPosition,
    touchStyle: TouchStyle,
    onMove: (Float, Float) -> Unit,
    onOk: () -> Unit,
    markActivity: () -> Unit
) {
    val okBar = @Composable {
        OkButton(
            onOk,
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.pointer_ok_bar_height)),
            shape = RoundedCornerShape(percent = 50)
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.pointer_padding))
    ) {
        if (okPosition == OkButtonPosition.TOP) okBar()
        when (touchStyle) {
            TouchStyle.SWIPE -> TouchArea(onMove, onOk, markActivity)
            TouchStyle.JOYSTICK -> JoystickArea(onMove, onOk, markActivity)
        }
        if (okPosition == OkButtonPosition.BOTTOM) okBar()
    }
}

@Composable
private fun ColumnScope.TouchArea(onMove: (Float, Float) -> Unit, onTap: () -> Unit, markActivity: () -> Unit) {
    val vibrate = rememberKeyHaptic()
    val shape = RoundedCornerShape(dimensionResource(R.dimen.pointer_area_corner))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(colorResource(R.color.pointer_area), shape)
            .border(dimensionResource(R.dimen.key_rim_width), colorResource(R.color.pointer_area_border), shape)
            .pointerInput(vibrate) {
                detectTapGestures(onTap = {
                    vibrate()
                    onTap()
                })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    markActivity()
                    onMove(dragAmount.x * TOUCH_GAIN, dragAmount.y * TOUCH_GAIN)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_dpad_background),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxSize(0.8f)
                .alpha(BACKGROUND_PAD_ALPHA)
        )
        Hint(stringResource(R.string.pointer_touch_hint))
    }
}

@Composable
private fun ColumnScope.JoystickArea(onMove: (Float, Float) -> Unit, onTap: () -> Unit, markActivity: () -> Unit) {
    val vibrate = rememberKeyHaptic()
    val shape = RoundedCornerShape(dimensionResource(R.dimen.pointer_area_corner))
    val density = LocalDensity.current
    val radius = with(density) { dimensionResource(R.dimen.joystick_radius).toPx() }
    val deadZone = with(density) { dimensionResource(R.dimen.joystick_dead_zone).toPx() }
    val knobRadius = with(density) { dimensionResource(R.dimen.joystick_knob_radius).toPx() }
    val ringWidth = with(density) { dimensionResource(R.dimen.joystick_ring_width).toPx() }
    val ringColor = colorResource(R.color.joystick_ring)
    val knobColor = colorResource(R.color.joystick_knob)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentMarkActivity by rememberUpdatedState(markActivity)
    var origin by remember { mutableStateOf<Offset?>(null) }
    var position by remember { mutableStateOf(Offset.Zero) }
    val active = origin != null

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = (frame - lastFrame) / NANOS_PER_SECOND
            lastFrame = frame
            val start = origin ?: break
            val (vx, vy) = JoystickMotion.velocity(position.x - start.x, position.y - start.y, deadZone, radius)
            if (vx != 0f || vy != 0f) currentOnMove(vx * seconds, vy * seconds)
            currentMarkActivity()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(colorResource(R.color.pointer_area), shape)
            .border(dimensionResource(R.dimen.key_rim_width), colorResource(R.color.pointer_area_border), shape)
            .pointerInput(vibrate) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    origin = down.position
                    position = down.position
                    var dragged = false
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        position = change.position
                        if ((position - down.position).getDistance() > viewConfiguration.touchSlop) dragged = true
                        change.consume()
                    }
                    origin = null
                    if (!dragged) {
                        vibrate()
                        onTap()
                    }
                }
            }
            .drawBehind {
                val start = origin ?: return@drawBehind
                drawCircle(ringColor, radius, start, style = Stroke(ringWidth))
                val (kx, ky) = JoystickMotion.clampToRadius(position.x - start.x, position.y - start.y, radius)
                drawCircle(knobColor, knobRadius, Offset(start.x + kx, start.y + ky))
            },
        contentAlignment = Alignment.Center
    ) {
        if (!active) Hint(stringResource(R.string.pointer_joystick_hint))
    }
}

@Composable
private fun ScrollArea(onScroll: (Int) -> Unit, markActivity: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.pointer_area_corner))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.pointer_scroll_height))
            .background(colorResource(R.color.pointer_area), shape)
            .border(dimensionResource(R.dimen.key_rim_width), colorResource(R.color.pointer_area_border), shape)
            .pointerInput(Unit) {
                var accumulated = 0f
                detectVerticalDragGestures(onDragStart = { accumulated = 0f }) { change, dragAmount ->
                    change.consume()
                    markActivity()
                    accumulated += dragAmount
                    val steps = (accumulated / SCROLL_PIXELS_PER_STEP).roundToInt()
                    if (steps != 0) {
                        accumulated -= steps * SCROLL_PIXELS_PER_STEP
                        onScroll(-steps)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_up),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = stringResource(R.string.pointer_scroll_area),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            Image(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun OkButton(
    onOk: () -> Unit,
    modifier: Modifier,
    shape: Shape = CircleShape
) {
    RemoteKeyButton(
        contentDescription = stringResource(R.string.key_ok),
        onPress = onOk,
        modifier = modifier,
        shape = shape
    ) { color ->
        KeyLabel(text = stringResource(R.string.key_ok), color = color, fontSize = 28, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}
