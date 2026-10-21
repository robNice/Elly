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
import de.robnice.elgy.data.model.AppSlots
import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.data.model.DefaultScreens
import de.robnice.elgy.data.model.JoystickPlacement
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenLayout
import de.robnice.elgy.data.model.TouchStyle
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.remote.RemoteKey
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

    val screensAvailable: StateFlow<Boolean> = appSlotsRepository.activeOwner
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

    val screens: StateFlow<Map<RemoteScreen, CustomScreen>> = screenLayoutRepository.activeScreens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DefaultScreens.all())

    fun addElement(screen: RemoteScreen, type: CustomElementType, key: RemoteKey?): Boolean {
        if (screens.value[screen]?.add(type, key) == null) return false
        viewModelScope.launch { screenLayoutRepository.updateScreen(screen) { it.add(type, key) ?: it } }
        return true
    }

    fun moveElement(screen: RemoteScreen, id: Long, x: Int, y: Int) {
        viewModelScope.launch { screenLayoutRepository.updateScreen(screen) { it.move(id, x, y) } }
    }

    fun resizeElement(screen: RemoteScreen, id: Long, width: Int, height: Int) {
        viewModelScope.launch { screenLayoutRepository.updateScreen(screen) { it.resize(id, width, height) } }
    }

    fun removeElement(screen: RemoteScreen, id: Long) {
        viewModelScope.launch { screenLayoutRepository.updateScreen(screen) { it.remove(id) } }
    }

    fun resetScreen(screen: RemoteScreen) {
        viewModelScope.launch { screenLayoutRepository.resetScreen(screen) }
    }

    private val _appDraft = MutableStateFlow<AppDraft?>(null)
    val appDraft: StateFlow<AppDraft?> = _appDraft.asStateFlow()

    private val _appPicker = MutableStateFlow<AppPickerState?>(null)
    val appPicker: StateFlow<AppPickerState?> = _appPicker.asStateFlow()

    private val _styleEditor = MutableStateFlow(false)
    val styleEditor: StateFlow<Boolean> = _styleEditor.asStateFlow()

    fun openAppConfig(screen: RemoteScreen, id: Long) {
        val element = screens.value[screen]?.elements?.firstOrNull { it.id == id } ?: return
        _appDraft.value = AppDraft(screen, id, element.app)
    }

    fun confirmApp() {
        val draft = _appDraft.value ?: return
        _appDraft.value = null
        viewModelScope.launch { screenLayoutRepository.updateScreen(draft.screen) { it.updateApp(draft.id) { draft.app } } }
    }

    fun dismissApp() {
        _appDraft.value = null
    }

    fun openAppPicker() {
        if (_appDraft.value == null) return
        _appPicker.value = AppPickerState(loading = true)
        viewModelScope.launch {
            val loaded = remoteControl.loadApps()
            _appPicker.update { current ->
                when {
                    current == null -> null
                    loaded.result == SendResult.SENT || loaded.result == SendResult.DEMO ->
                        current.copy(loading = false, apps = loaded.apps)
                    else -> current.copy(loading = false, error = loaded.result)
                }
            }
        }
    }

    fun assignApp(app: TvApp) {
        val draft = _appDraft.value ?: return
        _appPicker.value = null
        updateDraft(draft) {
            (it ?: AppSlots.EMPTY).copy(enabled = true, appId = app.id, title = app.title, iconPath = null, customText = null)
        }
        viewModelScope.launch {
            val iconPath = app.iconUrl
                ?.let { remoteControl.downloadAppIcon(it) }
                ?.let { appSlotsRepository.storeIcon(app.id, it) }
                ?: return@launch
            val withIcon: (AppSlot?) -> AppSlot? = { current ->
                current?.takeIf { it.appId == app.id }?.copy(iconPath = iconPath) ?: current
            }
            if (_appDraft.value?.isFor(draft) == true) {
                updateDraft(draft, withIcon)
            } else {
                // Confirmed before the icon arrived: add it to the saved key instead of the closed draft.
                screenLayoutRepository.updateScreen(draft.screen) { it.updateApp(draft.id, withIcon) }
            }
        }
    }

    fun clearApp() {
        val draft = _appDraft.value ?: return
        _appPicker.value = null
        updateDraft(draft) { null }
    }

    fun dismissAppPicker() {
        _appPicker.value = null
    }

    fun openStyleEditor() {
        if (_appDraft.value?.app?.isUsable == true) _styleEditor.value = true
    }

    fun dismissStyleEditor() {
        _styleEditor.value = false
    }

    fun saveStyle(showIcon: Boolean, showText: Boolean, customText: String, textColor: Long?) {
        val draft = _appDraft.value ?: return
        _styleEditor.value = false
        updateDraft(draft) { slot ->
            slot?.copy(
                showIcon = showIcon,
                showText = showText || !showIcon,
                customText = customText.trim().ifBlank { null },
                textColor = textColor
            )
        }
    }

    private fun updateDraft(target: AppDraft, transform: (AppSlot?) -> AppSlot?) {
        _appDraft.update { draft -> if (draft?.isFor(target) == true) draft.copy(app = transform(draft.app)) else draft }
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

data class AppDraft(val screen: RemoteScreen, val id: Long, val app: AppSlot?) {
    fun isFor(other: AppDraft): Boolean = screen == other.screen && id == other.id
}

data class AppPickerState(
    val loading: Boolean,
    val apps: List<TvApp> = emptyList(),
    val error: SendResult? = null
)
