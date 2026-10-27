package de.robnice.elly.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.annotation.StringRes
import de.robnice.elly.AppContainer
import de.robnice.elly.R
import de.robnice.elly.data.AppIconFiller
import de.robnice.elly.data.model.AppBackground
import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.KeyRounding
import de.robnice.elly.data.model.OkButtonPosition
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.data.model.PointerSettings
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenLayout
import de.robnice.elly.epg.AudioTrack
import de.robnice.elly.epg.AudioTracks
import de.robnice.elly.epg.EpgAction
import de.robnice.elly.epg.EpgChannel
import de.robnice.elly.epg.EpgGuide
import de.robnice.elly.epg.EpgReservation
import de.robnice.elly.epg.EpgVideoInfo
import de.robnice.elly.epg.EpgRepository
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.remote.TvInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteViewModel(
    private val remoteControl: RemoteControl,
    private val appIconFiller: AppIconFiller,
    private val epgRepository: EpgRepository,
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

    private val _commandSent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val commandSent: SharedFlow<Unit> = _commandSent

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

    // Refreshes the favorite lists shown by guide elements while the remote is visible.
    suspend fun keepGuideFresh() {
        screens
            .map { all ->
                all.values.flatMap { it.elements }
                    .filter { it.type == CustomElementType.EPG }
                    .mapNotNull { it.epg?.listId }
                    .toSet()
            }
            .distinctUntilChanged()
            .collectLatest { lists ->
                if (lists.isEmpty()) return@collectLatest
                coroutineScope {
                    launch {
                        while (true) {
                            _currentChannel.value = remoteControl.currentChannelId()
                            delay(CURRENT_CHANNEL_INTERVAL_MILLIS)
                        }
                    }
                    while (true) {
                        epgRepository.refresh(lists)
                        delay(GUIDE_REFRESH_INTERVAL_MILLIS)
                    }
                }
            }
    }

    private val _currentChannel = MutableStateFlow<String?>(null)
    val currentChannel: StateFlow<String?> = _currentChannel.asStateFlow()

    fun guide(listId: String): Flow<EpgGuide?> = epgRepository.guide(listId)

    val guideRefreshing: StateFlow<Boolean> = epgRepository.refreshing

    val guideFailed: StateFlow<Boolean> = epgRepository.failed

    val guideReservations: StateFlow<List<EpgReservation>> = epgRepository.reservations

    val guideCanRecord: StateFlow<Boolean> = epgRepository.canRecord

    suspend fun guideVideoInfo(channel: EpgChannel): EpgVideoInfo? = epgRepository.videoInfo(channel)

    suspend fun guideProgramImage(url: String): ByteArray? = epgRepository.programImage(url)

    // Runs in the view model, so closing the info window does not stop a change the TV has already begun.
    suspend fun performGuideAction(action: EpgAction) = viewModelScope.async {
        _commandSent.tryEmit(Unit)
        epgRepository.perform(action)
    }.await()

    fun press(key: RemoteKey) {
        _commandSent.tryEmit(Unit)
        viewModelScope.launch { report(remoteControl.send(key)) }
    }

    fun switchChannel(channelId: String) {
        _commandSent.tryEmit(Unit)
        viewModelScope.launch {
            val result = remoteControl.openChannel(channelId)
            if (result == SendResult.SENT || result == SendResult.DEMO) _currentChannel.value = channelId
            report(result)
        }
    }

    fun launchApp(appId: String) {
        _commandSent.tryEmit(Unit)
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
        _commandSent.tryEmit(Unit)
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
        _commandSent.tryEmit(Unit)
        viewModelScope.launch {
            textMutex.withLock {
                val common = sentText.commonPrefixWith(text).length
                val result = remoteControl.editText(sentText.length - common, text.substring(common))
                if (result == SendResult.SENT || result == SendResult.DEMO) sentText = text else report(result)
            }
        }
    }

    fun sendEnter() {
        _commandSent.tryEmit(Unit)
        viewModelScope.launch { textMutex.withLock { report(remoteControl.sendEnter()) } }
    }

    private val _audioTracks = MutableStateFlow<AudioTrackPicker?>(null)
    val audioTracks: StateFlow<AudioTrackPicker?> = _audioTracks.asStateFlow()

    fun openAudioTracks() {
        _audioTracks.value = AudioTrackPicker.Loading
        viewModelScope.launch { showAudioTracks(loadAudioTracks()) }
    }

    // The list stays open, so the checkmark shows what the TV really switched to.
    fun selectAudioTrack(track: AudioTrack) {
        val shown = _audioTracks.value as? AudioTrackPicker.Tracks ?: return
        _audioTracks.value = AudioTrackPicker.Tracks(shown.tracks.copy(currentId = track.id))
        _commandSent.tryEmit(Unit)
        viewModelScope.launch {
            val result = try {
                val switched = epgRepository.selectAudioTrack(track.id, shown.tracks.channelId)
                val reloaded = loadAudioTracks()
                if (!switched && reloaded is AudioTrackPicker.Tracks) reloaded.copy(notice = R.string.audio_tracks_channel_changed) else reloaded
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AudioTrackPicker.Message(R.string.audio_tracks_failed)
            }
            showAudioTracks(result)
        }
    }

    // The TV then picks the described track by itself wherever there is one, so the list is read again.
    fun setAudioDescription(enabled: Boolean) {
        val shown = _audioTracks.value as? AudioTrackPicker.Tracks ?: return
        _audioTracks.value = AudioTrackPicker.Tracks(shown.tracks.copy(descriptionEnabled = enabled))
        _commandSent.tryEmit(Unit)
        viewModelScope.launch {
            val result = try {
                epgRepository.setAudioDescription(enabled)
                delay(AUDIO_DESCRIPTION_SETTLE_MILLIS)
                loadAudioTracks()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AudioTrackPicker.Message(R.string.audio_tracks_failed)
            }
            showAudioTracks(result)
        }
    }

    fun dismissAudioTracks() {
        _audioTracks.value = null
    }

    private fun showAudioTracks(picker: AudioTrackPicker) {
        _audioTracks.update { if (it == null) null else picker }
    }

    private suspend fun loadAudioTracks(): AudioTrackPicker = try {
        val tracks = epgRepository.audioTracks()
        when {
            tracks == null -> AudioTrackPicker.Message(R.string.audio_tracks_no_root)
            tracks.tracks.isEmpty() -> AudioTrackPicker.Message(R.string.audio_tracks_none)
            else -> AudioTrackPicker.Tracks(tracks)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        AudioTrackPicker.Message(R.string.audio_tracks_failed)
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
        private const val GUIDE_REFRESH_INTERVAL_MILLIS = 15 * 60_000L
        private const val CURRENT_CHANNEL_INTERVAL_MILLIS = 10_000L
        private const val AUDIO_DESCRIPTION_SETTLE_MILLIS = 1_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                RemoteViewModel(
                    container.remoteControl,
                    container.appIconFiller,
                    container.epgRepository,
                    container.settingsRepository.pointerSettings,
                    container.settingsRepository.keyHaptics,
                    container.settingsRepository.keyRounding,
                    container.settingsRepository.dpadRounding,
                    container.settingsRepository.colorPalette,
                    container.settingsRepository.background,
                    container.settingsRepository.lightEffect,
                    container.screenLayoutRepository.activeLayout,
                    // Without SSH access the program guide has no data, so its elements are left out.
                    combine(container.screenLayoutRepository.activeScreens, container.epgAccess.available) { screens, epg ->
                        if (epg) screens else screens.mapValues { it.value.withoutEpg() }
                    }
                )
            }
        }
    }
}

sealed interface AudioTrackPicker {
    data object Loading : AudioTrackPicker
    data class Tracks(val tracks: AudioTracks, @param:StringRes val notice: Int? = null) : AudioTrackPicker
    data class Message(@param:StringRes val text: Int) : AudioTrackPicker
}
