package de.robnice.elly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.data.model.AppSlots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private val Context.appSlotsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_slots")

class AppSlotsRepository(
    context: Context,
    tvRepository: TvRepository,
    settingsRepository: SettingsRepository,
    private val iconCache: AppIconCache
) {

    private val dataStore = context.applicationContext.appSlotsDataStore

    val activeOwner: Flow<String?> = combine(settingsRepository.demoMode, tvRepository.activeTv) { demo, tv ->
        if (demo) AppSlots.DEMO_OWNER else tv?.id
    }

    suspend fun cachedIcon(appId: String): String? {
        val owner = activeOwner.first() ?: return null
        return iconCache.existing(owner, appId)
    }

    suspend fun storeIcon(appId: String, bytes: ByteArray): String? {
        val owner = activeOwner.first() ?: return null
        return iconCache.store(owner, appId, bytes)
    }

    suspend fun removeOwner(owner: String) {
        iconCache.removeOwner(owner)
    }

    // App keys used to be configured here; they now live on the screens, so drop the old slots.
    suspend fun clearLegacySlots() {
        dataStore.edit { it.remove(LEGACY_SLOTS) }
    }

    private companion object {
        val LEGACY_SLOTS = stringPreferencesKey("slots_by_owner")
    }
}
