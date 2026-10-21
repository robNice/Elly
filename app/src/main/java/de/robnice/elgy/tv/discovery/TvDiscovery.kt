package de.robnice.elgy.tv.discovery

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TvDiscovery(
    private val ssdpDiscovery: SsdpDiscovery,
    private val subnetScanner: SubnetScanner
) {

    suspend fun discover(): List<DiscoveredTv> = coroutineScope {
        val ssdp = async { ssdpDiscovery.discover() }
        val subnet = async { subnetScanner.scan() }
        merge(ssdp.await(), subnet.await())
    }

    companion object {
        fun merge(preferred: List<DiscoveredTv>, fallback: List<DiscoveredTv>): List<DiscoveredTv> {
            val byIp = LinkedHashMap<String, DiscoveredTv>()
            (preferred + fallback).forEach { tv ->
                val known = byIp[tv.ipAddress]
                byIp[tv.ipAddress] = if (known == null) tv else known.copy(
                    name = known.name ?: tv.name,
                    modelName = known.modelName ?: tv.modelName
                )
            }
            return byIp.values.sortedWith(compareBy({ it.name == null }, { it.name }, { it.ipAddress }))
        }
    }
}
