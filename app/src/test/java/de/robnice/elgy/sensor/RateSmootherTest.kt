package de.robnice.elgy.sensor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RateSmootherTest {

    @Test
    fun firstValue_isTakenAsIs() {
        val smoother = RateSmoother(0.5f)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), smoother.smooth(floatArrayOf(1f, 2f, 3f)), 0.0001f)
    }

    @Test
    fun followingValues_moveTowardsInput() {
        val smoother = RateSmoother(0.5f)
        smoother.smooth(floatArrayOf(0f, 0f, 0f))

        assertArrayEquals(floatArrayOf(1f, 0f, -1f), smoother.smooth(floatArrayOf(2f, 0f, -2f)), 0.0001f)
    }

    @Test
    fun jitter_isDamped() {
        val smoother = RateSmoother()
        smoother.smooth(floatArrayOf(0f, 0f, 0f))
        val outputs = (0 until 20).map { i ->
            smoother.smooth(floatArrayOf(if (i % 2 == 0) 0.2f else -0.2f, 0f, 0f))[0]
        }

        assertTrue(outputs.all { abs(it) < 0.2f })
    }

    @Test
    fun reset_startsFromNextValue() {
        val smoother = RateSmoother(0.5f)
        smoother.smooth(floatArrayOf(5f, 5f, 5f))
        smoother.reset()

        assertArrayEquals(floatArrayOf(1f, 1f, 1f), smoother.smooth(floatArrayOf(1f, 1f, 1f)), 0.0001f)
    }
}
