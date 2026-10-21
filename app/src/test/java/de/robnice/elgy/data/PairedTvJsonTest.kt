package de.robnice.elgy.data

import de.robnice.elgy.data.model.PairedTv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedTvJsonTest {

    @Test
    fun encodeAndDecode_roundTrip() {
        val tvs = listOf(
            PairedTv("1", "192.168.0.20", "Living room", "OLED55C1", "key1", "AA:BB:CC:DD:EE:FF", "AB:CD", true),
            PairedTv("2", "192.168.0.30", "Bedroom", null, "key2", null, null, false)
        )

        assertEquals(tvs, PairedTvJson.decode(PairedTvJson.encode(tvs)))
    }

    @Test
    fun decode_skipsIncompleteEntriesAndInvalidJson() {
        assertTrue(PairedTvJson.decode("""[{"id": "1", "ipAddress": "192.168.0.20"}]""").isEmpty())
        assertTrue(PairedTvJson.decode("no json").isEmpty())
        assertTrue(PairedTvJson.decode(null).isEmpty())
    }
}
