package de.robnice.elgy.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalKeyHapticsEnabled = compositionLocalOf { true }

private const val REPEAT_DELAY_MILLIS = 400L

@Composable
fun rememberKeyHaptic(): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val enabled = LocalKeyHapticsEnabled.current
    return remember(hapticFeedback, enabled) {
        { if (enabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey) }
    }
}
private const val REPEAT_INTERVAL_MILLIS = 130L

@Composable
fun Modifier.keyPress(
    interactionSource: MutableInteractionSource,
    contentDescription: String,
    repeatOnHold: Boolean = false,
    onPress: () -> Unit
): Modifier {
    val vibrate = rememberKeyHaptic()
    val currentOnPress by rememberUpdatedState(onPress)

    return this
        .semantics {
            this.contentDescription = contentDescription
            role = Role.Button
            onClick {
                currentOnPress()
                true
            }
        }
        .pointerInput(interactionSource, repeatOnHold, vibrate) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    var repeated = false
                    val released = coroutineScope {
                        val repeater = if (repeatOnHold) launch {
                            delay(REPEAT_DELAY_MILLIS)
                            repeated = true
                            vibrate()
                            while (true) {
                                currentOnPress()
                                delay(REPEAT_INTERVAL_MILLIS)
                            }
                        } else null
                        tryAwaitRelease().also { repeater?.cancel() }
                    }
                    if (released && !repeated) {
                        vibrate()
                        currentOnPress()
                    }
                    interactionSource.emit(
                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                    )
                }
            )
        }
}
