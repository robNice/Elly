package de.robnice.elgy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elgy.AppContainer
import de.robnice.elgy.data.AppSlotsRepository
import de.robnice.elgy.data.ScreenLayoutRepository
import de.robnice.elgy.data.SettingsRepository
import de.robnice.elgy.data.TvRepository
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.JoystickPlacement
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenLayout
import de.robnice.elgy.data.model.TouchStyle
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.remote.SendResult
import de.robnice.elgy.remote.TvApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val tvRepository: TvRepository,
    private val appSlotsRepository: AppSlotsRepository,
    private val screenLayoutRepository: ScreenLayoutRepository,
    private val remoteControl: RemoteControl,
    val motionPointerAvailable: Boolean
) : ViewModel() {

    val appSlots: StateFlow<List<AppSlot>> = appSlotsRepository.activeSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val appSlotsAvailable: StateFlow<Boolean> = appSlotsRepository.activeOwner
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val screenLayout: StateFlow<ScreenLayout> = screenLayoutRepository.activeLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScreenLayout.DEFAULT)

    fun toggleScreen(screen: RemoteScreen) {
        viewModelScope.launch { screenLayoutRepository.update { it.toggle(screen) } }
    }

    fun reorderScreens(order: List<RemoteScreen>) {
        viewModelScope.launch { screenLayoutRepository.update { it.reorder(order) } }
    }

    private val _appPicker = MutableStateFlow<AppPickerState?>(null)
    val appPicker: StateFlow<AppPickerState?> = _appPicker.asStateFlow()

    fun setAppSlotEnabled(index: Int, enabled: Boolean) {
        val slot = appSlots.value.getOrNull(index) ?: return
        viewModelScope.launch { appSlotsRepository.updateSlot(index, slot.copy(enabled = enabled)) }
    }

    fun openAppPicker(index: Int) {
        _appPicker.value = AppPickerState(slotIndex = index, loading = true)
        viewModelScope.launch {
            val loaded = remoteControl.loadApps()
            _appPicker.update { current ->
                if (current?.slotIndex != index) current
                else if (loaded.result == SendResult.SENT || loaded.result == SendResult.DEMO) {
                    current.copy(loading = false, apps = loaded.apps)
                } else {
                    current.copy(loading = false, error = loaded.result)
                }
            }
        }
    }

    fun assignApp(app: TvApp) {
        val index = _appPicker.value?.slotIndex ?: return
        val previous = appSlots.value.getOrNull(index) ?: return
        _appPicker.value = null
        viewModelScope.launch {
            val iconPath = app.iconUrl
                ?.let { remoteControl.downloadAppIcon(it) }
                ?.let { appSlotsRepository.storeIcon(app.id, it) }
            appSlotsRepository.updateSlot(
                index,
                previous.copy(enabled = true, appId = app.id, title = app.title, iconPath = iconPath, customText = null)
            )
        }
    }

    private val _styleEditor = MutableStateFlow<Int?>(null)
    val styleEditor: StateFlow<Int?> = _styleEditor.asStateFlow()

    fun openStyleEditor(index: Int) {
        _styleEditor.value = index
    }

    fun dismissStyleEditor() {
        _styleEditor.value = null
    }

    fun saveStyle(index: Int, showIcon: Boolean, showText: Boolean, customText: String, textColor: Long?) {
        val slot = appSlots.value.getOrNull(index) ?: return
        _styleEditor.value = null
        viewModelScope.launch {
            appSlotsRepository.updateSlot(
                index,
                slot.copy(
                    showIcon = showIcon,
                    showText = showText || !showIcon,
                    customText = customText.trim().ifBlank { null },
                    textColor = textColor
                )
            )
        }
    }

    fun clearAppSlot() {
        val index = _appPicker.value?.slotIndex ?: return
        _appPicker.value = null
        viewModelScope.launch {
            appSlotsRepository.updateSlot(index, AppSlot(enabled = false, appId = null, title = null))
        }
    }

    fun dismissAppPicker() {
        _appPicker.value = null
    }

    val demoMode: StateFlow<Boolean> = settingsRepository.demoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val keyHaptics: StateFlow<Boolean> = settingsRepository.keyHaptics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    val activeTv: StateFlow<PairedTv?> = tvRepository.activeTv
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val pointerSettings: StateFlow<PointerSettings> = settingsRepository.pointerSettings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            PointerSettings(
                SettingsRepository.resolveMode(null, motionPointerAvailable),
                OkButtonPosition.BOTTOM
            )
        )

    fun setPointerMode(mode: PointerMode) {
        viewModelScope.launch { settingsRepository.setPointerMode(mode) }
    }

    fun setOkButtonPosition(position: OkButtonPosition) {
        viewModelScope.launch { settingsRepository.setOkButtonPosition(position) }
    }

    fun setTouchStyle(style: TouchStyle) {
        viewModelScope.launch { settingsRepository.setTouchStyle(style) }
    }

    fun setJoystickPlacement(placement: JoystickPlacement) {
        viewModelScope.launch { settingsRepository.setJoystickPlacement(placement) }
    }

    fun setMotionSensitivity(value: Float) {
        viewModelScope.launch { settingsRepository.setMotionSensitivity(value) }
    }

    fun setMotionAcceleration(value: Float) {
        viewModelScope.launch { settingsRepository.setMotionAcceleration(value) }
    }

    fun setKeyHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeyHaptics(enabled) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDemoMode(enabled) }
    }

    fun disconnectActiveTv() {
        viewModelScope.launch {
            tvRepository.activeTv.first()?.let {
                tvRepository.remove(it.id)
                appSlotsRepository.removeOwner(it.id)
                screenLayoutRepository.removeOwner(it.id)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.settingsRepository,
                    container.tvRepository,
                    container.appSlotsRepository,
                    container.screenLayoutRepository,
                    container.remoteControl,
                    container.motionPointerAvailable
                )
            }
        }
    }
}

data class AppPickerState(
    val slotIndex: Int,
    val loading: Boolean,
    val apps: List<TvApp> = emptyList(),
    val error: SendResult? = null
)
