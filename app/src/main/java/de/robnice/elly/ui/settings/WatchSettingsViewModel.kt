package de.robnice.elly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.AppContainer
import de.robnice.elly.data.AppSlotsRepository
import de.robnice.elly.data.WatchLayoutRepository
import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchPage
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.SendResult
import de.robnice.elly.remote.TvApp
import de.robnice.elly.sync.WatchApp
import de.robnice.elly.sync.WatchPresence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Pages and key functions of the watch app; the phone publishes every change to the watch.
class WatchSettingsViewModel(
    private val layoutRepository: WatchLayoutRepository,
    private val appSlotsRepository: AppSlotsRepository,
    private val remoteControl: RemoteControl,
    private val watchApp: WatchApp
) : ViewModel() {

    val presence: StateFlow<WatchPresence> = watchApp.presence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WatchPresence.NONE)

    private val _installPrompt = MutableStateFlow<Boolean?>(null)

    // Whether opening the Play Store on the watch worked; null until it was tried.
    val installPrompt: StateFlow<Boolean?> = _installPrompt.asStateFlow()

    fun installOnWatch() {
        viewModelScope.launch { _installPrompt.value = watchApp.openStoreOnWatch() }
    }

    val layout: StateFlow<WatchLayout> = layoutRepository.layout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WatchLayout())

    // Icon files of the apps the watch keys open, by app ID.
    val appIcons: StateFlow<Map<String, String>> = layoutRepository.layout
        .map { layout -> layout.apps.mapNotNull { app -> appSlotsRepository.cachedIcon(app.appId)?.let { app.appId to it } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    private val _editing = MutableStateFlow<WatchEditTarget?>(null)

    // The page or the tile whose keys are being set.
    val editing: StateFlow<WatchEditTarget?> = _editing.asStateFlow()

    private val _picking = MutableStateFlow<WatchSlot?>(null)
    val picking: StateFlow<WatchSlot?> = _picking.asStateFlow()

    private val _styling = MutableStateFlow<WatchSlot?>(null)

    // The app key whose display (icon, name or both) is being set.
    val styling: StateFlow<WatchSlot?> = _styling.asStateFlow()

    private val _appPicker = MutableStateFlow<AppPickerState?>(null)
    val appPicker: StateFlow<AppPickerState?> = _appPicker.asStateFlow()

    fun toggle(page: WatchPage) = update { layout ->
        layout.withVisible(page, layout.pages.none { it.page == page && it.visible })
    }

    fun reorder(order: List<WatchPage>) = update { it.reordered(order) }

    fun reset(target: WatchEditTarget) = update { it.reset(target.slots) }

    fun edit(target: WatchEditTarget) {
        _editing.value = target
    }

    fun closeEditor() {
        _editing.value = null
        _picking.value = null
        _styling.value = null
    }

    fun pick(slot: WatchSlot) {
        _picking.value = slot
    }

    fun dismissPicker() {
        _picking.value = null
    }

    // Opened right after an app was chosen, or from the picker of a key that already opens an app.
    fun styleApp() {
        _styling.value = _picking.value ?: return
        _picking.value = null
    }

    fun setDisplay(display: AppDisplay) {
        val slot = _styling.value ?: return
        update { layout ->
            (layout.action(slot) as? WatchAction.App)?.let { layout.with(slot, it.copy(display = display)) } ?: layout
        }
    }

    fun closeStyle() {
        _styling.value = null
    }

    fun assign(action: WatchAction?) {
        val slot = _picking.value ?: return
        _picking.value = null
        update { it.with(slot, action) }
    }

    fun openAppPicker() {
        if (_picking.value == null) return
        _appPicker.value = AppPickerState(loading = true)
        viewModelScope.launch {
            val loaded = remoteControl.loadApps()
            _appPicker.update { current ->
                when {
                    current == null -> null
                    loaded.result == SendResult.SENT || loaded.result == SendResult.DEMO -> current.copy(loading = false, apps = loaded.apps)
                    else -> current.copy(loading = false, error = loaded.result)
                }
            }
        }
    }

    // The icon is stored before the key changes, so the watch gets both in one update. Then the display of the
    // app is offered.
    fun assignApp(app: TvApp) {
        val slot = _picking.value ?: return
        _appPicker.value = null
        _picking.value = null
        _styling.value = slot
        viewModelScope.launch {
            if (appSlotsRepository.cachedIcon(app.id) == null) {
                app.iconUrl?.let { remoteControl.downloadAppIcon(it) }?.let { appSlotsRepository.storeIcon(app.id, it) }
            }
            layoutRepository.update { layout ->
                val display = (layout.action(slot) as? WatchAction.App)?.display ?: AppDisplay.ICON
                layout.with(slot, WatchAction.App(app.id, app.title, display))
            }
        }
    }

    fun dismissAppPicker() {
        _appPicker.value = null
    }

    private fun update(transform: (WatchLayout) -> WatchLayout) {
        viewModelScope.launch { layoutRepository.update(transform) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                WatchSettingsViewModel(
                    container.watchLayoutRepository,
                    container.appSlotsRepository,
                    container.remoteControl,
                    container.watchApp
                )
            }
        }
    }
}

sealed interface WatchEditTarget {
    val slots: List<WatchSlot>

    data class Page(val page: WatchPage) : WatchEditTarget {
        override val slots: List<WatchSlot> get() = WatchSlot.on(page)
    }

    data object Tile : WatchEditTarget {
        override val slots: List<WatchSlot> get() = WatchSlot.TILE
    }
}
