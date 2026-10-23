package de.robnice.elly.tv.pairing

import org.json.JSONObject

object MacAddressSelector {

    private val MAC_PATTERN = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

    fun select(networkInfo: JSONObject): String? {
        val interfaces = listOfNotNull(
            networkInfo.optJSONObject("wiredInfo"),
            networkInfo.optJSONObject("wifiInfo")
        )
        val ordered = interfaces.sortedByDescending { it.optString("state") == "connected" }
        return ordered.firstNotNullOfOrNull { normalize(it.optString("macAddress")) }
    }

    fun normalize(raw: String?): String? {
        val mac = raw?.trim()?.uppercase()?.replace('-', ':') ?: return null
        return mac.takeIf { MAC_PATTERN.matches(it) }
    }
}
