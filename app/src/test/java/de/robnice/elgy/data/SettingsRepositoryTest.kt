package de.robnice.elgy.data

import de.robnice.elgy.data.model.PointerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun resolveMode_defaultsToMotionWhenAvailable() {
        assertEquals(PointerMode.MOTION, SettingsRepository.resolveMode(null, motionAvailable = true))
    }

    @Test
    fun resolveMode_keepsStoredChoice() {
        assertEquals(PointerMode.TOUCH, SettingsRepository.resolveMode(PointerMode.TOUCH, motionAvailable = true))
    }

    @Test
    fun resolveMode_fallsBackToTouchWithoutGyroscope() {
        assertEquals(PointerMode.TOUCH, SettingsRepository.resolveMode(PointerMode.MOTION, motionAvailable = false))
        assertEquals(PointerMode.TOUCH, SettingsRepository.resolveMode(null, motionAvailable = false))
    }
}
