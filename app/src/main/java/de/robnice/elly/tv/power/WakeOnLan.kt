package de.robnice.elly.tv.power

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLan {

    suspend fun wake(macAddress: String, ipAddress: String?): Boolean = withContext(Dispatchers.IO) {
        val packet = magicPacket(macAddress) ?: return@withContext false
        val targets = listOfNotNull("255.255.255.255", ipAddress?.let(::subnetBroadcast)).distinct()
        var sent = false
        DatagramSocket().use { socket ->
            socket.broadcast = true
            targets.forEach { target ->
                PORTS.forEach { port ->
                    runCatching {
                        socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName(target), port))
                        sent = true
                    }
                }
            }
        }
        sent
    }

    companion object {
        private val PORTS = listOf(9, 7)

        fun magicPacket(macAddress: String): ByteArray? {
            val bytes = macAddress.split(':', '-')
                .takeIf { it.size == 6 }
                ?.map { it.toIntOrNull(16)?.toByte() ?: return null }
                ?: return null
            return ByteArray(6) { 0xFF.toByte() } + List(16) { bytes }.flatten().toByteArray()
        }

        fun subnetBroadcast(ipAddress: String): String? {
            val parts = ipAddress.split('.')
            if (parts.size != 4 || parts.any { it.toIntOrNull() !in 0..255 }) return null
            return parts.take(3).joinToString(".") + ".255"
        }
    }
}
