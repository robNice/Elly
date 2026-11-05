package de.robnice.elly.data

import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchPage
import de.robnice.elly.data.model.WatchPageEntry
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import org.json.JSONArray
import org.json.JSONObject

// Only changed keys are stored, so a later default reaches users who kept it. Unknown names from a newer or
// older version are skipped instead of failing the whole layout.
object WatchLayoutJson {

    private const val PAGES = "pages"
    private const val PAGE = "page"
    private const val VISIBLE = "visible"
    private const val ACTIONS = "actions"
    private const val KEY = "key"
    private const val APP = "app"
    private const val TITLE = "title"
    private const val PLAY_PAUSE = "playPause"
    private const val DISPLAY = "display"

    fun encode(layout: WatchLayout): String {
        val pages = JSONArray()
        layout.pages.forEach { pages.put(JSONObject().put(PAGE, it.page.name).put(VISIBLE, it.visible)) }
        val actions = JSONObject()
        layout.assigned.forEach { (slot, action) -> actions.put(slot.name, toJson(action)) }
        return JSONObject().put(PAGES, pages).put(ACTIONS, actions).toString()
    }

    fun decode(raw: String?): WatchLayout {
        if (raw.isNullOrBlank()) return WatchLayout()
        return try {
            val json = JSONObject(raw)
            val pageArray = json.optJSONArray(PAGES) ?: JSONArray()
            val stored = (0 until pageArray.length()).mapNotNull { index ->
                val item = pageArray.getJSONObject(index)
                enumOrNull<WatchPage>(item.optString(PAGE))?.let { WatchPageEntry(it, item.optBoolean(VISIBLE, true)) }
            }.distinctBy { it.page }
            // Pages added in a later version are appended and shown.
            val pages = stored + WatchPage.entries.filter { page -> stored.none { it.page == page } }.map { WatchPageEntry(it, true) }
            var layout = WatchLayout(pages = if (pages.any { it.visible }) pages else WatchLayout().pages)
            val actions = json.optJSONObject(ACTIONS) ?: JSONObject()
            actions.keys().forEach { name ->
                val slot = enumOrNull<WatchSlot>(name) ?: return@forEach
                fromJson(actions.optJSONObject(name))?.let { layout = layout.with(slot, it) }
            }
            layout
        } catch (_: Exception) {
            WatchLayout()
        }
    }

    private fun toJson(action: WatchAction): JSONObject = when (action) {
        is WatchAction.Key -> JSONObject().put(KEY, action.key.name)
        WatchAction.PlayPause -> JSONObject().put(PLAY_PAUSE, true)
        is WatchAction.App -> JSONObject().put(APP, action.appId).put(TITLE, action.title).put(DISPLAY, action.display.name)
    }

    private fun fromJson(json: JSONObject?): WatchAction? = when {
        json == null -> null
        json.has(KEY) -> enumOrNull<RemoteKey>(json.optString(KEY))?.takeIf { it in WatchAction.ASSIGNABLE_KEYS }?.let { WatchAction.Key(it) }
        json.optBoolean(PLAY_PAUSE) -> WatchAction.PlayPause
        json.has(APP) -> json.optString(APP).takeIf { it.isNotBlank() }?.let {
            WatchAction.App(it, json.optString(TITLE), enumOrNull<AppDisplay>(json.optString(DISPLAY)) ?: AppDisplay.ICON)
        }
        else -> null
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? = enumValues<T>().firstOrNull { it.name == name }
}
