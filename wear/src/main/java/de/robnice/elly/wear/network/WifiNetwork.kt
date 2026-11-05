package de.robnice.elly.wear.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

// Wear OS routes traffic through the phone's Bluetooth proxy, which usually cannot reach the TV in the
// local network. While the remote or a quick key needs the TV, Wi-Fi is requested and the whole process is
// bound to it. The app and the tile acquire it independently, so it is only released when neither needs it.
class WifiNetwork(context: Context) {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    // In power save the Wi-Fi chip holds packets back and sends them in bursts, which makes the TV pointer jump.
    private val lowLatency = context.getSystemService(WifiManager::class.java)
        .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, LOW_LATENCY_TAG)
        .apply { setReferenceCounted(false) }
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var users = 0
    private val available = MutableStateFlow(false)

    @Synchronized
    fun acquire() {
        users++
        if (callback != null) return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivity.bindProcessToNetwork(network)
                available.value = true
            }

            override fun onLost(network: Network) {
                available.value = false
                connectivity.bindProcessToNetwork(null)
            }
        }
        connectivity.requestNetwork(request, networkCallback)
        callback = networkCallback
    }

    @Synchronized
    fun release() {
        if (users == 0) return
        users--
        if (users > 0) return
        val networkCallback = callback ?: return
        callback = null
        available.value = false
        connectivity.unregisterNetworkCallback(networkCallback)
        connectivity.bindProcessToNetwork(null)
    }

    @Synchronized
    fun setLowLatency(enabled: Boolean) {
        if (enabled && !lowLatency.isHeld) lowLatency.acquire()
        if (!enabled && lowLatency.isHeld) lowLatency.release()
    }

    // Without Wi-Fi the command still goes out, through the phone, in case the TV is reachable that way.
    suspend fun awaitAvailable() {
        withTimeoutOrNull(WAIT_MILLIS) { available.first { it } }
    }

    private companion object {
        const val LOW_LATENCY_TAG = "Elly:pointer"
        const val WAIT_MILLIS = 4_000L
    }
}
