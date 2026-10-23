package de.robnice.elly.data.model

enum class PointerMode { MOTION, TOUCH }

enum class OkButtonPosition { TOP, BOTTOM }

enum class TouchStyle { SWIPE, JOYSTICK }

enum class JoystickPlacement { FREE, FIXED }

data class PointerSettings(
    val mode: PointerMode,
    val okButtonPosition: OkButtonPosition,
    val touchStyle: TouchStyle = TouchStyle.SWIPE,
    val joystickPlacement: JoystickPlacement = JoystickPlacement.FREE,
    val motionSensitivity: Float = MotionTuning.DEFAULT_SENSITIVITY,
    val motionAcceleration: Float = MotionTuning.DEFAULT_ACCELERATION,
    val swipeSpeed: Float = MotionTuning.DEFAULT_SWIPE_SPEED,
    val shakeToOpen: Boolean = false,
    val shakeSensitivity: Float = MotionTuning.DEFAULT_SHAKE_SENSITIVITY,
    val doubleTapToOpen: Boolean = false
)

object MotionTuning {
    const val DEFAULT_SENSITIVITY = 1f
    const val DEFAULT_ACCELERATION = 1.6f
    val SENSITIVITY_RANGE = 0.3f..3f
    val ACCELERATION_RANGE = 0f..4f
    const val DEFAULT_SWIPE_SPEED = 1f
    val SWIPE_SPEED_RANGE = 0.3f..3f
    const val DEFAULT_SHAKE_SENSITIVITY = 1.3f
    val SHAKE_SENSITIVITY_RANGE = 0.5f..5f
}
