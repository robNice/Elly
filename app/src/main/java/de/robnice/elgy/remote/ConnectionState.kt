package de.robnice.elgy.remote

enum class ConnectionState {
    CHECKING,
    CONNECTED,
    DISCONNECTED,
    DEMO;

    companion object {
        // A failed request still means the TV answered, so the connection itself is fine.
        fun after(result: SendResult): ConnectionState? = when (result) {
            SendResult.SENT, SendResult.FAILED -> CONNECTED
            SendResult.UNREACHABLE, SendResult.PAIRING_REQUIRED, SendResult.NO_TV -> DISCONNECTED
            SendResult.DEMO -> DEMO
            SendResult.WAKE_SENT, SendResult.UNSUPPORTED -> null
        }
    }
}
