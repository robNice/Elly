package de.robnice.elgy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    suspend fun update(transform: (ScreenLayout) -> ScreenLayout) {
        val owner = activeOwner.first() ?: return
        dataStore.edit { preferences ->
            val all = ScreenLayoutJson.decodeAll(preferences[LAYOUTS]).toMutableMap()
            all[owner] = transform(all[owner] ?: ScreenLayout.DEFAULT)
            preferences[LAYOUTS] = ScreenLayoutJson.encodeAll(all)
        }
    }

    suspend fun removeOwner(owner: String) {
        dataStore.edit { preferences ->
            val all = ScreenLayoutJson.decodeAll(preferences[LAYOUTS])
            if (owner in all) preferences[LAYOUTS] = ScreenLayoutJson.encodeAll(all - owner)
        }
    }

    private companion object {
        val LAYOUTS = stringPreferencesKey("layouts_by_owner")
    }
}
