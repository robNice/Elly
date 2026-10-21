package de.robnice.elgy.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun answeredRequests_meanConnected() {
        assertEquals(ConnectionState.CONNECTED, ConnectionState.after(SendResult.SENT))
        assertEquals(ConnectionState.CONNECTED, ConnectionState.after(SendResult.FAILED))
    }

    @Test
    fun unreachableOrRejected_meanDisconnected() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.after(SendResult.UNREACHABLE))
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.after(SendResult.PAIRING_REQUIRED))
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.after(SendResult.NO_TV))
    }

    @Test
    fun resultsWithoutConnectionInfo_keepTheState() {
        assertNull(ConnectionState.after(SendResult.WAKE_SENT))
        assertNull(ConnectionState.after(SendResult.UNSUPPORTED))
    }
}
