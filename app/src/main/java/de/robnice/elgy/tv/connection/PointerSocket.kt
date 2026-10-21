package de.robnice.elgy.tv.connection

import okhttp3.WebSocket
import java.io.Closeable

class PointerSocket internal constructor() : Closeable {

    private lateinit var webSocket: WebSocket

    @Volatile
    var isOpen: Boolean = true
        private set

    internal fun attach(socket: WebSocket) {
        webSocket = socket
    }

    internal fun onClosed() {
        isOpen = false
    }

    fun button(name: String): Boolean = send("type:button\nname:$name\n\n")

    fun move(dx: Int, dy: Int): Boolean = send("type:move\ndx:$dx\ndy:$dy\ndown:0\n\n")

    fun click(): Boolean = send("type:click\n\n")

    fun scroll(dy: Int): Boolean = send("type:scroll\ndx:0\ndy:$dy\n\n")

    private fun send(message: String): Boolean = isOpen && webSocket.send(message)

    override fun close() {
        if (::webSocket.isInitialized) webSocket.close(NORMAL_CLOSURE, null)
        onClosed()
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
