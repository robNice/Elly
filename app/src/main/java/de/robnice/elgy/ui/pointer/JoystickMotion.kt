package de.robnice.elgy.ui.pointer

import kotlin.math.sqrt

object JoystickMotion {

    const val MAX_SPEED_PIXELS_PER_SECOND = 1800f

    // Quadratic response: small deflections allow fine positioning, full deflection moves fast.
    fun velocity(offsetX: Float, offsetY: Float, deadZone: Float, radius: Float): Pair<Float, Float> {
        val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
        if (distance <= deadZone || radius <= deadZone) return 0f to 0f
        val deflection = ((distance.coerceAtMost(radius) - deadZone) / (radius - deadZone))
        val speed = MAX_SPEED_PIXELS_PER_SECOND * deflection * deflection
        return (offsetX / distance * speed) to (offsetY / distance * speed)
    }

    fun clampToRadius(offsetX: Float, offsetY: Float, radius: Float): Pair<Float, Float> {
        val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
        if (distance <= radius || distance == 0f) return offsetX to offsetY
        return (offsetX / distance * radius) to (offsetY / distance * radius)
    }
}
