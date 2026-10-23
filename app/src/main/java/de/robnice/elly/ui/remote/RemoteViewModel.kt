package de.robnice.elly.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.AppContainer
import de.robnice.elly.data.AppIconFiller
import de.robnice.elly.data.model.AppBackground
import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.KeyRounding
import de.robnice.elly.data.model.OkButtonPosition
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.data.model.PointerSettings
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenLayout
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.remote.TvInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteViewModel(
    private val remoteControl: RemoteControl,
    private val appIconFiller: AppIconFiller,
    pointerSettingsFlow: Flow<PointerSettings>,
    keyHapticsFlow: Flow<Boolean>,
    keyRoundingFlow: Flow<Float>,
    dpadRoundingFlow: Flow<Float>,
    colorPaletteFlow: Flow<ColorPalette>,
    backgroundFlow: Flow<AppBackground>,
    lightEffectFlow: Flow<Boolean>,
    screenLayoutFlow: Flow<ScreenLayout>,
    screensFlow: Flow<Map<RemoteScreen, CustomScreen>>
) : ViewModel() {

    val screens: StateFlow<Map<RemoteScreen, CustomScreen>> =
        screensFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DefaultScreens.all())

    val screenLayout: StateFlow<ScreenLayout> =
        screenLayoutFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScreenLayout.DEFAULT)

    val keyHaptics: StateFlow<Boolean> =
        keyHapticsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    val keyRounding: StateFlow<Float> =
        keyRoundingFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), KeyRounding.DEFAULT)

    val dpadRounding: StateFlow<Float> =
        dpadRoundingFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), KeyRounding.DEFAULT)

    val colorPalette: StateFlow<ColorPalette> =
        colorPaletteFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ColorPalette.GRAPHITE)

    val background: StateFlow<AppBackground> =
        backgroundFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppBackground())

    val lightEffect: StateFlow<Boolean> =
        lightEffectFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val _results = MutableSharedFlow<SendResult>(extraBufferCapacity = 1)
    val results: SharedFlow<SendResult> = _results

    var currentScreen: RemoteScreen? = null

    private val _inputs = MutableStateFlow<List<TvInput>?>(null)
    val inputs: StateFlow<List<TvInput>?> = _inputs.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = remoteControl.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectionState.CHECKING)

    private val _pointerActive = MutableStateFlow(false)
    val pointerActive: StateFlow<Boolean> = _pointerActive.asStateFlow()

    val pointerSettings: StateFlow<PointerSettings> = pointerSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        PointerSettings(PointerMode.TOUCH, OkButtonPosition.BOTTOM)
    )

    private val pointerEvents = Channel<PointerEvent>(Channel.UNLIMITED)

    // The text bar opens when the TV shows its keyboard or when the keyboard key is pressed.
    // Closed by hand, it stays closed until the TV opens a text field again.
    private val keyboardByKey = MutableStateFlow(false)
    private val keyboardDismissed = MutableStateFlow(false)
    val textInputOpen: StateFlow<Boolean> =
        combine(remoteControl.keyboardVisible, keyboardByKey, keyboardDismissed) { tv, byKey, dismissed ->
            byKey || (tv && !dismissed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val textMutex = Mutex()
    private var sentText = ""

    // The text the bar starts with: the last sent text, if the TV field still holds that many
    // characters; the TV does not tell its content.
    var initialText = ""
        private set

    init {
        viewModelScope.launch { forwardPointerMoves() }
        viewModelScope.launch {
            remoteControl.keyboardVisible.collect { visible ->
                if (!visible) keyboardDismissed.value = false
                if (visible) prepareText()
            }
        }
        viewModelScope.launch {
            combine(connectionState, screens) { state, _ -> state }
                .filter { it == ConnectionState.CONNECTED }
                .collect { appIconFiller.fillMissing() }
        }
    }

    suspend fun monitorConnection() {
        while (true) {
            remoteControl.checkConnection()
            delay(CONNECTION_CHECK_INTERVAL_MILLIS)
        }
    }

    fun press(key: RemoteKey) {
        viewModelScope.launch { report(remoteControl.send(key)) }
    }

    fun launchApp(appId: String) {
        viewModelScope.launch { report(remoteControl.launchApp(appId)) }
    }

    fun openInputPicker() {
        viewModelScope.launch {
            val loaded = remoteControl.loadInputs()
            if (loaded.result == SendResult.SENT || loaded.result == SendResult.DEMO) {
                _inputs.value = loaded.inputs
            } else {
                report(loaded.result)
            }
        }
    }

    fun selectInput(input: TvInput) {
        _inputs.value = null
        viewModelScope.launch { report(remoteControl.switchInput(input)) }
    }

    fun openKeyboard() {
        prepareText()
        keyboardByKey.value = true
    }

    // An unknown text in the TV field stands in as placeholders, so the first change deletes it
    // and the field matches the bar from then on.
    private fun prepareText() {
        val length = remoteControl.keyboardTextLength.value
        if (length != sentText.length) sentText = UNKNOWN_CHARACTER.toString().repeat(length)
        initialText = if (UNKNOWN_CHARACTER in sentText) "" else sentText
    }

    fun closeKeyboard() {
        keyboardByKey.value = false
        keyboardDismissed.value = true
    }

    fun changeText(text: String) {
        viewModelScope.launch {
            textMutex.withLock {
                val common = sentText.commonPrefixWith(text).length
                val result = remoteControl.editText(sentText.length - common, text.substring(common))
                if (result == SendResult.SENT || result == SendResult.DEMO) sentText = text else report(result)
            }
        }
    }

    fun sendEnter() {
        viewModelScope.launch { textMutex.withLock { report(remoteControl.sendEnter()) } }
    }

    fun dismissInputPicker() {
        _inputs.value = null
    }

    fun openPointer() {
        _pointerActive.value = true
    }

    fun closePointer() {
        if (!_pointerActive.value) return
        _pointerActive.value = false
        pointerEvents.trySend(PointerEvent.Release)
    }

    fun movePointer(dx: Float, dy: Float) {
        if (_pointerActive.value) pointerEvents.trySend(PointerEvent.Move(dx, dy))
    }

    fun scrollPointer(steps: Int) {
        viewModelScope.launch { report(remoteControl.scrollPointer(steps)) }
    }

    fun clickPointer() {
        viewModelScope.launch { report(remoteControl.clickPointer()) }
    }

    private suspend fun forwardPointerMoves() {
        var restX = 0f
        var restY = 0f
        for (event in pointerEvents) {
            var release = event == PointerEvent.Release
            if (event is PointerEvent.Move) {
                restX += event.dx
                restY += event.dy
            }
            while (!release) {
                when (val next = pointerEvents.tryReceive().getOrNull() ?: break) {
                    is PointerEvent.Move -> {
                        restX += next.dx
                        restY += next.dy
                    }
                    PointerEvent.Release -> release = true
                }
            }
            val dx = restX.toInt()
            val dy = restY.toInt()
            restX -= dx
            restY -= dy
            if (dx != 0 || dy != 0) report(remoteControl.movePointer(dx, dy))
            // Any move that reaches the TV after the hide key brings the cursor back in the screen center.
            if (release) {
                restX = 0f
                restY = 0f
                remoteControl.releasePointer()
            }
        }
    }

    private sealed interface PointerEvent {
        data class Move(val dx: Float, val dy: Float) : PointerEvent
        data object Release : PointerEvent
    }

    private suspend fun report(result: SendResult) {
        if (result != SendResult.SENT && result != SendResult.DEMO) _results.emit(result)
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val UNKNOWN_CHARACTER = '\u0000'
        private const val CONNECTION_CHECK_INTERVAL_MILLIS = 10_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                RemoteViewModel(
                    container.remoteControl,
                    container.appIconFiller,
                    container.settingsRepository.pointerSettings,
                    container.settingsRepository.keyHaptics,
                    container.settingsRepository.keyRounding,
                    container.settingsRepository.dpadRounding,
                    container.settingsRepository.colorPalette,
                    container.settingsRepository.background,
                    container.settingsRepository.lightEffect,
                    container.screenLayoutRepository.activeLayout,
                    container.screenLayoutRepository.activeScreens
                )
            }
        }
    }
}
