package de.robnice.elly.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.data.TvRepository
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import de.robnice.elly.data.model.MotionTuning
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.sensor.MotionPointerSensor
import de.robnice.elly.wear.WearContainer
import de.robnice.elly.wear.data.AppIconStore
import de.robnice.elly.wear.data.WearSettings
import de.robnice.elly.wear.network.WifiNetwork
import de.robnice.elly.wear.remote.Haptics
import de.robnice.elly.wear.remote.PointerPacer
import de.robnice.elly.wear.sync.PhoneApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WearRemoteViewModel(
    private val remoteControl: RemoteControl,
    private val tvRepository: TvRepository,
    private val settings: WearSettings,
    private val phoneApp: PhoneApp,
    private val haptics: Haptics,
    private val appIconStore: AppIconStore,
    private val pointerSensor: MotionPointerSensor,
    private val wifiNetwork: WifiNetwork
) : ViewModel() {

    private val pointerMoves = Channel<PointerMove>(Channel.UNLIMITED)

    // The hide key webOS takes to remove the cursor is also a directional key, so it is only sent after use.
    @Volatile
    private var pointerUsed = false

    // Off the UI thread: a busy frame held the moves back and the TV pointer jumped after it.
    init {
        viewModelScope.launch(Dispatchers.Default) { forwardPointerMoves() }
    }

    // The steps go straight to the open pointer socket in a steady tick; without a connection they
    // take the way that connects first.
    private val pointerPacer = PointerPacer { x, y ->
        if (remoteControl.tryMovePointer(x, y)) {
            pointerUsed = true
        } else {
            pointerMoves.trySend(PointerMove.Move(x.toFloat(), y.toFloat()))
        }
    }

    fun holdPointer(holding: Boolean) {
        if (holding) {
            pointerPacer.start()
            val tuning = preferences.value
            val vertical = if (tuning.pointerInverted) -1f else 1f
            pointerSensor.start(tuning.pointerSpeed, MotionTuning.DEFAULT_ACCELERATION) { dx, dy ->
                pointerPacer.add(dx, dy * vertical, pointerSensor.eventTimeNanos)
            }
        } else {
            pointerSensor.stop()
            pointerPacer.finish()
        }
    }

    fun clickPointer() {
        haptics.press()
        viewModelScope.launch { report(tracked { remoteControl.clickPointer() }, announce = true) }
    }

    fun scrollPointer(steps: Int) {
        viewModelScope.launch { report(remoteControl.scrollPointer(steps), announce = false) }
    }

    // While the pointer page is shown, Wi-Fi leaves power save so the moves reach the TV evenly.
    fun pointerShown(shown: Boolean) {
        wifiNetwork.setLowLatency(shown)
        if (!shown) releasePointer()
    }

    fun releasePointer() {
        pointerSensor.stop()
        pointerPacer.stop()
        if (pointerUsed) pointerMoves.trySend(PointerMove.Release)
    }

    // Moves that pile up while the TV is slow are sent as one.
    private suspend fun forwardPointerMoves() {
        var restX = 0f
        var restY = 0f
        for (event in pointerMoves) {
            var release = event == PointerMove.Release
            if (event is PointerMove.Move) {
                restX += event.dx
                restY += event.dy
            }
            while (!release) {
                when (val next = pointerMoves.tryReceive().getOrNull() ?: break) {
                    is PointerMove.Move -> {
                        restX += next.dx
                        restY += next.dy
                    }
                    PointerMove.Release -> release = true
                }
            }
            val dx = restX.toInt()
            val dy = restY.toInt()
            restX -= dx
            restY -= dy
            if (dx != 0 || dy != 0) {
                pointerUsed = true
                report(remoteControl.movePointer(dx, dy), announce = false)
            }
            if (release) {
                restX = 0f
                restY = 0f
                pointerUsed = false
                remoteControl.releasePointer()
            }
        }
    }

    override fun onCleared() {
        pointerSensor.stop()
        pointerPacer.stop()
        wifiNetwork.setLowLatency(false)
    }

    private sealed interface PointerMove {
        data class Move(val dx: Float, val dy: Float) : PointerMove
        data object Release : PointerMove
    }

    private val demoMode: Flow<Boolean> = settings.demoMode

    val tvState: StateFlow<TvState> = combine(tvRepository.activeTv, demoMode) { tv, demo ->
        when {
            demo -> TvState.Demo
            tv == null -> TvState.None
            else -> TvState.Ready(tv.name)
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TvState.Loading)

    val phoneAppStatus: StateFlow<PhoneApp.Status> = phoneApp.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PhoneApp.Status.CHECKING)

    private val prompt = MutableStateFlow<PhonePrompt?>(null)

    // What became of the last request to open something on the phone.
    val phonePrompt: StateFlow<PhonePrompt?> = prompt.asStateFlow()

    fun installOnPhone() = openOnPhone { phoneApp.openStoreOnPhone() }

    fun openAppOnPhone() = openOnPhone { phoneApp.openAppOnPhone() }

    fun openPrivacyOnPhone() = openOnPhone { phoneApp.openPrivacyOnPhone() }

    private fun openOnPhone(open: suspend () -> Boolean) {
        viewModelScope.launch {
            prompt.value = if (open()) PhonePrompt.OPENED else PhonePrompt.UNREACHABLE
        }
    }

    fun phonePromptShown() {
        prompt.value = null
    }

    val preferences: StateFlow<Preferences> = combine(
        settings.haptics,
        settings.ringGesture,
        settings.lightEffect,
        settings.pointerSpeed,
        settings.pointerInverted
    ) { haptics, ring, light, speed, inverted ->
        Preferences(haptics, ring, light, speed, inverted)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Preferences())

    val layout: StateFlow<WatchLayout> = settings.layout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WatchLayout())

    val appIcons: StateFlow<Map<String, ImageBitmap>> = combine(settings.layout, settings.iconsVersion) { layout, _ -> layout.apps }
        .map { apps ->
            withContext(Dispatchers.IO) {
                apps.mapNotNull { app -> appIconStore.bitmap(app.appId)?.let { app.appId to it.asImageBitmap() } }.toMap()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    // The TV does not tell whether something is playing, so play/pause alternates, starting with pause.
    private var paused = false

    fun run(action: WatchAction) {
        when (action) {
            is WatchAction.Key -> send(action.key)
            WatchAction.PlayPause -> {
                send(if (paused) RemoteKey.PLAY else RemoteKey.PAUSE)
                paused = !paused
            }
            is WatchAction.App -> {
                stopVolume()
                haptics.press()
                viewModelScope.launch { report(tracked { remoteControl.launchApp(action.appId) }, announce = true) }
            }
        }
    }

    val pairedTvs: StateFlow<List<PairedTv>> = tvRepository.pairedTvs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val activeTvId: StateFlow<String?> = tvRepository.activeTv
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settings.setHaptics(enabled) }
    }

    fun setRingGesture(enabled: Boolean) {
        viewModelScope.launch { settings.setRingGesture(enabled) }
    }

    fun setLightEffect(enabled: Boolean) {
        viewModelScope.launch { settings.setLightEffect(enabled) }
    }

    // The old connection belongs to the previous TV, so it is dropped before the next command.
    fun selectTv(id: String) {
        viewModelScope.launch {
            settings.setWatchTvId(id)
            tvRepository.setActive(id)
            remoteControl.disconnect()
            remoteControl.checkConnection()
        }
    }

    val connectionState: StateFlow<ConnectionState> = remoteControl.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectionState.CHECKING)

    private val result = MutableStateFlow<SendResult?>(null)

    // Last result worth telling the user; cleared once it was shown.
    val message: StateFlow<SendResult?> = result.asStateFlow()

    private val inFlight = MutableStateFlow(0)

    private val volumeSteps = mutableSetOf<Job>()

    // True while at least one command waits for the TV.
    val busy: StateFlow<Boolean> = inFlight
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val finished = MutableSharedFlow<Boolean>(extraBufferCapacity = EVENT_BUFFER)

    // One event per finished command: true when the TV took it, false when it failed.
    val sendResults: SharedFlow<Boolean> = finished.asSharedFlow()

    // Volume steps come in quick succession and are shown by the ring segment instead, so they only report failures.
    fun send(key: RemoteKey, announce: Boolean = true) {
        if (announce) {
            stopVolume()
            haptics.press()
        }
        val job = viewModelScope.launch { report(tracked { remoteControl.send(key) }, announce) }
        if (!announce) {
            volumeSteps += job
            job.invokeOnCompletion { volumeSteps -= job }
        }
    }

    // Volume steps queue up behind the slower TV; a touch on the screen or another key drops the ones still
    // waiting, so the volume stops where the user let go.
    fun stopVolume() {
        volumeSteps.toList().forEach { it.cancel() }
    }

    fun volumeMotion(stepsPerSecond: Float) = haptics.motion(stepsPerSecond)

    fun volumeMotionEnded() = haptics.motionEnded()

    fun messageShown() {
        result.value = null
    }

    private suspend fun tracked(block: suspend () -> SendResult): SendResult {
        inFlight.update { it + 1 }
        try {
            return block()
        } finally {
            inFlight.update { it - 1 }
        }
    }

    private fun report(sent: SendResult, announce: Boolean) {
        if (sent.failed) haptics.failure()
        if (sent != SendResult.SENT && sent != SendResult.DEMO) result.value = sent
        if (announce || sent.failed) finished.tryEmit(!sent.failed)
    }

    private val SendResult.failed: Boolean
        get() = this == SendResult.UNREACHABLE || this == SendResult.FAILED ||
            this == SendResult.PAIRING_REQUIRED || this == SendResult.NO_TV

    data class Preferences(
        val haptics: Boolean = true,
        val ringGesture: Boolean = true,
        val lightEffect: Boolean = true,
        val pointerSpeed: Float = MotionTuning.DEFAULT_SENSITIVITY,
        val pointerInverted: Boolean = false
    )

    fun setPointerSpeed(speed: Float) {
        viewModelScope.launch { settings.setPointerSpeed(speed) }
    }

    fun setPointerInverted(inverted: Boolean) {
        viewModelScope.launch { settings.setPointerInverted(inverted) }
    }

    enum class PhonePrompt { OPENED, UNREACHABLE }

    sealed interface TvState {
        data object Loading : TvState
        data object None : TvState
        data object Demo : TvState
        data class Ready(val name: String) : TvState
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val EVENT_BUFFER = 16

        fun factory(container: WearContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { WearRemoteViewModel(container.remoteControl, container.tvRepository, container.settings, container.phoneApp, container.haptics, container.appIcons, container.pointerSensor, container.wifiNetwork) }
        }
    }
}
