package de.robnice.elgy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elgy.data.model.MotionTuning
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.TouchStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context, private val motionAvailable: Boolean) {

    private val dataStore = context.applicationContext.settingsDataStore

    val demoMode: Flow<Boolean> = dataStore.data.map { it[DEMO_MODE] ?: false }

    val keyHaptics: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS] ?: true }

    val pointerSettings: Flow<PointerSettings> = dataStore.data.map { preferences ->
        val storedMode = preferences[POINTER_MODE]?.let { runCatching { PointerMode.valueOf(it) }.getOrNull() }
        PointerSettings(
            mode = resolveMode(storedMode, motionAvailable),
            okButtonPosition = preferences[OK_BUTTON_POSITION]
                ?.let { runCatching { OkButtonPosition.valueOf(it) }.getOrNull() }
                ?: OkButtonPosition.BOTTOM,
            touchStyle = preferences[TOUCH_STYLE]
                ?.let { runCatching { TouchStyle.valueOf(it) }.getOrNull() }
                ?: TouchStyle.SWIPE,
            motionSensitivity = (preferences[MOTION_SENSITIVITY] ?: MotionTuning.DEFAULT_SENSITIVITY)
                .coerceIn(MotionTuning.SENSITIVITY_RANGE),
            motionAcceleration = (preferences[MOTION_ACCELERATION] ?: MotionTuning.DEFAULT_ACCELERATION)
                .coerceIn(MotionTuning.ACCELERATION_RANGE)
        )
    }

    suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { it[DEMO_MODE] = enabled }
    }

    suspend fun setKeyHaptics(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setPointerMode(mode: PointerMode) {
        dataStore.edit { it[POINTER_MODE] = mode.name }
    }

    suspend fun setOkButtonPosition(position: OkButtonPosition) {
        dataStore.edit { it[OK_BUTTON_POSITION] = position.name }
    }

    suspend fun setTouchStyle(style: TouchStyle) {
        dataStore.edit { it[TOUCH_STYLE] = style.name }
    }

    suspend fun setMotionSensitivity(value: Float) {
        dataStore.edit { it[MOTION_SENSITIVITY] = value.coerceIn(MotionTuning.SENSITIVITY_RANGE) }
    }

    suspend fun setMotionAcceleration(value: Float) {
        dataStore.edit { it[MOTION_ACCELERATION] = value.coerceIn(MotionTuning.ACCELERATION_RANGE) }
    }

    companion object {
        private val DEMO_MODE = booleanPreferencesKey("demo_mode")
        private val KEY_HAPTICS = booleanPreferencesKey("key_haptics")
        private val POINTER_MODE = stringPreferencesKey("pointer_mode")
        private val OK_BUTTON_POSITION = stringPreferencesKey("ok_button_position")
        private val TOUCH_STYLE = stringPreferencesKey("touch_style")
        private val MOTION_SENSITIVITY = floatPreferencesKey("motion_sensitivity")
        private val MOTION_ACCELERATION = floatPreferencesKey("motion_acceleration")

        fun resolveMode(stored: PointerMode?, motionAvailable: Boolean): PointerMode = when {
            !motionAvailable -> PointerMode.TOUCH
            else -> stored ?: PointerMode.MOTION
        }
    }
}
