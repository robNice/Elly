package de.robnice.elly.tv.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URL

class SsdpDiscovery(
    private val context: Context,
    private val networkInterfaces: LocalNetworkInterfaces
) {

    suspend fun discover(listenMillis: Long = LISTEN_MILLIS): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val multicastLock = acquireMulticastLock()
        try {
            val locations = coroutineScope {
                networkInterfaces.ipv4Addresses()
                    .map { address -> async { searchFrom(address, listenMillis) } }
                    .awaitAll()
                    .flatten()
                    .toMap()
            }
            coroutineScope {
                locations.map { (ip, location) -> async { describe(ip, location) } }.awaitAll()
            }
        } finally {
            multicastLock?.takeIf { it.isHeld }?.release()
        }
    }

    private fun searchFrom(localAddress: Inet4Address, listenMillis: Long): List<Pair<String, String?>> {
        val results = mutableListOf<Pair<String, String?>>()
        val target = InetAddress.getByName(SsdpMessages.MULTICAST_ADDRESS)
        val request = SsdpMessages.mSearch()
        try {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(localAddress, 0))
                socket.soTimeout = RECEIVE_TIMEOUT_MILLIS
                repeat(SEND_REPEATS) {
                    socket.send(DatagramPacket(request, request.size, target, SsdpMessages.PORT))
                }
                val buffer = ByteArray(BUFFER_SIZE)
                val end = System.currentTimeMillis() + listenMillis
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val headers = SsdpMessages.parseHeaders(String(packet.data, 0, packet.length, Charsets.UTF_8))
                    val ip = packet.address.hostAddress ?: continue
                    if (SsdpMessages.isWebOsResponse(headers)) results += ip to headers["location"]
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SSDP search from ${localAddress.hostAddress} failed: ${e.message}")
        }
        return results
    }

    private fun describe(ip: String, location: String?): DiscoveredTv {
        val description = location?.let { fetchDescription(it) }
        return DiscoveredTv(
            ipAddress = ip,
            name = description?.friendlyName,
            modelName = description?.modelName
        )
    }

    private fun fetchDescription(location: String): DeviceDescriptionParser.DeviceDescription? = try {
        val connection = URL(location).openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_TIMEOUT_MILLIS
        connection.readTimeout = HTTP_TIMEOUT_MILLIS
        try {
            DeviceDescriptionParser.parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Device description $location failed: ${e.message}")
        null
    }

    private fun acquireMulticastLock(): WifiManager.MulticastLock? = try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiManager?.createMulticastLock(TAG)?.apply {
            setReferenceCounted(false)
            acquire()
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TAG = "EllySsdp"
        const val LISTEN_MILLIS = 3000L
        const val RECEIVE_TIMEOUT_MILLIS = 300
        const val SEND_REPEATS = 3
        const val BUFFER_SIZE = 4096
        const val HTTP_TIMEOUT_MILLIS = 2000
    }
}
