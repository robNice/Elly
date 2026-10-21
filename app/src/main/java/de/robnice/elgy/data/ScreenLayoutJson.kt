package de.robnice.elgy.data

import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenEntry
import de.robnice.elgy.data.model.ScreenLayout
import org.json.JSONArray
import org.json.JSONObject

object ScreenLayoutJson {

    private const val SCREEN = "screen"
    private const val ENABLED = "enabled"

    fun decodeAll(raw: String?): Map<String, ScreenLayout> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { owner -> decode(json.optJSONArray(owner)) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeAll(layouts: Map<String, ScreenLayout>): String {
        val json = JSONObject()
        layouts.forEach { (owner, layout) ->
            val array = JSONArray()
            layout.entries.forEach { array.put(JSONObject().put(SCREEN, it.screen.name).put(ENABLED, it.enabled)) }
            json.put(owner, array)
        }
        return json.toString()
    }

    private fun decode(array: JSONArray?): ScreenLayout {
        if (array == null) return ScreenLayout.DEFAULT
        val entries = (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val screen = runCatching { RemoteScreen.valueOf(json.optString(SCREEN)) }.getOrNull() ?: return@mapNotNull null
            ScreenEntry(screen, json.optBoolean(ENABLED, true))
        }
        return ScreenLayout.normalize(entries)
    }
}
