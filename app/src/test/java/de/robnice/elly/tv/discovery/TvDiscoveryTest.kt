package de.robnice.elly.tv.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class TvDiscoveryTest {

    @Test
    fun merge_combinesResultsByIpAndKeepsNames() {
        val ssdp = listOf(DiscoveredTv("192.168.0.20", "Living room", "OLED55C1"))
        val subnet = listOf(
            DiscoveredTv("192.168.0.20", null, null),
            DiscoveredTv("192.168.0.30", null, null)
        )

        val merged = TvDiscovery.merge(ssdp, subnet)

        assertEquals(
            listOf(
                DiscoveredTv("192.168.0.20", "Living room", "OLED55C1"),
                DiscoveredTv("192.168.0.30", null, null)
            ),
            merged
        )
    }

    @Test
    fun merge_sortsNamedTvsFirst() {
        val merged = TvDiscovery.merge(
            listOf(DiscoveredTv("192.168.0.9", null, null), DiscoveredTv("192.168.0.50", "Bedroom", null)),
            emptyList()
        )

        assertEquals(listOf("192.168.0.50", "192.168.0.9"), merged.map { it.ipAddress })
    }
}
