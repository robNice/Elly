package de.robnice.elgy.tv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpMessagesTest {

    private val webOsResponse = listOf(
        "HTTP/1.1 200 OK",
        "CACHE-CONTROL: max-age=1800",
        "LOCATION: http://192.168.0.20:1754/",
        "ST: urn:lge-com:service:webos-second-screen:1",
        "USN: uuid:abc::urn:lge-com:service:webos-second-screen:1",
        ""
    ).joinToString("\r\n")

    @Test
    fun parseHeaders_readsHeadersCaseInsensitive() {
        val headers = SsdpMessages.parseHeaders(webOsResponse)

        assertEquals("http://192.168.0.20:1754/", headers["location"])
        assertEquals("max-age=1800", headers["cache-control"])
    }

    @Test
    fun isWebOsResponse_detectsWebOsSearchTarget() {
        assertTrue(SsdpMessages.isWebOsResponse(SsdpMessages.parseHeaders(webOsResponse)))
    }

    @Test
    fun isWebOsResponse_ignoresOtherDevices() {
        val headers = mapOf("st" to "urn:schemas-upnp-org:device:MediaRenderer:1")

        assertFalse(SsdpMessages.isWebOsResponse(headers))
    }

    @Test
    fun mSearch_containsSearchTarget() {
        val message = String(SsdpMessages.mSearch(), Charsets.UTF_8)

        assertTrue(message.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(message.contains("ST: urn:lge-com:service:webos-second-screen:1\r\n"))
        assertTrue(message.endsWith("\r\n\r\n"))
    }
}
