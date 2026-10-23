package de.robnice.elly.tv.pairing

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacAddressSelectorTest {

    @Test
    fun select_prefersConnectedInterface() {
        val info = JSONObject(
            """
            {
              "wiredInfo": {"state": "disconnected", "macAddress": "aa:bb:cc:dd:ee:01"},
              "wifiInfo": {"state": "connected", "macAddress": "aa:bb:cc:dd:ee:02"}
            }
            """
        )

        assertEquals("AA:BB:CC:DD:EE:02", MacAddressSelector.select(info))
    }

    @Test
    fun select_fallsBackToAnyValidAddress() {
        val info = JSONObject("""{"wiredInfo": {"macAddress": "aa-bb-cc-dd-ee-03"}}""")

        assertEquals("AA:BB:CC:DD:EE:03", MacAddressSelector.select(info))
    }

    @Test
    fun select_returnsNullWithoutValidAddress() {
        val info = JSONObject("""{"wifiInfo": {"state": "connected", "macAddress": "unknown"}}""")

        assertNull(MacAddressSelector.select(info))
    }
}
