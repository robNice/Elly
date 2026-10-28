package de.robnice.elly.data

import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.KeyRounding
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenBackground
import de.robnice.elly.data.model.ScreenLayout
import org.json.JSONObject
import java.util.Base64

sealed interface Backup {

    // Only screens changed from their factory layout are included.
    data class Screens(val layout: ScreenLayout, val screens: Map<RemoteScreen, CustomScreen>) : Backup

    class Appearance(
        val palette: ColorPalette,
        val background: ScreenBackground,
        val image: ByteArray?,
        val keyRounding: Float,
        val dpadRounding: Float,
        val lightEffect: Boolean
    ) : Backup
}

// What a backup file holds: the screens, the appearance or both.
class BackupContent(val screens: Backup.Screens?, val appearance: Backup.Appearance?) {
    val isEmpty get() = screens == null && appearance == null
}

// A backup file holds the screens, the appearance or both, so one can be passed on without the other.
// Older files hold only one of them, each in a format of its own.
object BackupJson {

    private const val FORMAT = "format"
    private const val VERSION = "version"
    private const val BACKUP_FORMAT = "elly-backup"
    private const val SCREENS_FORMAT = "elly-screens"
    private const val APPEARANCE_FORMAT = "elly-appearance"
    private const val CURRENT_VERSION = 1
    private const val LAYOUT = "layout"
    private const val SCREENS = "screens"
    private const val APPEARANCE = "appearance"
    private const val PALETTE = "palette"
    private const val BACKGROUND = "background"
    private const val IMAGE = "image"
    private const val KEY_ROUNDING = "keyRounding"
    private const val DPAD_ROUNDING = "dpadRounding"
    private const val LIGHT_EFFECT = "lightEffect"

    fun encode(content: BackupContent): String {
        val json = JSONObject().put(FORMAT, BACKUP_FORMAT).put(VERSION, CURRENT_VERSION)
        content.screens?.let { json.put(SCREENS, encodeScreens(it)) }
        content.appearance?.let { json.put(APPEARANCE, encodeAppearance(it)) }
        return json.toString()
    }

    fun decode(raw: String): BackupContent? = try {
        val json = JSONObject(raw)
        when (json.optString(FORMAT)) {
            BACKUP_FORMAT -> BackupContent(
                screens = json.optJSONObject(SCREENS)?.let(::decodeScreens),
                appearance = json.optJSONObject(APPEARANCE)?.let(::decodeAppearance)
            ).takeUnless { it.isEmpty }
            SCREENS_FORMAT -> BackupContent(decodeScreens(json), null)
            APPEARANCE_FORMAT -> BackupContent(null, decodeAppearance(json))
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun encodeScreens(backup: Backup.Screens): JSONObject = JSONObject()
        .put(LAYOUT, ScreenLayoutJson.encode(backup.layout))
        .put(SCREENS, CustomScreenJson.encodeOwner(withoutIcons(backup.screens)))

    private fun encodeAppearance(backup: Backup.Appearance): JSONObject = JSONObject()
        .put(PALETTE, backup.palette.name)
        .put(BACKGROUND, backup.background.name)
        .putOpt(IMAGE, backup.image?.let { Base64.getEncoder().encodeToString(it) })
        .put(KEY_ROUNDING, backup.keyRounding.toDouble())
        .put(DPAD_ROUNDING, backup.dpadRounding.toDouble())
        .put(LIGHT_EFFECT, backup.lightEffect)

    private fun decodeScreens(json: JSONObject) = Backup.Screens(
        layout = ScreenLayoutJson.decode(json.optJSONArray(LAYOUT)),
        screens = withoutIcons(CustomScreenJson.decodeOwner(json.optJSONObject(SCREENS)))
    )

    private fun decodeAppearance(json: JSONObject) = Backup.Appearance(
        palette = runCatching { ColorPalette.valueOf(json.optString(PALETTE)) }.getOrDefault(ColorPalette.GRAPHITE),
        background = runCatching { ScreenBackground.valueOf(json.optString(BACKGROUND)) }.getOrDefault(ScreenBackground.NONE),
        image = json.optString(IMAGE).takeIf { it.isNotEmpty() }?.let { Base64.getDecoder().decode(it) },
        keyRounding = json.optDouble(KEY_ROUNDING, KeyRounding.DEFAULT.toDouble()).toFloat().coerceIn(KeyRounding.RANGE),
        dpadRounding = json.optDouble(DPAD_ROUNDING, KeyRounding.DEFAULT.toDouble()).toFloat().coerceIn(KeyRounding.RANGE),
        lightEffect = json.optBoolean(LIGHT_EFFECT, false)
    )

    // Icon paths point into this phone's cache; the app fetches the icons from the TV again.
    private fun withoutIcons(screens: Map<RemoteScreen, CustomScreen>): Map<RemoteScreen, CustomScreen> =
        screens.mapValues { (_, screen) ->
            CustomScreen(screen.elements.map { element -> element.copy(app = element.app?.copy(iconPath = null)) })
        }
}
