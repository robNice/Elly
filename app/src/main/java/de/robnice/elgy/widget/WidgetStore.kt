package de.robnice.elgy.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widgets")

class WidgetStore(context: Context) {

    private val dataStore = context.applicationContext.widgetDataStore

    suspend fun tvId(widgetId: Int): String? = dataStore.data.first()[tvKey(widgetId)]

    suspend fun setTv(widgetId: Int, tvId: String) {
        dataStore.edit { it[tvKey(widgetId)] = tvId }
    }

    suspend fun remove(widgetId: Int) {
        dataStore.edit { it.remove(tvKey(widgetId)) }
    }

    private fun tvKey(widgetId: Int) = stringPreferencesKey("tv_$widgetId")
}
