package de.robnice.elgy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elgy.data.model.PairedTv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tvDataStore: DataStore<Preferences> by preferencesDataStore(name = "tvs")

class TvRepository(context: Context) {

    private val dataStore = context.applicationContext.tvDataStore

    val pairedTvs: Flow<List<PairedTv>> = dataStore.data.map { PairedTvJson.decode(it[PAIRED_TVS]) }

    val activeTvId: Flow<String?> = dataStore.data.map { it[ACTIVE_TV_ID] }

    val activeTv: Flow<PairedTv?> = dataStore.data.map { preferences ->
        val tvs = PairedTvJson.decode(preferences[PAIRED_TVS])
        tvs.firstOrNull { it.id == preferences[ACTIVE_TV_ID] } ?: tvs.firstOrNull()
    }

    suspend fun findByIp(ipAddress: String): PairedTv? =
        pairedTvs.first().firstOrNull { it.ipAddress == ipAddress }

    suspend fun saveAndActivate(tv: PairedTv) {
        dataStore.edit { preferences ->
            val others = PairedTvJson.decode(preferences[PAIRED_TVS])
                .filterNot { it.id == tv.id || it.ipAddress == tv.ipAddress }
            preferences[PAIRED_TVS] = PairedTvJson.encode(others + tv)
            preferences[ACTIVE_TV_ID] = tv.id
        }
    }

    suspend fun setActive(id: String) {
        dataStore.edit { it[ACTIVE_TV_ID] = id }
    }

    suspend fun remove(id: String) {
        dataStore.edit { preferences ->
            val remaining = PairedTvJson.decode(preferences[PAIRED_TVS]).filterNot { it.id == id }
            preferences[PAIRED_TVS] = PairedTvJson.encode(remaining)
            if (preferences[ACTIVE_TV_ID] == id) {
                val next = remaining.firstOrNull()
                if (next != null) preferences[ACTIVE_TV_ID] = next.id else preferences.remove(ACTIVE_TV_ID)
            }
        }
    }

    private companion object {
        val PAIRED_TVS = stringPreferencesKey("paired_tvs")
        val ACTIVE_TV_ID = stringPreferencesKey("active_tv_id")
    }
}
