package de.robnice.elly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.data.model.WatchLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.watchLayoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_layout")

// Pages and key functions of the watch app. One layout for all TVs: the watch can switch between them, and
// app IDs are the same on every LG TV.
class WatchLayoutRepository(context: Context) {

    private val dataStore = context.applicationContext.watchLayoutDataStore

    val layout: Flow<WatchLayout> = dataStore.data.map { WatchLayoutJson.decode(it[LAYOUT]) }

    suspend fun update(transform: (WatchLayout) -> WatchLayout) {
        dataStore.edit { it[LAYOUT] = WatchLayoutJson.encode(transform(WatchLayoutJson.decode(it[LAYOUT]))) }
    }

    private companion object {
        val LAYOUT = stringPreferencesKey("layout")
    }
}
