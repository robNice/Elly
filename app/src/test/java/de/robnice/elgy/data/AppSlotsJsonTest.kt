package de.robnice.elgy.data

import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.AppSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSlotsJsonTest {

    @Test
    fun encodeAndDecode_roundTripPerOwner() {
        val slots = AppSlots.DEFAULT.toMutableList().apply {
            set(3, AppSlot(true, "youtube.leanback.v4", "YouTube", "/icons/yt.img", true, true, "YT", 0xFF1565C0L))
        }
        val all = mapOf("tv-1" to slots, AppSlots.DEMO_OWNER to AppSlots.DEFAULT)

        assertEquals(all, AppSlotsJson.decodeAll(AppSlotsJson.encodeAll(all)))
    }

    @Test
    fun decode_padsMissingSlotsAndCapsAtLimit() {
        val decoded = AppSlotsJson.decodeAll("""{"tv-1": [{"enabled": true, "appId": "netflix", "title": "Netflix"}]}""")

        assertEquals(AppSlots.COUNT, decoded.getValue("tv-1").size)
        assertEquals("netflix", decoded.getValue("tv-1")[0].appId)
        assertEquals(AppSlots.EMPTY, decoded.getValue("tv-1")[1])
    }

    @Test
    fun decode_handlesInvalidInput() {
        assertTrue(AppSlotsJson.decodeAll("no json").isEmpty())
        assertTrue(AppSlotsJson.decodeAll(null).isEmpty())
    }

    @Test
    fun label_prefersCustomText() {
        assertEquals("YT", AppSlot(true, "yt", "YouTube", customText = "YT").label)
        assertEquals("YouTube", AppSlot(true, "yt", "YouTube", customText = " ").label)
    }
}
