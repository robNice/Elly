package de.robnice.elgy.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elgy.AppContainer
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenLayout
import de.robnice.elgy.remote.ConnectionState
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.remote.SendResult
import de.robnice.elgy.remote.TvInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemoteViewModel(
    private val remoteControl: RemoteControl,
    pointerSettingsFlow: Flow<PointerSettings>,
    keyHapticsFlow: Flow<Boolean>,
    appSlotsFlow: Flow<List<AppSlot>>,
    screenLayoutFlow: Flow<ScreenLayout>
) : ViewModel() {

    val screenLayout: StateFlow<ScreenLayout> =
        screenLayoutFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScreenLayout.DEFAULT)

    val appSlots: StateFlow<List<AppSlot>> =
        appSlotsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val keyHaptics: StateFlow<Boolean> =
        keyHapticsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

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

    init {
        viewModelScope.launch { forwardPointerMoves() }
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
        private const val CONNECTION_CHECK_INTERVAL_MILLIS = 10_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                RemoteViewModel(
                    container.remoteControl,
                    container.settingsRepository.pointerSettings,
                    container.settingsRepository.keyHaptics,
                    container.appSlotsRepository.activeSlots,
                    container.screenLayoutRepository.activeLayout
                )
            }
        }
    }
}
