package de.robnice.elly.tv.discovery

object SsdpMessages {

    const val MULTICAST_ADDRESS = "239.255.255.250"
    const val PORT = 1900
    const val WEBOS_SEARCH_TARGET = "urn:lge-com:service:webos-second-screen:1"

    fun mSearch(searchTarget: String = WEBOS_SEARCH_TARGET): ByteArray = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $MULTICAST_ADDRESS:$PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 2\r\n")
        append("ST: $searchTarget\r\n")
        append("\r\n")
    }.toByteArray(Charsets.UTF_8)

    fun parseHeaders(raw: String): Map<String, String> =
        raw.split("\r\n", "\n")
            .drop(1)
            .mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) null
                else line.substring(0, separator).trim().lowercase() to line.substring(separator + 1).trim()
            }
            .toMap()

    fun isWebOsResponse(headers: Map<String, String>): Boolean =
        headers["st"]?.contains(WEBOS_SEARCH_TARGET, ignoreCase = true) == true ||
                headers["usn"]?.contains(WEBOS_SEARCH_TARGET, ignoreCase = true) == true
}
