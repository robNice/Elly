package de.robnice.elgy.tv.connection

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import okhttp3.WebSocket
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class WebOsConnection internal constructor(
    val secure: Boolean,
    private val trustManager: CertificatePinTrustManager?
) : Closeable {

    private lateinit var webSocket: WebSocket
    private val nextId = AtomicInteger()
    private val pending = ConcurrentHashMap<String, Channel<JSONObject>>()
    private val subscriptions = ConcurrentHashMap<String, (JSONObject) -> Unit>()

    @Volatile
    var isOpen: Boolean = true
        private set

    val certificatePin: String?
        get() = trustManager?.observedPin

    internal fun attach(socket: WebSocket) {
        webSocket = socket
    }

    internal fun onMessage(text: String) {
        val message = try {
            JSONObject(text)
        } catch (_: Exception) {
            return
        }
        val id = message.optString(ID)
        pending[id]?.trySend(message)
        subscriptions[id]?.let { handler -> message.optJSONObject(PAYLOAD)?.let(handler) }
    }

    internal fun onClosed() {
        isOpen = false
        pending.values.forEach { it.close() }
        pending.clear()
        subscriptions.clear()
    }

    suspend fun register(clientKey: String?, manifest: JSONObject, onPrompt: () -> Unit): String {
        val payload = JSONObject()
            .put("forcePairing", false)
            .put("pairingType", "PROMPT")
            .put("manifest", manifest)
        if (clientKey != null) payload.put(CLIENT_KEY, clientKey)

        return exchange(type = TYPE_REGISTER, uri = null, payload = payload, timeoutMillis = PAIRING_TIMEOUT_MILLIS) { message ->
            when (message.optString(TYPE)) {
                TYPE_REGISTERED -> message.optJSONObject(PAYLOAD)?.optString(CLIENT_KEY)?.ifBlank { null }
                    ?: throw TvConnectionException.PairingRejected("missing client key")
                TYPE_ERROR -> throw TvConnectionException.PairingRejected(message.optString(ERROR))
                else -> {
                    if (message.optJSONObject(PAYLOAD)?.optString("pairingType") == "PROMPT") onPrompt()
                    null
                }
            }
        }
    }

    suspend fun request(uri: String, payload: JSONObject? = null): JSONObject =
        exchange(type = TYPE_REQUEST, uri = uri, payload = payload, timeoutMillis = REQUEST_TIMEOUT_MILLIS) { message ->
            if (message.optString(TYPE) == TYPE_ERROR) {
                throw TvConnectionException.RequestFailed(message.optString(ERROR))
            }
            message.optJSONObject(PAYLOAD) ?: JSONObject()
        }

    // The TV keeps sending updates for this uri until the connection closes.
    fun subscribe(uri: String, onUpdate: (JSONObject) -> Unit) {
        val id = "${TYPE_SUBSCRIBE}_${nextId.incrementAndGet()}"
        subscriptions[id] = onUpdate
        val message = JSONObject().put(TYPE, TYPE_SUBSCRIBE).put(ID, id).put("uri", uri)
        if (!webSocket.send(message.toString())) {
            subscriptions.remove(id)
            throw TvConnectionException.ConnectionLost()
        }
    }

    private suspend fun <T : Any> exchange(
        type: String,
        uri: String?,
        payload: JSONObject?,
        timeoutMillis: Long,
        handle: (JSONObject) -> T?
    ): T {
        val id = "${type}_${nextId.incrementAndGet()}"
        val channel = Channel<JSONObject>(Channel.UNLIMITED)
        pending[id] = channel
        try {
            val message = JSONObject().put(TYPE, type).put(ID, id)
            if (uri != null) message.put("uri", uri)
            if (payload != null) message.put(PAYLOAD, payload)
            if (!webSocket.send(message.toString())) throw TvConnectionException.ConnectionLost()

            return withTimeout(timeoutMillis) {
                var result: T? = null
                while (result == null) result = handle(channel.receive())
                result
            }
        } catch (_: TimeoutCancellationException) {
            throw TvConnectionException.Timeout()
        } catch (_: ClosedReceiveChannelException) {
            throw TvConnectionException.ConnectionLost()
        } finally {
            pending.remove(id)
        }
    }

    override fun close() {
        if (::webSocket.isInitialized) webSocket.close(NORMAL_CLOSURE, null)
        onClosed()
    }

    private companion object {
        const val ID = "id"
        const val TYPE = "type"
        const val PAYLOAD = "payload"
        const val ERROR = "error"
        const val CLIENT_KEY = "client-key"
        const val TYPE_REGISTER = "register"
        const val TYPE_REGISTERED = "registered"
        const val TYPE_REQUEST = "request"
        const val TYPE_SUBSCRIBE = "subscribe"
        const val TYPE_ERROR = "error"
        const val NORMAL_CLOSURE = 1000
        const val PAIRING_TIMEOUT_MILLIS = 60_000L
        const val REQUEST_TIMEOUT_MILLIS = 10_000L
    }
}
