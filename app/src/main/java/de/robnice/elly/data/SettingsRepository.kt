package de.robnice.elly.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.robnice.elly.data.model.AppBackground
import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.KeyRounding
import de.robnice.elly.data.model.MotionTuning
import de.robnice.elly.data.model.JoystickPlacement
import de.robnice.elly.data.model.OkButtonPosition
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.data.model.PointerSettings
import de.robnice.elly.data.model.ScreenBackground
import de.robnice.elly.data.model.TouchStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context, private val motionAvailable: Boolean) {

    private val dataStore = context.applicationContext.settingsDataStore

    val demoMode: Flow<Boolean> = dataStore.data.map { it[DEMO_MODE] ?: false }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val keyHaptics: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS] ?: true }

    val keyRounding: Flow<Float> = dataStore.data.map { (it[KEY_ROUNDING] ?: KeyRounding.DEFAULT).coerceIn(KeyRounding.RANGE) }

    // The soft shadow edges need the blur of Android 12, so the effect is off on older versions.
    val lightEffectAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightEffect: Flow<Boolean> = dataStore.data.map { lightEffectAvailable && (it[LIGHT_EFFECT] ?: false) }

    val background: Flow<AppBackground> = dataStore.data.map { preferences ->
        AppBackground(
            type = preferences[BACKGROUND]?.let { runCatching { ScreenBackground.valueOf(it) }.getOrNull() } ?: ScreenBackground.NONE,
            image = preferences[BACKGROUND_IMAGE]
        )
    }

    val colorPalette: Flow<ColorPalette> = dataStore.data.map { preferences ->
        preferences[COLOR_PALETTE]?.let { runCatching { ColorPalette.valueOf(it) }.getOrNull() } ?: ColorPalette.GRAPHITE
    }

    val dpadRounding: Flow<Float> = dataStore.data.map { (it[DPAD_ROUNDING] ?: KeyRounding.DEFAULT).coerceIn(KeyRounding.RANGE) }

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
            joystickPlacement = preferences[JOYSTICK_PLACEMENT]
                ?.let { runCatching { JoystickPlacement.valueOf(it) }.getOrNull() }
                ?: JoystickPlacement.FREE,
            motionSensitivity = (preferences[MOTION_SENSITIVITY] ?: MotionTuning.DEFAULT_SENSITIVITY)
                .coerceIn(MotionTuning.SENSITIVITY_RANGE),
            motionAcceleration = (preferences[MOTION_ACCELERATION] ?: MotionTuning.DEFAULT_ACCELERATION)
                .coerceIn(MotionTuning.ACCELERATION_RANGE),
            swipeSpeed = (preferences[SWIPE_SPEED] ?: MotionTuning.DEFAULT_SWIPE_SPEED)
                .coerceIn(MotionTuning.SWIPE_SPEED_RANGE),
            shakeToOpen = preferences[SHAKE_TO_OPEN] ?: false,
            shakeSensitivity = (preferences[SHAKE_SENSITIVITY] ?: MotionTuning.DEFAULT_SHAKE_SENSITIVITY)
                .coerceIn(MotionTuning.SHAKE_SENSITIVITY_RANGE),
            doubleTapToOpen = preferences[DOUBLE_TAP_TO_OPEN] ?: false
        )
    }

    suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { it[DEMO_MODE] = enabled }
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setKeyHaptics(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setKeyRounding(value: Float) {
        dataStore.edit { it[KEY_ROUNDING] = value.coerceIn(KeyRounding.RANGE) }
    }

    suspend fun setLightEffect(enabled: Boolean) {
        dataStore.edit { it[LIGHT_EFFECT] = enabled }
    }

    suspend fun setBackground(type: ScreenBackground) {
        dataStore.edit { it[BACKGROUND] = type.name }
    }

    suspend fun setBackgroundImage(path: String) {
        dataStore.edit {
            it[BACKGROUND] = ScreenBackground.CUSTOM.name
            it[BACKGROUND_IMAGE] = path
        }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        dataStore.edit { it[COLOR_PALETTE] = palette.name }
    }

    suspend fun setDpadRounding(value: Float) {
        dataStore.edit { it[DPAD_ROUNDING] = value.coerceIn(KeyRounding.RANGE) }
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

    suspend fun setJoystickPlacement(placement: JoystickPlacement) {
        dataStore.edit { it[JOYSTICK_PLACEMENT] = placement.name }
    }

    suspend fun setMotionSensitivity(value: Float) {
        dataStore.edit { it[MOTION_SENSITIVITY] = value.coerceIn(MotionTuning.SENSITIVITY_RANGE) }
    }

    suspend fun setShakeToOpen(enabled: Boolean) {
        dataStore.edit { it[SHAKE_TO_OPEN] = enabled }
    }

    suspend fun setShakeSensitivity(value: Float) {
        dataStore.edit { it[SHAKE_SENSITIVITY] = value.coerceIn(MotionTuning.SHAKE_SENSITIVITY_RANGE) }
    }

    suspend fun setDoubleTapToOpen(enabled: Boolean) {
        dataStore.edit { it[DOUBLE_TAP_TO_OPEN] = enabled }
    }

    suspend fun setSwipeSpeed(value: Float) {
        dataStore.edit { it[SWIPE_SPEED] = value.coerceIn(MotionTuning.SWIPE_SPEED_RANGE) }
    }

    suspend fun setMotionAcceleration(value: Float) {
        dataStore.edit { it[MOTION_ACCELERATION] = value.coerceIn(MotionTuning.ACCELERATION_RANGE) }
    }

    companion object {
        private val DEMO_MODE = booleanPreferencesKey("demo_mode")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_HAPTICS = booleanPreferencesKey("key_haptics")
        private val KEY_ROUNDING = floatPreferencesKey("key_rounding")
        private val DPAD_ROUNDING = floatPreferencesKey("dpad_rounding")
        private val COLOR_PALETTE = stringPreferencesKey("color_palette")
        private val BACKGROUND = stringPreferencesKey("background")
        private val LIGHT_EFFECT = booleanPreferencesKey("light_effect")
        private val BACKGROUND_IMAGE = stringPreferencesKey("background_image")
        private val POINTER_MODE = stringPreferencesKey("pointer_mode")
        private val OK_BUTTON_POSITION = stringPreferencesKey("ok_button_position")
        private val TOUCH_STYLE = stringPreferencesKey("touch_style")
        private val JOYSTICK_PLACEMENT = stringPreferencesKey("joystick_placement")
        private val MOTION_SENSITIVITY = floatPreferencesKey("motion_sensitivity")
        private val MOTION_ACCELERATION = floatPreferencesKey("motion_acceleration")
        private val SWIPE_SPEED = floatPreferencesKey("swipe_speed")
        private val SHAKE_TO_OPEN = booleanPreferencesKey("shake_to_open")
        private val SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")
        private val DOUBLE_TAP_TO_OPEN = booleanPreferencesKey("double_tap_to_open")

        fun resolveMode(stored: PointerMode?, motionAvailable: Boolean): PointerMode = when {
            !motionAvailable -> PointerMode.TOUCH
            else -> stored ?: PointerMode.MOTION
        }
    }
}
