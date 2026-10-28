package de.robnice.elly.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.AppContainer
import de.robnice.elly.R
import de.robnice.elly.data.AppSlotsRepository
import de.robnice.elly.data.Backup
import de.robnice.elly.data.BackupContent
import de.robnice.elly.data.BackupJson
import de.robnice.elly.data.BackgroundImageStore
import de.robnice.elly.data.ScreenLayoutRepository
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.model.AppBackground
import de.robnice.elly.data.model.AppSlot
import de.robnice.elly.data.model.AppSlots
import de.robnice.elly.data.model.ButtonGroup
import de.robnice.elly.data.model.CellShape
import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.EpgConfig
import de.robnice.elly.data.model.JoystickPlacement
import de.robnice.elly.data.model.KeyRounding
import de.robnice.elly.data.model.OkButtonPosition
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.data.model.PointerSettings
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenBackground
import de.robnice.elly.data.model.ScreenLayout
import de.robnice.elly.data.model.TouchStyle
import de.robnice.elly.epg.EpgAccess
import de.robnice.elly.epg.FavoriteList
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.remote.TvApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val appSlotsRepository: AppSlotsRepository,
    private val screenLayoutRepository: ScreenLayoutRepository,
    private val backgroundImageStore: BackgroundImageStore,
    private val remoteControl: RemoteControl,
    private val epgAccess: EpgAccess,
    val motionPointerAvailable: Boolean
) : ViewModel() {

    val epgAvailable: StateFlow<Boolean> = epgAccess.available
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val _epgPicker = MutableStateFlow<EpgPickerState?>(null)
    val epgPicker: StateFlow<EpgPickerState?> = _epgPicker.asStateFlow()

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

    // Earlier states of the screen open in the editor, newest last.
    private val history = MutableStateFlow<List<CustomScreen>>(emptyList())
    val canUndo: StateFlow<Boolean> = history
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Tabs shown when the settings or the add dialog open again, kept while the app runs.
    var lastTab = 0
    var lastPickerTab = 0

    fun clearHistory() {
        history.value = emptyList()
    }

    val background: StateFlow<AppBackground> = settingsRepository.background
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppBackground())

    val lightEffectAvailable: Boolean = settingsRepository.lightEffectAvailable

    val lightEffect: StateFlow<Boolean> = settingsRepository.lightEffect
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun setLightEffect(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLightEffect(enabled) }
    }

    fun setBackground(type: ScreenBackground) {
        viewModelScope.launch { settingsRepository.setBackground(type) }
    }

    // The previous own picture is replaced, so its copy is deleted afterwards.
    fun setBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            val path = backgroundImageStore.import(uri) ?: return@launch
            settingsRepository.setBackgroundImage(path)
            backgroundImageStore.deleteUnused(setOf(path))
        }
    }

    private val _backupToRestore = MutableStateFlow<BackupContent?>(null)
    val backupToRestore: StateFlow<BackupContent?> = _backupToRestore.asStateFlow()

    private val _backupMessage = MutableStateFlow<Int?>(null)
    val backupMessage: StateFlow<Int?> = _backupMessage.asStateFlow()

    fun exportBackup(resolver: ContentResolver, uri: Uri, screens: Boolean, appearance: Boolean) = export(resolver, uri) {
        BackupContent(
            screens = if (screens) screenLayoutRepository.activeBackup() else null,
            appearance = if (appearance) appearanceBackup() else null
        ).takeUnless { it.isEmpty }
    }

    // The own picture goes along only while it is the chosen background.
    private suspend fun appearanceBackup(): Backup.Appearance {
        val background = settingsRepository.background.first()
        val image = background.image
            ?.takeIf { background.type == ScreenBackground.CUSTOM }
            ?.let { backgroundImageStore.read(it) }
        return Backup.Appearance(
            palette = settingsRepository.colorPalette.first(),
            background = if (background.type == ScreenBackground.CUSTOM && image == null) ScreenBackground.NONE else background.type,
            image = image,
            keyRounding = settingsRepository.keyRounding.first(),
            dpadRounding = settingsRepository.dpadRounding.first(),
            lightEffect = settingsRepository.lightEffect.first()
        )
    }

    private fun export(resolver: ContentResolver, uri: Uri, create: suspend () -> BackupContent?) {
        viewModelScope.launch {
            val saved = try {
                val text = BackupJson.encode(create() ?: throw IllegalStateException("nothing to back up"))
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: throw IllegalStateException("no stream")
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            _backupMessage.value = if (saved) R.string.settings_backup_saved else R.string.settings_backup_save_failed
        }
    }

    fun readBackup(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { it.readBackupFile() } }.getOrNull()
            }
            val backup = text?.let { BackupJson.decode(it) }
            // Screens belong to a TV, so without one only the appearance can be taken over.
            val usable = backup?.takeIf { it.screens == null || appSlotsRepository.activeOwner.first() != null }
                ?: backup?.let { BackupContent(null, it.appearance) }
            when {
                backup == null -> _backupMessage.value = R.string.settings_backup_invalid
                usable == null || usable.isEmpty -> _backupMessage.value = R.string.settings_backup_no_tv
                else -> _backupToRestore.value = usable
            }
        }
    }

    fun confirmRestore(screens: Boolean, appearance: Boolean) {
        val backup = _backupToRestore.value ?: return
        _backupToRestore.value = null
        viewModelScope.launch {
            backup.screens?.takeIf { screens }?.let {
                screenLayoutRepository.restoreActive(it)
                clearHistory()
            }
            backup.appearance?.takeIf { appearance }?.let {
                val path = it.image?.let { image -> backgroundImageStore.import(image) }
                val type = if (it.background == ScreenBackground.CUSTOM && path == null) ScreenBackground.NONE else it.background
                settingsRepository.setAppearance(it.palette, AppBackground(type, path), it.keyRounding, it.dpadRounding, it.lightEffect)
                backgroundImageStore.deleteUnused(setOfNotNull(path))
            }
            _backupMessage.value = R.string.settings_backup_restored
        }
    }

    fun dismissRestore() {
        _backupToRestore.value = null
    }

    fun dismissBackupMessage() {
        _backupMessage.value = null
    }

    // Backups hold settings and at most one picture; anything much larger is not one.
    private fun InputStream.readBackupFile(): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BACKUP_BYTES) return null
        }
        return out.toString(Charsets.UTF_8.name())
    }

    fun undo(screen: RemoteScreen) {
        val previous = history.value.lastOrNull() ?: return
        history.update { it.dropLast(1) }
        viewModelScope.launch { screenLayoutRepository.updateScreen(screen) { previous } }
    }

    private fun remember(state: CustomScreen) {
        history.update { (it + state).takeLast(MAX_UNDO_STEPS) }
    }

    private fun editScreen(screen: RemoteScreen, transform: (CustomScreen) -> CustomScreen) {
        viewModelScope.launch {
            screenLayoutRepository.updateScreen(screen) { current ->
                transform(current).also { if (it != current) remember(current) }
            }
        }
    }

    // The program guide asks for its favorite list first; here it is only checked that it would fit.
    fun addElement(screen: RemoteScreen, type: CustomElementType, key: RemoteKey?): Boolean {
        if (type == CustomElementType.EPG) {
            if (screens.value[screen]?.add(type, epg = PLACEHOLDER_EPG) == null) return false
            openEpgPicker(screen, null)
            return true
        }
        if (screens.value[screen]?.add(type, key) == null) return false
        editScreen(screen) { it.add(type, key) ?: it }
        return true
    }

    fun addGroup(screen: RemoteScreen, group: ButtonGroup): Boolean {
        if ((screens.value[screen] ?: DefaultScreens.layout(screen)).addGroup(group) == null) return false
        editScreen(screen) { it.addGroup(group) ?: it }
        return true
    }

    fun moveElement(screen: RemoteScreen, id: Long, x: Int, y: Int, cells: CellShape) {
        editScreen(screen) { it.move(id, x, y, cells) }
    }

    fun resizeElement(screen: RemoteScreen, id: Long, width: Int, height: Int, cells: CellShape) {
        editScreen(screen) { it.resize(id, width, height, cells) }
    }

    fun removeElement(screen: RemoteScreen, id: Long) {
        editScreen(screen) { it.remove(id) }
    }

    fun moveElements(screen: RemoteScreen, ids: Set<Long>, dx: Int, dy: Int, cells: CellShape) {
        editScreen(screen) { it.moveAll(ids, dx, dy, cells) ?: it }
    }

    fun removeElements(screen: RemoteScreen, ids: Set<Long>) {
        editScreen(screen) { screenState -> ids.fold(screenState) { current, id -> current.remove(id) } }
    }

    fun resetScreen(screen: RemoteScreen) {
        screens.value[screen]?.let(::remember)
        viewModelScope.launch { screenLayoutRepository.resetScreen(screen) }
    }

    private val _appDraft = MutableStateFlow<AppDraft?>(null)
    val appDraft: StateFlow<AppDraft?> = _appDraft.asStateFlow()

    private val _appPicker = MutableStateFlow<AppPickerState?>(null)
    val appPicker: StateFlow<AppPickerState?> = _appPicker.asStateFlow()

    private val _styleEditor = MutableStateFlow(false)
    val styleEditor: StateFlow<Boolean> = _styleEditor.asStateFlow()

    fun configureElement(screen: RemoteScreen, id: Long) {
        val element = screens.value[screen]?.elements?.firstOrNull { it.id == id } ?: return
        when (element.type) {
            CustomElementType.APP -> _appDraft.value = AppDraft(screen, id, element.app)
            CustomElementType.EPG -> openEpgPicker(screen, id, element.epg?.listId)
            else -> Unit
        }
    }

    private fun openEpgPicker(screen: RemoteScreen, id: Long?, current: String? = null) {
        val request = EpgPickerState(screen, id, current, loading = true)
        _epgPicker.value = request
        viewModelScope.launch {
            val lists = try {
                epgAccess.source()?.favoriteLists()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _epgPicker.update { state ->
                if (state != request) state else state.copy(loading = false, lists = lists.orEmpty(), failed = lists == null)
            }
        }
    }

    fun pickEpgList(list: FavoriteList) {
        val picker = _epgPicker.value ?: return
        _epgPicker.value = null
        val config = EpgConfig(list.id, list.name)
        val id = picker.elementId
        if (id == null) {
            editScreen(picker.screen) { it.add(CustomElementType.EPG, epg = config) ?: it }
        } else {
            editScreen(picker.screen) { it.updateEpg(id, config) }
        }
    }

    fun dismissEpgPicker() {
        _epgPicker.value = null
    }

    fun confirmApp() {
        val draft = _appDraft.value ?: return
        _appDraft.value = null
        editScreen(draft.screen) { it.updateApp(draft.id) { draft.app } }
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

    val keyRounding: StateFlow<Float> = settingsRepository.keyRounding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), KeyRounding.DEFAULT)

    val colorPalette: StateFlow<ColorPalette> = settingsRepository.colorPalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ColorPalette.GRAPHITE)

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch { settingsRepository.setColorPalette(palette) }
    }

    val dpadRounding: StateFlow<Float> = settingsRepository.dpadRounding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), KeyRounding.DEFAULT)

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

    fun setShakeToOpen(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShakeToOpen(enabled) }
    }

    fun setShakeSensitivity(value: Float) {
        viewModelScope.launch { settingsRepository.setShakeSensitivity(value) }
    }

    fun setDoubleTapToOpen(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDoubleTapToOpen(enabled) }
    }

    fun setPointerHeaderButton(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPointerHeaderButton(enabled) }
    }

    fun setSwipeSpeed(value: Float) {
        viewModelScope.launch { settingsRepository.setSwipeSpeed(value) }
    }

    fun setMotionAcceleration(value: Float) {
        viewModelScope.launch { settingsRepository.setMotionAcceleration(value) }
    }

    fun setKeyHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeyHaptics(enabled) }
    }

    fun setKeyRounding(value: Float) {
        viewModelScope.launch { settingsRepository.setKeyRounding(value) }
    }

    fun setDpadRounding(value: Float) {
        viewModelScope.launch { settingsRepository.setDpadRounding(value) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDemoMode(enabled) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val MAX_UNDO_STEPS = 50
        private const val BUFFER_BYTES = 64 * 1024
        private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
        private val PLACEHOLDER_EPG = EpgConfig("", "")

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.settingsRepository,
                    container.appSlotsRepository,
                    container.screenLayoutRepository,
                    container.backgroundImageStore,
                    container.remoteControl,
                    container.epgAccess,
                    container.motionPointerAvailable
                )
            }
        }
    }
}

data class AppDraft(val screen: RemoteScreen, val id: Long, val app: AppSlot?) {
    fun isFor(other: AppDraft): Boolean = screen == other.screen && id == other.id
}

data class EpgPickerState(
    val screen: RemoteScreen,
    val elementId: Long?,
    val current: String?,
    val loading: Boolean,
    val lists: List<FavoriteList> = emptyList(),
    val failed: Boolean = false
)

data class AppPickerState(
    val loading: Boolean,
    val apps: List<TvApp> = emptyList(),
    val error: SendResult? = null
)
