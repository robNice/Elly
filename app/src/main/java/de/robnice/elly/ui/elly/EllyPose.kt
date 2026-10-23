package de.robnice.elly.ui.elly

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import de.robnice.elly.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class EllyMotion { NONE, BOB, BREATHE, DRIFT, SPIN, WANDER, AIM, POP, FLICK, STRIKE, FLASH }

// pivotX/pivotY are fractions of the drawable, used as the centre for rotation and scaling.
// range multiplies the drift distance; for AIM, FLICK and STRIKE it is the tilt in degrees,
// for SPIN the turns per loop (negative turns the other way). cycles speeds up AIM.
// Layers with a frame index are shown one at a time, each for FRAME_MS; frame -1 is always visible.
data class EllyLayer(
    @DrawableRes val image: Int,
    val motions: Set<EllyMotion>,
    val phase: Float = 0f,
    val pivotX: Float = 0.5f,
    val pivotY: Float = 0.5f,
    val range: Float = 1f,
    val cycles: Int = 1,
    val frame: Int = -1
) {
    constructor(
        @DrawableRes image: Int,
        motion: EllyMotion,
        phase: Float = 0f,
        pivotX: Float = 0.5f,
        pivotY: Float = 0.5f,
        range: Float = 1f,
        cycles: Int = 1,
        frame: Int = -1
    ) : this(image, setOf(motion), phase, pivotX, pivotY, range, cycles, frame)
}

// All layers share one viewport; the first layer's aspect ratio sets the mascot's width.
data class EllyPose(val layers: List<EllyLayer>) {
    constructor(@DrawableRes image: Int, motion: EllyMotion = EllyMotion.BOB) : this(listOf(EllyLayer(image, motion)))

    val frameCount: Int = layers.maxOf { it.frame } + 1
}

object EllyPoses {
    val Remote = EllyPose(R.drawable.elly)
    val Costumes = EllyPose(
        listOf(
            R.drawable.elly_costume_police,
            R.drawable.elly_costume_plumber,
            R.drawable.elly_costume_hero,
            R.drawable.elly_costume_santa,
            R.drawable.elly_costume_zombie,
            R.drawable.elly,
            R.drawable.elly_costume_skeleton,
            R.drawable.elly_costume_bunny,
            R.drawable.elly_costume_blocky,
            R.drawable.elly_costume_robot,
            R.drawable.elly_costume_headstand,
            R.drawable.elly_costume_martian,
            R.drawable.elly_costume_detective,
            R.drawable.elly_costume_hockey
        ).mapIndexed { index, image ->
            if (image == R.drawable.elly_costume_headstand) {
                EllyLayer(image, EllyMotion.DRIFT, pivotY = 0.92f, frame = index)
            } else {
                EllyLayer(image, EllyMotion.BOB, frame = index)
            }
        }
    )
    val Reading = EllyPose(R.drawable.elly_reading)
    val Hero = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_remote_hero_glow, setOf(EllyMotion.BOB, EllyMotion.SPIN), pivotX = 0.76f, pivotY = 0.22f),
            EllyLayer(R.drawable.elly_remote_hero, EllyMotion.BOB)
        )
    )
    val FirstSteps = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_first_steps, EllyMotion.BREATHE, pivotY = 1f),
            EllyLayer(R.drawable.elly_first_steps_question, EllyMotion.DRIFT, pivotX = 0.71f, pivotY = 0.1f)
        )
    )
    val KeysReading = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_keys_cloud, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_keys_leg, EllyMotion.AIM, pivotX = 0.139f, pivotY = 0.788f, range = 12f),
            EllyLayer(R.drawable.elly_keys_reading, EllyMotion.BREATHE, pivotY = 1f),
            EllyLayer(R.drawable.elly_keys_power, EllyMotion.DRIFT, 0f, 0.605f, 0.21f),
            EllyLayer(R.drawable.elly_keys_volume, EllyMotion.DRIFT, 0.3f, 0.701f, 0.24f),
            EllyLayer(R.drawable.elly_keys_dpad, EllyMotion.DRIFT, 0.55f, 0.803f, 0.24f),
            EllyLayer(R.drawable.elly_keys_play, EllyMotion.DRIFT, 0.8f, 0.903f, 0.24f)
        )
    )
    val Pointer = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_pointer_chair, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_pointer_elly, EllyMotion.BREATHE, pivotX = 0.27f, pivotY = 0.64f),
            EllyLayer(R.drawable.elly_pointer_arm, EllyMotion.AIM, pivotX = 0.267f, pivotY = 0.484f, range = 3.8f),
            EllyLayer(R.drawable.elly_pointer_front, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_pointer_cursor, EllyMotion.WANDER, range = 4f)
        )
    )
    val Painter = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_painter_easel, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_painter_elly, EllyMotion.BREATHE, pivotX = 0.313f, pivotY = 0.91f),
            EllyLayer(R.drawable.elly_painter_arm, EllyMotion.AIM, pivotX = 0.453f, pivotY = 0.62f, range = 5f, cycles = 3)
        )
    )
    val Magician = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_magic_elly, EllyMotion.BREATHE, pivotX = 0.3f, pivotY = 0.92f),
            EllyLayer(R.drawable.elly_magic_arm, EllyMotion.FLICK, pivotX = 0.453f, pivotY = 0.64f, range = 10f)
        ) + listOf(
            R.drawable.elly_magic_volume,
            R.drawable.elly_magic_channel,
            R.drawable.elly_magic_play,
            R.drawable.elly_magic_power,
            R.drawable.elly_magic_mute,
            R.drawable.elly_magic_screen
        ).mapIndexed { index, image ->
            EllyLayer(image, EllyMotion.POP, pivotX = 0.78f, pivotY = 0.39f, range = 15.5f, frame = index)
        } + listOf(
            EllyLayer(R.drawable.elly_magic_hat, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_magic_sparkles, EllyMotion.DRIFT, pivotX = 0.78f, pivotY = 0.4f)
        )
    )
    val Mechanic = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_mechanic_steam, EllyMotion.DRIFT, pivotX = 0.913f, pivotY = 0.07f),
            EllyLayer(R.drawable.elly_mechanic_machine, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_mechanic_gear_big, EllyMotion.SPIN, pivotX = 0.82f, pivotY = 0.55f),
            EllyLayer(R.drawable.elly_mechanic_gear_small, EllyMotion.SPIN, pivotX = 0.7333f, pivotY = 0.64f, range = -10f / 7f),
            EllyLayer(R.drawable.elly_mechanic_needle, EllyMotion.AIM, pivotX = 0.72f, pivotY = 0.42f, range = 25f, cycles = 3),
            EllyLayer(R.drawable.elly_mechanic_elly, EllyMotion.BREATHE, pivotX = 0.3f, pivotY = 0.92f),
            // The wing turns around the shoulder against the wrench so the wing tip stays on the handle.
            EllyLayer(R.drawable.elly_mechanic_arm, EllyMotion.AIM, pivotX = 0.48f, pivotY = 0.65f, range = -8.6f, cycles = 2),
            EllyLayer(R.drawable.elly_mechanic_wrench, EllyMotion.AIM, pivotX = 0.7333f, pivotY = 0.64f, range = 6f, cycles = 2)
        )
    )
    val Judge = EllyPose(
        listOf(
            EllyLayer(R.drawable.elly_judge_elly, EllyMotion.BREATHE, pivotY = 0.85f),
            EllyLayer(R.drawable.elly_judge_bench, EllyMotion.NONE),
            EllyLayer(R.drawable.elly_judge_arm, EllyMotion.STRIKE, pivotX = 0.68f, pivotY = 0.66f, range = 30f),
            EllyLayer(R.drawable.elly_judge_impact, EllyMotion.FLASH)
        )
    )
}

