package de.robnice.elgy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.AppSlots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSlotsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_slots")

class AppSlotsRepository(
    context: Context,
    tvRepository: TvRepository,
    settingsRepository: SettingsRepository,
    private val iconCache: AppIconCache
) {

    private val dataStore = context.applicationContext.appSlotsDataStore

    private val allSlots: Flow<Map<String, List<AppSlot>>> =
        dataStore.data.map { AppSlotsJson.decodeAll(it[SLOTS]) }

    val activeOwner: Flow<String?> = combine(settingsRepository.demoMode, tvRepository.activeTv) { demo, tv ->
        if (demo) AppSlots.DEMO_OWNER else tv?.id
    }

    val activeSlots: Flow<List<AppSlot>> = combine(allSlots, activeOwner) { slots, owner ->
        owner?.let { slots[it] } ?: AppSlots.DEFAULT
    }

    suspend fun storeIcon(appId: String, bytes: ByteArray): String? {
        val owner = activeOwner.first() ?: return null
        return iconCache.store(owner, appId, bytes)
    }

    suspend fun updateSlot(index: Int, slot: AppSlot) {
        val owner = activeOwner.first() ?: return
        dataStore.edit { preferences ->
            val all = AppSlotsJson.decodeAll(preferences[SLOTS]).toMutableMap()
            val current = (all[owner] ?: AppSlots.DEFAULT).toMutableList()
            if (index !in current.indices) return@edit
            current[index] = slot
            all[owner] = current
            preferences[SLOTS] = AppSlotsJson.encodeAll(all)
        }
    }

    suspend fun removeOwner(owner: String) {
        dataStore.edit { preferences ->
            val all = AppSlotsJson.decodeAll(preferences[SLOTS])
            if (owner in all) preferences[SLOTS] = AppSlotsJson.encodeAll(all - owner)
        }
        iconCache.removeOwner(owner)
    }

    private companion object {
        val SLOTS = stringPreferencesKey("slots_by_owner")
    }
}
