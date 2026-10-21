package de.robnice.elgy.remote

import org.json.JSONObject

data class TvInput(
    val id: String,
    val label: String,
    val connected: Boolean
)

data class InputListResult(
    val result: SendResult,
    val inputs: List<TvInput> = emptyList()
)

object TvInputParser {

    fun parse(payload: JSONObject): List<TvInput> {
        val devices = payload.optJSONArray("devices") ?: return emptyList()
        return (0 until devices.length()).mapNotNull { index ->
            val device = devices.optJSONObject(index) ?: return@mapNotNull null
            val id = device.optString("id").ifBlank { return@mapNotNull null }
            TvInput(
                id = id,
                label = device.optString("label").ifBlank { id },
                connected = device.optBoolean("connected", false)
            )
        }
    }
}
