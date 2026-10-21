package de.robnice.elgy.data

import de.robnice.elgy.data.model.PairedTv
import org.json.JSONArray
import org.json.JSONObject

object PairedTvJson {

    private const val ID = "id"
    private const val IP_ADDRESS = "ipAddress"
    private const val NAME = "name"
    private const val MODEL_NAME = "modelName"
    private const val CLIENT_KEY = "clientKey"
    private const val MAC_ADDRESS = "macAddress"
    private const val CERTIFICATE_PIN = "certificatePin"
    private const val SECURE = "secure"

    fun encode(tvs: List<PairedTv>): String {
        val array = JSONArray()
        tvs.forEach { array.put(toJson(it)) }
        return array.toString()
    }

    fun decode(raw: String?): List<PairedTv> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { fromJson(array.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun toJson(tv: PairedTv) = JSONObject()
        .put(ID, tv.id)
        .put(IP_ADDRESS, tv.ipAddress)
        .put(NAME, tv.name)
        .putOpt(MODEL_NAME, tv.modelName)
        .put(CLIENT_KEY, tv.clientKey)
        .putOpt(MAC_ADDRESS, tv.macAddress)
        .putOpt(CERTIFICATE_PIN, tv.certificatePin)
        .put(SECURE, tv.secure)

    private fun fromJson(json: JSONObject): PairedTv? {
        val id = json.optString(ID).ifBlank { return null }
        val ipAddress = json.optString(IP_ADDRESS).ifBlank { return null }
        val clientKey = json.optString(CLIENT_KEY).ifBlank { return null }
        return PairedTv(
            id = id,
            ipAddress = ipAddress,
            name = json.optString(NAME).ifBlank { ipAddress },
            modelName = json.optStringOrNull(MODEL_NAME),
            clientKey = clientKey,
            macAddress = json.optStringOrNull(MAC_ADDRESS),
            certificatePin = json.optStringOrNull(CERTIFICATE_PIN),
            secure = json.optBoolean(SECURE, true)
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).ifBlank { null } else null
}
