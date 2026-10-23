package de.robnice.elly.ui.pointer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoystickMotionTest {

    @Test
    fun insideDeadZone_doesNotMove() {
        assertEquals(0f to 0f, JoystickMotion.velocity(5f, 5f, deadZone = 10f, radius = 100f))
    }

    @Test
    fun fullDeflection_reachesMaximumSpeed() {
        val (vx, vy) = JoystickMotion.velocity(100f, 0f, deadZone = 10f, radius = 100f)

        assertEquals(JoystickMotion.MAX_SPEED_PIXELS_PER_SECOND, vx, 0.01f)
        assertEquals(0f, vy, 0.01f)
    }

    @Test
    fun deflectionBeyondRadius_isCapped() {
        val (vx, _) = JoystickMotion.velocity(0f, 0f, deadZone = 10f, radius = 100f)
        val (cappedX, _) = JoystickMotion.velocity(-300f, 0f, deadZone = 10f, radius = 100f)

        assertEquals(0f, vx, 0.01f)
        assertEquals(-JoystickMotion.MAX_SPEED_PIXELS_PER_SECOND, cappedX, 0.01f)
    }

    @Test
    fun speedGrowsWithDeflection() {
        val small = JoystickMotion.velocity(0f, 30f, deadZone = 10f, radius = 100f).second
        val large = JoystickMotion.velocity(0f, 80f, deadZone = 10f, radius = 100f).second

        assertTrue(small > 0f)
        assertTrue(large > small)
    }

    @Test
    fun clampToRadius_keepsDirection() {
        val (x, y) = JoystickMotion.clampToRadius(300f, 400f, radius = 50f)

        assertEquals(30f, x, 0.01f)
        assertEquals(40f, y, 0.01f)
    }
}
