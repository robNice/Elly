package de.robnice.elly.tv.discovery

import java.net.Inet4Address
import java.net.NetworkInterface

class LocalNetworkInterfaces {

    fun ipv4Addresses(): List<Inet4Address> = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback && it.supportsMulticast() }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
    } catch (_: Exception) {
        emptyList()
    }
}
