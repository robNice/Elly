package de.robnice.elly.data

import de.robnice.elly.data.model.AppSlot
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

    fun encodeSlot(slot: AppSlot): JSONObject = JSONObject()
        .put(ENABLED, slot.enabled)
        .putOpt(APP_ID, slot.appId)
        .putOpt(TITLE, slot.title)
        .putOpt(ICON_PATH, slot.iconPath)
        .put(SHOW_ICON, slot.showIcon)
        .put(SHOW_TEXT, slot.showText)
        .putOpt(CUSTOM_TEXT, slot.customText)
        .putOpt(TEXT_COLOR, slot.textColor)

    fun decodeSlot(json: JSONObject): AppSlot = AppSlot(
        enabled = json.optBoolean(ENABLED, false),
        appId = json.optStringOrNull(APP_ID),
        title = json.optStringOrNull(TITLE),
        iconPath = json.optStringOrNull(ICON_PATH),
        showIcon = json.optBoolean(SHOW_ICON, true),
        showText = json.optBoolean(SHOW_TEXT, false),
        customText = json.optStringOrNull(CUSTOM_TEXT),
        textColor = if (json.has(TEXT_COLOR) && !json.isNull(TEXT_COLOR)) json.optLong(TEXT_COLOR) else null
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).ifBlank { null } else null
}