// time runs from 0 to 1 once per loop; every motion completes whole cycles so the loop is seamless.
// frameProgress runs from 0 to 1 once per frame (FRAME_MS) and drives POP, FLICK, STRIKE and FLASH.
internal fun GraphicsLayerScope.applyMotion(layer: EllyLayer, time: Float, frameProgress: Float, bob: Float, drift: Float) {
    layer.motions.forEach { applyMotion(it, layer, time, frameProgress, bob, drift) }
}

private fun GraphicsLayerScope.applyMotion(
    motion: EllyMotion,
    layer: EllyLayer,
    time: Float,
    frameProgress: Float,
    bob: Float,
    drift: Float
) {
    val turn = 2 * PI.toFloat()
    when (motion) {
        EllyMotion.NONE -> Unit
        EllyMotion.BOB -> {
            translationY = -bob * (1 - cos(turn * (BOB_CYCLES * time + layer.phase))) / 2
        }
        EllyMotion.BREATHE -> {
            val breath = (1 - cos(turn * (BREATHE_CYCLES * time + layer.phase))) / 2
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            scaleY = 1 + BREATHE_SCALE * breath
            scaleX = 1 + BREATHE_SCALE / 2 * breath
        }
        EllyMotion.DRIFT -> {
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            translationX = drift * sin(turn * (time + layer.phase))
            translationY = drift * sin(turn * (2 * time + layer.phase * 1.7f))
            rotationZ = DRIFT_DEGREES * sin(turn * (time + layer.phase + 0.25f))
        }
        EllyMotion.WANDER -> {
            translationX = drift * layer.range * sin(turn * (time + layer.phase))
            translationY = drift * layer.range * WANDER_HEIGHT * sin(turn * (2 * time + layer.phase))
        }
        EllyMotion.AIM -> {
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            rotationZ = layer.range * sin(turn * (2 * layer.cycles * time + layer.phase))
        }
        EllyMotion.POP -> {
            val rise = 1 - (1 - (frameProgress / POP_SHARE).coerceAtMost(1f)).let { it * it * it }
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            translationY = drift * layer.range * (1 - rise)
            scaleX = POP_START_SCALE + (1 - POP_START_SCALE) * rise
            scaleY = scaleX
        }
        EllyMotion.FLICK -> {
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            rotationZ = -layer.range * sin(turn * frameProgress)
        }
        EllyMotion.STRIKE -> {
            val lift = if (frameProgress < STRIKE_RISE) {
                (frameProgress / STRIKE_RISE).let { it * it * (3 - 2 * it) }
            } else {
                1 - ((frameProgress - STRIKE_RISE) / (1 - STRIKE_RISE)).let { it * it }
            }
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            rotationZ = -layer.range * lift
        }
        EllyMotion.FLASH -> {
            alpha = if (frameProgress < FLASH_SHARE) 1f else 0f
        }
        EllyMotion.SPIN -> {
            transformOrigin = TransformOrigin(layer.pivotX, layer.pivotY)
            rotationZ = 360f * layer.range * (time + layer.phase)
        }
    }
}

internal const val LOOP_MS = 8400
internal const val FRAME_MS = 1000
private const val BOB_CYCLES = 3
private const val BREATHE_CYCLES = 2
private const val BREATHE_SCALE = 0.025f
private const val DRIFT_DEGREES = 8f
private const val WANDER_HEIGHT = 0.8f
private const val POP_SHARE = 0.35f
private const val POP_START_SCALE = 0.5f
private const val STRIKE_RISE = 0.8f
private const val FLASH_SHARE = 0.15f
