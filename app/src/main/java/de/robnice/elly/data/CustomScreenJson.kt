package de.robnice.elly.data

import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.remote.RemoteKey
import org.json.JSONArray
import org.json.JSONObject

object CustomScreenJson {

    private const val ID = "id"
    private const val TYPE = "type"
    private const val X = "x"
    private const val Y = "y"
    private const val WIDTH = "w"
    private const val HEIGHT = "h"
    private const val KEY = "key"
    private const val APP = "app"
    private const val COLUMNS = "columns"
    private const val ROWS = "rows"
    private const val ELEMENTS = "elements"
    private const val SCREENS = "screens"

    // Earlier formats: a bare array for the custom screen on 8 x 12, then one object on 24 x 36.
    private const val LEGACY_COLUMNS = 8
    private const val LEGACY_ROWS = 12
    private const val FIRST_FINE_ROWS = 36

    fun decodeAll(raw: String?): Map<String, Map<RemoteScreen, CustomScreen>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { owner -> decodeOwner(json.opt(owner)) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeAll(screens: Map<String, Map<RemoteScreen, CustomScreen>>): String {
        val json = JSONObject()
        screens.forEach { (owner, byScreen) ->
            val encoded = JSONObject()
            byScreen.forEach { (screen, layout) -> encoded.put(screen.name, encode(layout)) }
            json.put(
                owner,
                JSONObject().put(COLUMNS, CustomScreen.COLUMNS).put(ROWS, CustomScreen.ROWS).put(SCREENS, encoded)
            )
        }
        return json.toString()
    }

    private fun decodeOwner(value: Any?): Map<RemoteScreen, CustomScreen> = when (value) {
        is JSONArray -> mapOf(RemoteScreen.CUSTOM to decode(value, LEGACY_COLUMNS, LEGACY_ROWS))
        is JSONObject -> {
            val columns = value.optInt(COLUMNS, CustomScreen.COLUMNS)
            val rows = value.optInt(ROWS, FIRST_FINE_ROWS)
            val screens = value.optJSONObject(SCREENS)
            if (screens == null) {
                mapOf(RemoteScreen.CUSTOM to decode(value.optJSONArray(ELEMENTS), columns, rows))
            } else {
                screens.keys().asSequence().mapNotNull { name ->
                    val screen = runCatching { RemoteScreen.valueOf(name) }.getOrNull() ?: return@mapNotNull null
                    screen to decode(screens.optJSONArray(name), columns, rows)
                }.toMap()
            }
        }
        else -> emptyMap()
    }

    private fun encode(layout: CustomScreen): JSONArray {
        val array = JSONArray()
        layout.elements.forEach { element ->
            val item = JSONObject()
                .put(ID, element.id)
                .put(TYPE, element.type.name)
                .put(X, element.x)
                .put(Y, element.y)
                .put(WIDTH, element.width)
                .put(HEIGHT, element.height)
            element.key?.let { item.put(KEY, it.name) }
            element.app?.let { item.put(APP, AppSlotsJson.encodeSlot(it)) }
            array.put(item)
        }
        return array
    }

    private fun decode(array: JSONArray?, columns: Int, rows: Int): CustomScreen {
        if (array == null) return CustomScreen.EMPTY
        val scaleX = if (columns > 0) CustomScreen.COLUMNS.toFloat() / columns else 1f
        val scaleY = if (rows > 0) CustomScreen.ROWS.toFloat() / rows else 1f
        val elements = (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val type = runCatching { CustomElementType.valueOf(json.optString(TYPE)) }.getOrNull() ?: return@mapNotNull null
            CustomElement(
                id = json.optLong(ID),
                type = type,
                x = json.optInt(X),
                y = json.optInt(Y),
                width = json.optInt(WIDTH),
                height = json.optInt(HEIGHT),
                key = json.optString(KEY).takeIf { it.isNotEmpty() }
                    ?.let { runCatching { RemoteKey.valueOf(it) }.getOrNull() },
                app = json.optJSONObject(APP)?.let { AppSlotsJson.decodeSlot(it) }
            ).scaled(scaleX, scaleY)
        }
        return CustomScreen.normalize(elements)
    }
}
