package de.robnice.elgy.tv.connection

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.net.ssl.SSLContext

class WebOsConnector(
    private val httpClient: OkHttpClient,
    private val trustAllCertificates: Boolean
) {

    suspend fun connect(ipAddress: String, certificatePin: String?, allowInsecure: Boolean): WebOsConnection {
        try {
            return open(ipAddress, secure = true, certificatePin = certificatePin)
        } catch (e: TvConnectionException.CertificateChanged) {
            throw e
        } catch (e: TvConnectionException) {
            if (!allowInsecure) throw e
            Log.i(TAG, "Secure connection to $ipAddress failed, trying unencrypted: ${e.message}")
        }
        return open(ipAddress, secure = false, certificatePin = null)
    }

    private suspend fun open(ipAddress: String, secure: Boolean, certificatePin: String?): WebOsConnection {
        val trustManager = if (secure) CertificatePinTrustManager(certificatePin, trustAllCertificates) else null
        val client = clientFor(trustManager)
        val url = if (secure) "wss://$ipAddress:$SECURE_PORT" else "ws://$ipAddress:$PLAIN_PORT"
        val connection = WebOsConnection(secure, trustManager)
        val opened = CompletableDeferred<Unit>()

        val socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                opened.complete(Unit)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                connection.onMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connection.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                opened.completeExceptionally(mapFailure(t))
                connection.onClosed()
            }
        })
        connection.attach(socket)

        try {
            withTimeout(CONNECT_TIMEOUT_MILLIS) { opened.await() }
        } catch (_: TimeoutCancellationException) {
            socket.cancel()
            throw TvConnectionException.Timeout()
        }
        return connection
    }

    suspend fun openPointerSocket(url: String, certificatePin: String?): PointerSocket {
        val trustManager = if (url.startsWith("wss", ignoreCase = true)) {
            CertificatePinTrustManager(certificatePin, trustAllCertificates)
        } else null
        val pointerSocket = PointerSocket()
        val opened = CompletableDeferred<Unit>()

        val socket = clientFor(trustManager).newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                opened.complete(Unit)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                pointerSocket.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                opened.completeExceptionally(mapFailure(t))
                pointerSocket.onClosed()
            }
        })
        pointerSocket.attach(socket)

        try {
            withTimeout(CONNECT_TIMEOUT_MILLIS) { opened.await() }
        } catch (_: TimeoutCancellationException) {
            socket.cancel()
            throw TvConnectionException.Timeout()
        }
        return pointerSocket
    }

    // Resources served by the TV (e.g. app icons) use the same self-signed certificate as the WebSocket.
    fun httpClient(certificatePin: String?): OkHttpClient =
        clientFor(CertificatePinTrustManager(certificatePin, trustAllCertificates))

    private fun clientFor(trustManager: CertificatePinTrustManager?): OkHttpClient {
        if (trustManager == null) return httpClient
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        return httpClient.newBuilder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private fun mapFailure(throwable: Throwable): TvConnectionException {
        var cause: Throwable? = throwable
        while (cause != null) {
            if (cause is CertificatePinMismatchException) return TvConnectionException.CertificateChanged()
            cause = cause.cause
        }
        return TvConnectionException.Unreachable(throwable)
    }

    private companion object {
        const val TAG = "ElgyConnector"
        const val SECURE_PORT = 3001
        const val PLAIN_PORT = 3000
        const val CONNECT_TIMEOUT_MILLIS = 6_000L
    }
}
