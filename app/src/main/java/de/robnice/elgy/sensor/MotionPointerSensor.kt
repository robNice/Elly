package de.robnice.elgy.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import de.robnice.elgy.data.model.MotionTuning
import kotlin.math.abs
import kotlin.math.sqrt

class MotionPointerSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = floatArrayOf(0f, 0f, SensorManager.GRAVITY_EARTH)
    private var lastTimestamp = 0L
    private var onMove: ((Float, Float) -> Unit)? = null
    private var sensitivity = MotionTuning.DEFAULT_SENSITIVITY
    private var acceleration = MotionTuning.DEFAULT_ACCELERATION
    private val smoother = RateSmoother()

    val isAvailable: Boolean
        get() = gyroscope != null

    fun start(sensitivity: Float, acceleration: Float, onMove: (dx: Float, dy: Float) -> Unit) {
        this.onMove = onMove
        this.sensitivity = sensitivity
        this.acceleration = acceleration
        lastTimestamp = 0L
        smoother.reset()
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onMove = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] = gravity[i] + GRAVITY_SMOOTHING * (event.values[i] - gravity[i])
            }
            Sensor.TYPE_GYROSCOPE -> handleRotation(event)
        }
    }

    private fun handleRotation(event: SensorEvent) {
        val previous = lastTimestamp
        lastTimestamp = event.timestamp
        if (previous == 0L) return
        val seconds = (event.timestamp - previous) / NANOS_PER_SECOND

        val delta = PointerMotion.delta(
            rotationRate = smoother.smooth(event.values),
            gravity = gravity,
            seconds = seconds,
            sensitivity = sensitivity,
            acceleration = acceleration
        ) ?: return
        onMove?.invoke(delta.first, delta.second)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val GRAVITY_SMOOTHING = 0.2f
        private const val NANOS_PER_SECOND = 1_000_000_000f

        fun isSupported(context: Context): Boolean =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
}

object PointerMotion {

    const val PIXELS_PER_RADIAN = 900f
    private const val DEAD_ZONE_RADIANS_PER_SECOND = 0.015f
    private const val MIN_VECTOR_LENGTH = 0.1f

    // Android reports gravity pointing up. The phone aims along its top edge when held flat like a remote
    // and along its back when held upright; blending both and projecting onto the horizontal plane covers
    // every grip in between. Horizontal movement is swinging around the vertical axis, vertical movement is
    // tilting around the horizontal cross axis. Rolling around the aiming axis is ignored on purpose: it happens
    // unintentionally while aiming and makes the pointer shaky.
    fun delta(
        rotationRate: FloatArray,
        gravity: FloatArray,
        seconds: Float,
        sensitivity: Float = MotionTuning.DEFAULT_SENSITIVITY,
        acceleration: Float = MotionTuning.DEFAULT_ACCELERATION
    ): Pair<Float, Float>? {
        if (seconds <= 0f) return null
        val up = normalize(gravity) ?: return null
        val aim = floatArrayOf(0f, 1f, -1f)
        val forward = normalize(minus(aim, scale(up, dot(aim, up)))) ?: return null
        val right = cross(forward, up)

        val horizontal = -dot(rotationRate, up)
        val vertical = -dot(rotationRate, right)
        if (abs(horizontal) < DEAD_ZONE_RADIANS_PER_SECOND && abs(vertical) < DEAD_ZONE_RADIANS_PER_SECOND) return null
        val gain = gainFor(sqrt(horizontal * horizontal + vertical * vertical), sensitivity, acceleration)
        return (horizontal * seconds * gain) to (vertical * seconds * gain)
    }

    // Slow movements stay precise, quick swings cover the whole screen.
    fun gainFor(
        angularSpeed: Float,
        sensitivity: Float = MotionTuning.DEFAULT_SENSITIVITY,
        acceleration: Float = MotionTuning.DEFAULT_ACCELERATION
    ): Float = PIXELS_PER_RADIAN * sensitivity * (1f + acceleration * angularSpeed)

    private fun dot(a: FloatArray, b: FloatArray): Float = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    private fun cross(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    )

    private fun scale(a: FloatArray, factor: Float): FloatArray = floatArrayOf(a[0] * factor, a[1] * factor, a[2] * factor)

    private fun minus(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])

    private fun normalize(a: FloatArray): FloatArray? {
        val length = sqrt(dot(a, a))
        return if (length < MIN_VECTOR_LENGTH) null else scale(a, 1f / length)
    }
}
