package de.robnice.elly.tv.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class SubnetScanner(private val networkInterfaces: LocalNetworkInterfaces) {

    suspend fun scan(): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val localAddresses = networkInterfaces.ipv4Addresses()
            .filter { it.isSiteLocalAddress }
            .mapNotNull { it.hostAddress }
        val candidates = localAddresses.flatMap { hostsOfSubnet(it) }.toSet() - localAddresses.toSet()
        val semaphore = Semaphore(MAX_PARALLEL_PROBES)
        coroutineScope {
            candidates.map { ip ->
                async { semaphore.withPermit { if (isWebOsHost(ip)) ip else null } }
            }.awaitAll()
        }.filterNotNull().map { DiscoveredTv(ipAddress = it, name = null, modelName = null) }
    }

    private fun isWebOsHost(ip: String): Boolean = WEBOS_PORTS.any { isPortOpen(ip, it) }

    private fun isPortOpen(ip: String, port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MILLIS) }
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val MAX_PARALLEL_PROBES = 32
        private const val CONNECT_TIMEOUT_MILLIS = 400
        private val WEBOS_PORTS = listOf(3001, 3000)

        fun hostsOfSubnet(ipAddress: String): List<String> {
            val parts = ipAddress.split('.')
            if (parts.size != 4 || parts.any { it.toIntOrNull() !in 0..255 }) return emptyList()
            val prefix = parts.take(3).joinToString(".")
            return (1..254).map { "$prefix.$it" }
        }
    }
}
