package de.robnice.elly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.data.model.RootAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.rootAccessDataStore: DataStore<Preferences> by preferencesDataStore(name = "root_access")

class RootAccessRepository(context: Context, private val cipher: SecretCipher) {

    private val dataStore = context.applicationContext.rootAccessDataStore

    fun access(tvId: String): Flow<RootAccess?> = dataStore.data.map { RootAccessJson.decode(it[key(tvId)], cipher::decrypt) }

    suspend fun get(tvId: String): RootAccess? = access(tvId).first()

    suspend fun save(tvId: String, access: RootAccess) {
        dataStore.edit { it[key(tvId)] = RootAccessJson.encode(access, cipher::encrypt) }
    }

    suspend fun remove(tvId: String) {
        dataStore.edit { it.remove(key(tvId)) }
    }

    private fun key(tvId: String) = stringPreferencesKey("$KEY_PREFIX$tvId")

    private companion object {
        const val KEY_PREFIX = "tv_"
    }
}
