package de.robnice.elly.widget

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.robnice.elly.R
import de.robnice.elly.remote.RemoteKey

// SYSTEM follows the wallpaper colors (Android 12+) and light/dark mode; the others are fixed.
enum class WidgetPanelColor(@param:ColorRes val color: Int?, @param:StringRes val title: Int) {
    SYSTEM(null, R.string.widget_color_system),
    BLACK(R.color.widget_panel_black, R.string.widget_color_black),
    WHITE(R.color.widget_panel_white, R.string.widget_color_white),
    CREAM(R.color.widget_panel_cream, R.string.widget_color_cream),
    ORANGE(R.color.widget_panel_orange, R.string.widget_color_orange),
    BLUE(R.color.widget_panel_blue, R.string.widget_color_blue),
    GREEN(R.color.widget_panel_green, R.string.widget_color_green),
    RED(R.color.widget_panel_red, R.string.widget_color_red)
}

data class AllWidgetOptions(
    val tvId: String?,
    val keys: Set<RemoteKey> = AllWidgetLayout.keys.toSet(),
    val columns: Int = AllWidgetLayout.DEFAULT_COLUMNS,
    val showName: Boolean = false,
    val rowBackground: Boolean = true,
    val panelColor: WidgetPanelColor = WidgetPanelColor.SYSTEM,
    val panelOpacity: Int = DEFAULT_OPACITY
) {
    companion object {
        const val DEFAULT_OPACITY = 50
    }
}
