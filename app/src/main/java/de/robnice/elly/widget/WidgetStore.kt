package de.robnice.elly.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.remote.RemoteKey
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widgets")

class WidgetStore(context: Context) {

    private val dataStore = context.applicationContext.widgetDataStore

    suspend fun tvId(widgetId: Int): String? = dataStore.data.first()[tvKey(widgetId)]

    suspend fun setTv(widgetId: Int, tvId: String) {
        dataStore.edit { it[tvKey(widgetId)] = tvId }
    }

    suspend fun options(widgetId: Int, defaultTvId: String?): AllWidgetOptions {
        val prefs = dataStore.data.first()
        val defaults = AllWidgetOptions(tvId = prefs[tvKey(widgetId)] ?: defaultTvId)
        return defaults.copy(
            keys = prefs[keysKey(widgetId)]?.mapNotNull { name -> RemoteKey.entries.firstOrNull { it.name == name } }?.toSet()
                ?: defaults.keys,
            columns = prefs[columnsKey(widgetId)] ?: defaults.columns,
            showName = prefs[showNameKey(widgetId)] ?: defaults.showName,
            rowBackground = prefs[rowBackgroundKey(widgetId)] ?: defaults.rowBackground,
            panelColor = prefs[panelColorKey(widgetId)]?.let { name -> WidgetPanelColor.entries.firstOrNull { it.name == name } }
                ?: defaults.panelColor,
            panelOpacity = prefs[panelOpacityKey(widgetId)] ?: defaults.panelOpacity
        )
    }

    suspend fun setOptions(widgetId: Int, options: AllWidgetOptions) {
        dataStore.edit {
            options.tvId?.let { tvId -> it[tvKey(widgetId)] = tvId }
            it[keysKey(widgetId)] = options.keys.map { key -> key.name }.toSet()
            it[columnsKey(widgetId)] = options.columns
            it[showNameKey(widgetId)] = options.showName
            it[rowBackgroundKey(widgetId)] = options.rowBackground
            it[panelColorKey(widgetId)] = options.panelColor.name
            it[panelOpacityKey(widgetId)] = options.panelOpacity
        }
    }

    suspend fun remove(widgetId: Int) {
        dataStore.edit {
            it.remove(tvKey(widgetId))
            it.remove(keysKey(widgetId))
            it.remove(columnsKey(widgetId))
            it.remove(showNameKey(widgetId))
            it.remove(rowBackgroundKey(widgetId))
            it.remove(panelColorKey(widgetId))
            it.remove(panelOpacityKey(widgetId))
        }
    }

    private fun tvKey(widgetId: Int) = stringPreferencesKey("tv_$widgetId")

    private fun keysKey(widgetId: Int) = stringSetPreferencesKey("keys_$widgetId")

    private fun columnsKey(widgetId: Int) = intPreferencesKey("columns_$widgetId")

    private fun showNameKey(widgetId: Int) = booleanPreferencesKey("show_name_$widgetId")

    private fun rowBackgroundKey(widgetId: Int) = booleanPreferencesKey("row_background_$widgetId")

    private fun panelColorKey(widgetId: Int) = stringPreferencesKey("panel_color_$widgetId")

    private fun panelOpacityKey(widgetId: Int) = intPreferencesKey("panel_opacity_$widgetId")
}
