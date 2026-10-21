package de.robnice.elgy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.data.model.DefaultScreens
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.screenLayoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "screen_layout")

class ScreenLayoutRepository(context: Context, private val activeOwner: Flow<String?>) {

    private val dataStore = context.applicationContext.screenLayoutDataStore

    private val allLayouts: Flow<Map<String, ScreenLayout>> =
        dataStore.data.map { ScreenLayoutJson.decodeAll(it[LAYOUTS]) }

    val activeLayout: Flow<ScreenLayout> = combine(allLayouts, activeOwner) { layouts, owner ->
        owner?.let { layouts[it] } ?: ScreenLayout.DEFAULT
    }

    // Screens without a stored layout show their factory layout, so a reset only removes the entry.
    val activeScreens: Flow<Map<RemoteScreen, CustomScreen>> = combine(
        dataStore.data.map { CustomScreenJson.decodeAll(it[CUSTOM_SCREENS]) },
        activeOwner
    ) { all, owner ->
        val stored = owner?.let { all[it] }.orEmpty()
        RemoteScreen.entries.associateWith { stored[it] ?: DefaultScreens.layout(it) }
    }

    suspend fun update(transform: (ScreenLayout) -> ScreenLayout) {
        val owner = activeOwner.first() ?: return
        dataStore.edit { preferences ->
            val all = ScreenLayoutJson.decodeAll(preferences[LAYOUTS]).toMutableMap()
            all[owner] = transform(all[owner] ?: ScreenLayout.DEFAULT)
            preferences[LAYOUTS] = ScreenLayoutJson.encodeAll(all)
        }
    }

    suspend fun updateScreen(screen: RemoteScreen, transform: (CustomScreen) -> CustomScreen) {
        editScreens { screens -> screens[screen] = transform(screens[screen] ?: DefaultScreens.layout(screen)) }
    }

    suspend fun resetScreen(screen: RemoteScreen) {
        editScreens { screens -> screens.remove(screen) }
    }

    private suspend fun editScreens(change: (MutableMap<RemoteScreen, CustomScreen>) -> Unit) {
        val owner = activeOwner.first() ?: return
        dataStore.edit { preferences ->
            val all = CustomScreenJson.decodeAll(preferences[CUSTOM_SCREENS]).toMutableMap()
            all[owner] = all[owner].orEmpty().toMutableMap().also(change)
            preferences[CUSTOM_SCREENS] = CustomScreenJson.encodeAll(all)
        }
    }

    suspend fun removeOwner(owner: String) {
        dataStore.edit { preferences ->
            val all = ScreenLayoutJson.decodeAll(preferences[LAYOUTS])
            if (owner in all) preferences[LAYOUTS] = ScreenLayoutJson.encodeAll(all - owner)
            val custom = CustomScreenJson.decodeAll(preferences[CUSTOM_SCREENS])
            if (owner in custom) preferences[CUSTOM_SCREENS] = CustomScreenJson.encodeAll(custom - owner)
        }
    }

    private companion object {
        val LAYOUTS = stringPreferencesKey("layouts_by_owner")
        val CUSTOM_SCREENS = stringPreferencesKey("custom_screens_by_owner")
    }
}
