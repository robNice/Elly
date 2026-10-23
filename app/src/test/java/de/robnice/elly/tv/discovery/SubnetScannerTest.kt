package de.robnice.elly.tv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetScannerTest {

    @Test
    fun hostsOfSubnet_returnsAllHostsOfSlash24() {
        val hosts = SubnetScanner.hostsOfSubnet("192.168.178.42")

        assertEquals(254, hosts.size)
        assertEquals("192.168.178.1", hosts.first())
        assertEquals("192.168.178.254", hosts.last())
    }

    @Test
    fun hostsOfSubnet_rejectsInvalidAddresses() {
        assertTrue(SubnetScanner.hostsOfSubnet("192.168.1").isEmpty())
        assertTrue(SubnetScanner.hostsOfSubnet("192.168.1.300").isEmpty())
        assertTrue(SubnetScanner.hostsOfSubnet("fe80::1").isEmpty())
    }
}
