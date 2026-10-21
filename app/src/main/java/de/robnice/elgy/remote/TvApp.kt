package de.robnice.elgy.remote

import org.json.JSONObject

data class TvApp(
    val id: String,
    val title: String,
    val iconUrl: String? = null
)

data class AppListResult(
    val result: SendResult,
    val apps: List<TvApp> = emptyList()
)

object TvAppParser {

    fun parseLaunchPoints(payload: JSONObject): List<TvApp> {
        val launchPoints = payload.optJSONArray("launchPoints") ?: return emptyList()
        return (0 until launchPoints.length())
            .mapNotNull { index ->
                val point = launchPoints.optJSONObject(index) ?: return@mapNotNull null
                val id = point.optString("id").ifBlank { return@mapNotNull null }
                TvApp(
                    id = id,
                    title = point.optString("title").ifBlank { id },
                    iconUrl = point.optString("largeIcon").ifBlank { point.optString("icon") }.ifBlank { null }
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }
    }
}
