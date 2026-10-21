package de.robnice.elgy.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerMotionTest {

    // Held like a remote: screen up, top edge pointing at the TV.
    private val remoteGrip = floatArrayOf(0f, 0f, 9.81f)

    // Held upright in front of the body: screen facing the user, back facing the TV.
    private val uprightGrip = floatArrayOf(0f, 9.81f, 0f)

    // Halfway between both grips.
    private val tiltedGrip = floatArrayOf(0f, 6.94f, 6.94f)

    private fun move(rate: FloatArray, gravity: FloatArray) = PointerMotion.delta(rate, gravity, 0.01f)!!

    @Test
    fun remoteGrip_swingLeft_movesLeft() {
        val (dx, dy) = move(floatArrayOf(0f, 0f, 1f), remoteGrip)

        assertTrue(dx < 0f)
        assertEquals(0f, dy, 0.0001f)
    }

    @Test
    fun remoteGrip_twistWrist_isIgnored() {
        assertNull(PointerMotion.delta(floatArrayOf(0f, 1f, 0f), remoteGrip, 0.01f))
    }

    @Test
    fun remoteGrip_tiltTopUp_movesUp() {
        val (dx, dy) = move(floatArrayOf(1f, 0f, 0f), remoteGrip)

        assertEquals(0f, dx, 0.0001f)
        assertTrue(dy < 0f)
    }

    @Test
    fun uprightGrip_turnLeft_movesLeft() {
        val (dx, dy) = move(floatArrayOf(0f, 1f, 0f), uprightGrip)

        assertEquals(-PointerMotion.gainFor(1f) * 0.01f, dx, 0.0001f)
        assertEquals(0f, dy, 0.0001f)
    }

    @Test
    fun uprightGrip_steerLikeWheel_isIgnored() {
        assertNull(PointerMotion.delta(floatArrayOf(0f, 0f, -1f), uprightGrip, 0.01f))
    }

    @Test
    fun uprightGrip_tiltTopBackwards_movesUp() {
        val (dx, dy) = move(floatArrayOf(1f, 0f, 0f), uprightGrip)

        assertEquals(0f, dx, 0.0001f)
        assertTrue(dy < 0f)
    }

    @Test
    fun tiltedGrip_swingAroundVerticalAxis_movesHorizontallyOnly() {
        val vertical = floatArrayOf(0f, 0.7071f, 0.7071f)
        val (dx, dy) = move(vertical, tiltedGrip)

        assertTrue(dx < 0f)
        assertEquals(0f, dy, 0.001f)
    }

    @Test
    fun fastSwings_moveFurtherPerRadianThanSlowMovements() {
        assertTrue(PointerMotion.gainFor(3f) > PointerMotion.gainFor(0.2f) * 2f)
    }

    @Test
    fun sensitivityScalesGainLinearly() {
        assertEquals(PointerMotion.gainFor(1f) * 2f, PointerMotion.gainFor(1f, sensitivity = 2f), 0.01f)
    }

    @Test
    fun zeroAcceleration_keepsGainConstant() {
        assertEquals(
            PointerMotion.gainFor(0.1f, acceleration = 0f),
            PointerMotion.gainFor(3f, acceleration = 0f),
            0.01f
        )
    }

    @Test
    fun smallRotations_areIgnored() {
        assertNull(PointerMotion.delta(floatArrayOf(0.001f, 0.001f, 0.001f), remoteGrip, 0.01f))
    }
}
