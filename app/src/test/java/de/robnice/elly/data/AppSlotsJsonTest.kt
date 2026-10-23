package de.robnice.elly.data

import de.robnice.elly.data.model.AppSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSlotsJsonTest {

    @Test
    fun encodeAndDecode_roundTrip() {
        val slot = AppSlot(true, "youtube.leanback.v4", "YouTube", "/icons/yt.img", true, true, "YT", 0xFF1565C0L)

        assertEquals(slot, AppSlotsJson.decodeSlot(AppSlotsJson.encodeSlot(slot)))
    }

    @Test
    fun label_prefersCustomText() {
        assertEquals("YT", AppSlot(true, "yt", "YouTube", customText = "YT").label)
        assertEquals("YouTube", AppSlot(true, "yt", "YouTube", customText = " ").label)
    }
}
