package de.robnice.elly.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.data.WatchLayoutJson
import de.robnice.elly.data.model.MotionTuning
import de.robnice.elly.data.model.WatchLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.wearSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "wear_settings")

// The few settings kept on the watch itself; everything else comes from the phone.
class WearSettings(context: Context) {

    private val dataStore = context.applicationContext.wearSettingsDataStore

    val demoMode: Flow<Boolean> = dataStore.data.map { it[DEMO_MODE] ?: false }

    val haptics: Flow<Boolean> = dataStore.data.map { it[HAPTICS] ?: true }

    val ringGesture: Flow<Boolean> = dataStore.data.map { it[RING_GESTURE] ?: true }

    val lightEffect: Flow<Boolean> = dataStore.data.map { it[LIGHT_EFFECT] ?: true }

    val pointerSpeed: Flow<Float> = dataStore.data.map { it[POINTER_SPEED] ?: MotionTuning.DEFAULT_SENSITIVITY }

    // Worn on the right wrist the watch is turned round, and up and down of the pointer swap.
    val pointerInverted: Flow<Boolean> = dataStore.data.map { it[POINTER_INVERTED] ?: false }

    // Pages and key functions set on the phone.
    val layout: Flow<WatchLayout> = dataStore.data.map { WatchLayoutJson.decode(it[LAYOUT]) }

    // Changes whenever app icons were stored, so keys showing them are drawn again.
    val iconsVersion: Flow<Long> = dataStore.data.map { it[ICONS_VERSION] ?: 0L }

    // TV chosen on the watch; null while the watch follows the phone's active TV.
    val watchTvId: Flow<String?> = dataStore.data.map { it[WATCH_TV_ID] }

    suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { it[DEMO_MODE] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun setRingGesture(enabled: Boolean) {
        dataStore.edit { it[RING_GESTURE] = enabled }
    }

    suspend fun setLightEffect(enabled: Boolean) {
        dataStore.edit { it[LIGHT_EFFECT] = enabled }
    }

    suspend fun setPointerSpeed(speed: Float) {
        dataStore.edit { it[POINTER_SPEED] = speed }
    }

    suspend fun setPointerInverted(inverted: Boolean) {
        dataStore.edit { it[POINTER_INVERTED] = inverted }
    }

    suspend fun setLayout(raw: String?) {
        dataStore.edit { if (raw == null) it.remove(LAYOUT) else it[LAYOUT] = raw }
    }

    suspend fun iconsChanged() {
        dataStore.edit { it[ICONS_VERSION] = System.currentTimeMillis() }
    }

    suspend fun setWatchTvId(id: String) {
        dataStore.edit { it[WATCH_TV_ID] = id }
    }

    suspend fun hapticsEnabled(): Boolean = haptics.first()

    private companion object {
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val HAPTICS = booleanPreferencesKey("haptics")
        val RING_GESTURE = booleanPreferencesKey("ring_gesture")
        val LIGHT_EFFECT = booleanPreferencesKey("light_effect")
        val WATCH_TV_ID = stringPreferencesKey("watch_tv_id")
        val LAYOUT = stringPreferencesKey("watch_layout")
        val POINTER_SPEED = floatPreferencesKey("pointer_speed")
        val POINTER_INVERTED = booleanPreferencesKey("pointer_inverted")
        val ICONS_VERSION = longPreferencesKey("icons_version")
    }
}
