package de.robnice.elgy.tv.connection

import android.util.Log
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

// Debug builds only: logs how long each phase of a TV connection takes, to find where it stalls.
class ConnectionTimingLogger(private val url: String) : EventListener() {

    private val start = System.nanoTime()

    private fun log(event: String) {
        Log.d(TAG, "$url +${(System.nanoTime() - start) / 1_000_000} ms $event")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) = log("tcp connect start")

    override fun secureConnectStart(call: Call) = log("tcp connected, tls start")

    override fun secureConnectEnd(call: Call, handshake: Handshake?) = log("tls done")

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) =
        log("connection ready")

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) = log("connect failed: $ioe")

    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) = log("upgrade request sent")

    override fun responseHeadersEnd(call: Call, response: Response) = log("upgrade response ${response.code}")

    override fun callFailed(call: Call, ioe: IOException) = log("failed: $ioe")

    override fun canceled(call: Call) = log("canceled")

    private companion object {
        const val TAG = "ElgyTiming"
    }
}
