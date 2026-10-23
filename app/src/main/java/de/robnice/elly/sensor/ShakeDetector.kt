package de.robnice.elly.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import de.robnice.elly.data.model.MotionTuning
import kotlin.math.sqrt

// Reports a shake: several strong jolts of the phone within a short time.
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private var jolts = 0
    private var firstJolt = 0L
    private var lastJolt = 0L
    private var lastShake = 0L
    private var startedAt = 0L

    // Higher values need a weaker jolt.
    var sensitivity = MotionTuning.DEFAULT_SHAKE_SENSITIVITY

    // Movements right after the start are ignored, for example the tap that closed the pointer.
    fun start() {
        startedAt = SystemClock.elapsedRealtimeNanos()
        sensor?.let { manager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        manager?.unregisterListener(this)
        jolts = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (sqrt(x * x + y * y + z * z) < JOLT_ACCELERATION / sensitivity) return
        val now = event.timestamp
        if (now - startedAt < START_PAUSE_NANOS) return
        if (now - lastJolt < MIN_JOLT_GAP_NANOS) return
        if (jolts == 0 || now - firstJolt > SHAKE_WINDOW_NANOS) {
            jolts = 0
            firstJolt = now
        }
        jolts++
        lastJolt = now
        if (jolts >= JOLTS_PER_SHAKE && now - lastShake > SHAKE_PAUSE_NANOS) {
            jolts = 0
            lastShake = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val JOLT_ACCELERATION = 13f
        const val JOLTS_PER_SHAKE = 3
        const val MIN_JOLT_GAP_NANOS = 90_000_000L
        const val SHAKE_WINDOW_NANOS = 900_000_000L
        const val SHAKE_PAUSE_NANOS = 1_500_000_000L
        const val START_PAUSE_NANOS = 1_500_000_000L
    }
}
