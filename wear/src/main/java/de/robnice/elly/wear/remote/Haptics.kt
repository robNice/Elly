package de.robnice.elly.wear.remote

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import de.robnice.elly.wear.data.WearSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Uses the vibrator directly: the system touch feedback that Compose goes through is off on many watches.
// The setting is kept in memory, so a vibration starts in the same call as the touch and does not wait for
// DataStore.
class Haptics(context: Context, settings: WearSettings, private val scope: CoroutineScope) {

    private val vibrator = context.applicationContext.getSystemService(Vibrator::class.java)

    @Volatile
    private var enabled = true
    private var beat: Job? = null

    @Volatile
    private var speed = 0f

    @Volatile
    private var lastMotion = 0L

    init {
        scope.launch { settings.haptics.collect { enabled = it } }
    }

    fun press() = play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

    // While the volume gesture or the crown moves, the watch pulses in a steady beat; faster movement makes the
    // pulses stronger. A pulse per volume step felt uneven, because the steps follow the touch events and the
    // finger never moves at an even speed. The beat stops shortly after the last movement.
    @Synchronized
    fun motion(stepsPerSecond: Float) {
        if (!enabled) return
        lastMotion = SystemClock.uptimeMillis()
        if (beat?.isActive == true) {
            speed += SPEED_SMOOTHING * (stepsPerSecond - speed)
        } else {
            speed = stepsPerSecond
            beat = scope.launch { pulse() }
        }
    }

    @Synchronized
    fun motionEnded() {
        beat?.cancel()
        beat = null
    }

    private suspend fun pulse() {
        while (SystemClock.uptimeMillis() - lastMotion < MOTION_TIMEOUT_MILLIS) {
            val share = ((speed - SLOW_STEPS_PER_SECOND) / (FAST_STEPS_PER_SECOND - SLOW_STEPS_PER_SECOND)).coerceIn(0f, 1f)
            val amplitude = (MIN_AMPLITUDE + (MAX_AMPLITUDE - MIN_AMPLITUDE) * share).roundToInt()
            vibrator?.vibrate(VibrationEffect.createOneShot(PULSE_MILLIS, amplitude))
            delay(BEAT_MILLIS)
        }
    }

    fun failure() = play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))

    private fun play(effect: VibrationEffect) {
        if (enabled) vibrator?.vibrate(effect)
    }

    private companion object {
        const val BEAT_MILLIS = 60L
        const val PULSE_MILLIS = 20L
        const val MOTION_TIMEOUT_MILLIS = 150L
        const val SPEED_SMOOTHING = 0.2f
        const val SLOW_STEPS_PER_SECOND = 2f
        const val FAST_STEPS_PER_SECOND = 15f
        const val MIN_AMPLITUDE = 60
        const val MAX_AMPLITUDE = 255
    }
}
