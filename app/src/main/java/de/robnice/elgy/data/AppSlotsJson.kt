package de.robnice.elgy.data

import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.AppSlots
import org.json.JSONArray
import org.json.JSONObject

object AppSlotsJson {

    private const val ENABLED = "enabled"
    private const val APP_ID = "appId"
    private const val TITLE = "title"
    private const val ICON_PATH = "iconPath"
    private const val SHOW_ICON = "showIcon"
    private const val SHOW_TEXT = "showText"
    private const val CUSTOM_TEXT = "customText"
    private const val TEXT_COLOR = "textColor"

    fun decodeAll(raw: String?): Map<String, List<AppSlot>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { owner -> decodeSlots(json.optJSONArray(owner)) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeAll(slotsByOwner: Map<String, List<AppSlot>>): String {
        val json = JSONObject()
        slotsByOwner.forEach { (owner, slots) -> json.put(owner, encodeSlots(slots)) }
        return json.toString()
    }

    private fun encodeSlots(slots: List<AppSlot>): JSONArray {
        val array = JSONArray()
        slots.forEach { slot ->
            array.put(
                JSONObject()
                    .put(ENABLED, slot.enabled)
                    .putOpt(APP_ID, slot.appId)
                    .putOpt(TITLE, slot.title)
                    .putOpt(ICON_PATH, slot.iconPath)
                    .put(SHOW_ICON, slot.showIcon)
                    .put(SHOW_TEXT, slot.showText)
                    .putOpt(CUSTOM_TEXT, slot.customText)
                    .putOpt(TEXT_COLOR, slot.textColor)
            )
        }
        return array
    }

    private fun decodeSlots(array: JSONArray?): List<AppSlot> {
        if (array == null) return AppSlots.DEFAULT
        val slots = (0 until minOf(array.length(), AppSlots.COUNT)).map { index ->
            val json = array.optJSONObject(index) ?: return@map AppSlots.EMPTY
            AppSlot(
                enabled = json.optBoolean(ENABLED, false),
                appId = json.optStringOrNull(APP_ID),
                title = json.optStringOrNull(TITLE),
                iconPath = json.optStringOrNull(ICON_PATH),
                showIcon = json.optBoolean(SHOW_ICON, true),
                showText = json.optBoolean(SHOW_TEXT, false),
                customText = json.optStringOrNull(CUSTOM_TEXT),
                textColor = if (json.has(TEXT_COLOR) && !json.isNull(TEXT_COLOR)) json.optLong(TEXT_COLOR) else null
            )
        }
        return slots + List(AppSlots.COUNT - slots.size) { AppSlots.EMPTY }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).ifBlank { null } else null
}
