package de.robnice.elly.tv.power

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeOnLanTest {

    @Test
    fun magicPacket_containsSyncStreamAndSixteenRepetitions() {
        val packet = WakeOnLan.magicPacket("AA:BB:CC:DD:EE:FF")!!
        val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())

        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xFF.toByte() }, packet.copyOfRange(0, 6))
        (0 until 16).forEach { i -> assertArrayEquals(mac, packet.copyOfRange(6 + i * 6, 12 + i * 6)) }
    }

    @Test
    fun magicPacket_acceptsDashSeparator() {
        assertEquals(102, WakeOnLan.magicPacket("aa-bb-cc-dd-ee-ff")?.size)
    }

    @Test
    fun magicPacket_rejectsInvalidAddresses() {
        assertNull(WakeOnLan.magicPacket("AA:BB:CC:DD:EE"))
        assertNull(WakeOnLan.magicPacket("AA:BB:CC:DD:EE:GG"))
    }

    @Test
    fun subnetBroadcast_replacesLastOctet() {
        assertEquals("192.168.178.255", WakeOnLan.subnetBroadcast("192.168.178.42"))
        assertNull(WakeOnLan.subnetBroadcast("no ip"))
    }
}
