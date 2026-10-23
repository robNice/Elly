package de.robnice.elly.remote

import android.util.Log
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.connection.PointerSocket
import de.robnice.elly.tv.connection.TvConnectionException
import de.robnice.elly.tv.connection.WebOsConnection
import de.robnice.elly.tv.connection.WebOsConnector
import de.robnice.elly.tv.pairing.PairingManifest
import de.robnice.elly.tv.power.WakeOnLan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject

class RemoteControl(
    private val tvRepository: TvRepository,
    private val settingsRepository: SettingsRepository,
    private val connector: WebOsConnector,
    private val manifest: PairingManifest,
    private val wakeOnLan: WakeOnLan
) {

    private val mutex = Mutex()
    private var session: TvSession? = null
    private val state = MutableStateFlow(ConnectionState.CHECKING)
    private val tvKeyboard = MutableStateFlow(false)
    private val tvTextLength = MutableStateFlow(0)
    private var tvEnterKeyType = 0

    // True while a text field on the TV has the focus and its on-screen keyboard is shown.
    val keyboardVisible: StateFlow<Boolean> = tvKeyboard.asStateFlow()

    // Length of the text in that field; the TV does not tell its content.
    val keyboardTextLength: StateFlow<Int> = tvTextLength.asStateFlow()

    val connectionState: Flow<ConnectionState> = combine(settingsRepository.demoMode, state) { demo, current ->
        if (demo) ConnectionState.DEMO else current
    }

    // Widgets pass their own TV; only results for the active TV change the app's connection state.
    suspend fun send(key: RemoteKey, target: PairedTv? = null): SendResult {
        val command = key.command
        if (command == RemoteCommand.Unsupported) return SendResult.UNSUPPORTED
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val active = tvRepository.activeTv.first()
        val tv = target ?: active ?: return SendResult.NO_TV
        val report = tv.id == active?.id

        return when (command) {
            RemoteCommand.Power -> togglePower(tv, report)
            is RemoteCommand.Button -> withSession(tv, report) { it.pointer.buttonDelivered(command.name).toResult() }
            is RemoteCommand.Request -> withSession(tv, report) {
                try {
                    it.connection.request(command.uri)
                } catch (e: TvConnectionException.RequestFailed) {
                    it.connection.request(command.fallbackUri ?: throw e)
                }
                SendResult.SENT
            }
            is RemoteCommand.LaunchApp -> withSession(tv, report) {
                it.connection.request(LAUNCH_APP, JSONObject().put("id", command.appId))
                SendResult.SENT
            }
            RemoteCommand.InputPicker, RemoteCommand.TextInput, RemoteCommand.Unsupported -> SendResult.UNSUPPORTED
        }
    }

    suspend fun loadApps(): AppListResult {
        if (settingsRepository.demoMode.first()) return AppListResult(SendResult.DEMO, DEMO_APPS)
        val tv = tvRepository.activeTv.first() ?: return AppListResult(SendResult.NO_TV)
        var apps = emptyList<TvApp>()
        val result = withSession(tv) {
            apps = TvAppParser.parseLaunchPoints(it.connection.request(LIST_LAUNCH_POINTS))
            SendResult.SENT
        }
        return AppListResult(result, apps)
    }

    suspend fun downloadAppIcon(url: String): ByteArray? {
        if (settingsRepository.demoMode.first()) return null
        val tv = tvRepository.activeTv.first() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                connector.httpClient(tv.certificatePin).newCall(Request.Builder().url(url).build()).execute().use { response ->
                    response.body?.bytes()?.takeIf { response.isSuccessful && it.isNotEmpty() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Icon download $url failed: ${e.message}")
                null
            }
        }
    }

    suspend fun launchApp(appId: String): SendResult {
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV
        return withSession(tv) {
            it.connection.request(LAUNCH_APP, JSONObject().put("id", appId))
            SendResult.SENT
        }
    }

    suspend fun loadInputs(): InputListResult {
        if (settingsRepository.demoMode.first()) return InputListResult(SendResult.DEMO, DEMO_INPUTS)
        val tv = tvRepository.activeTv.first() ?: return InputListResult(SendResult.NO_TV)
        var inputs = emptyList<TvInput>()
        val result = withSession(tv) {
            inputs = TvInputParser.parse(it.connection.request(GET_INPUTS))
            SendResult.SENT
        }
        return InputListResult(result, inputs)
    }

    suspend fun switchInput(input: TvInput): SendResult {
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV
        return withSession(tv) {
            it.connection.request(SWITCH_INPUT, JSONObject().put("inputId", input.id))
            SendResult.SENT
        }
    }

    // Edits the focused text field on the TV: delete characters before the cursor, then insert text.
    suspend fun editText(delete: Int, insert: String): SendResult {
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV
        return withSession(tv) {
            if (delete > 0) it.connection.request(DELETE_CHARACTERS, JSONObject().put("count", delete))
            if (insert.isNotEmpty()) it.connection.request(INSERT_TEXT, JSONObject().put("text", insert).put("replace", 0))
            SendResult.SENT
        }
    }

    // A field whose enter means "next" only closes the keyboard and moves the focus on, for example
    // to the search behind a search field. If no other text field takes the focus, OK presses it.
    suspend fun sendEnter(): SendResult {
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV
        val next = tvEnterKeyType == ENTER_KEY_NEXT
        val result = withSession(tv) {
            it.connection.request(SEND_ENTER)
            SendResult.SENT
        }
        if (result != SendResult.SENT || !next) return result
        withTimeoutOrNull(KEYBOARD_CLOSE_WAIT_MILLIS) { tvKeyboard.first { !it } } ?: return result
        if (withTimeoutOrNull(NEXT_FIELD_WAIT_MILLIS) { tvKeyboard.first { it } } != null) return result
        return withSession(tv) { it.pointer.buttonDelivered(OK_BUTTON).toResult() }
    }

    suspend fun movePointer(dx: Int, dy: Int): SendResult = pointerAction { it.pointer.move(dx, dy) }

    suspend fun clickPointer(): SendResult = pointerAction { it.pointer.click() }

    suspend fun scrollPointer(dy: Int): SendResult = pointerAction { it.pointer.scroll(dy) }

    // webOS hides the cursor as soon as a directional key arrives while it is visible.
    suspend fun releasePointer() {
        mutex.withLock { session?.pointer?.button(HIDE_POINTER_BUTTON) }
    }

    suspend fun disconnect() {
        mutex.withLock {
            closeSession()
            state.value = ConnectionState.CHECKING
        }
    }

    // Retries like a command, but takes the lock per attempt and skips while a command holds it,
    // so a check never delays a key press by more than one attempt.
    suspend fun checkConnection() {
        repeat(ATTEMPTS) {
            when (checkOnce() ?: return) {
                ConnectionState.CONNECTED -> {
                    state.value = ConnectionState.CONNECTED
                    return
                }
                else -> Unit
            }
        }
        state.value = ConnectionState.DISCONNECTED
    }

    private suspend fun checkOnce(): ConnectionState? {
        if (settingsRepository.demoMode.first()) return null
        val tv = tvRepository.activeTv.first() ?: return ConnectionState.DISCONNECTED
        if (!mutex.tryLock()) return null
        return try {
            sessionFor(tv).connection.request(CONNECTION_CHECK)
            ConnectionState.CONNECTED
        } catch (e: CancellationException) {
            throw e
        } catch (e: TvConnectionException.RequestFailed) {
            ConnectionState.CONNECTED
        } catch (e: TvConnectionException) {
            Log.w(TAG, "Connection check failed: ${e.message}")
            closeSession()
            ConnectionState.DISCONNECTED
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun pointerAction(action: (TvSession) -> Boolean): SendResult {
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV
        return withSession(tv) { action(it).toResult() }
    }

    private suspend fun togglePower(tv: PairedTv, report: Boolean): SendResult {
        val result = withSession(tv, report) {
            it.connection.request(TURN_OFF)
            closeSession()
            SendResult.SENT
        }
        if (result != SendResult.UNREACHABLE) return result
        val mac = tv.macAddress ?: return SendResult.UNREACHABLE
        return if (wakeOnLan.wake(mac, tv.ipAddress)) SendResult.WAKE_SENT else SendResult.UNREACHABLE
    }

    private suspend fun withSession(
        tv: PairedTv,
        report: Boolean = true,
        action: suspend (TvSession) -> SendResult
    ): SendResult =
        mutex.withLock {
            runWithRetries(tv, action).also { result ->
                if (report) ConnectionState.after(result)?.let { state.value = it }
            }
        }

    private suspend fun runWithRetries(tv: PairedTv, action: suspend (TvSession) -> SendResult): SendResult {
        repeat(ATTEMPTS) { attempt ->
            try {
                return action(sessionFor(tv))
            } catch (e: CancellationException) {
                throw e
            } catch (e: TvConnectionException) {
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                closeSession()
                when (e) {
                    is TvConnectionException.CertificateChanged,
                    is TvConnectionException.PairingRejected -> return SendResult.PAIRING_REQUIRED
                    is TvConnectionException.RequestFailed -> return SendResult.FAILED
                    else -> Unit
                }
            }
        }
        return SendResult.UNREACHABLE
    }

    private suspend fun sessionFor(tv: PairedTv): TvSession {
        session?.let { current ->
            if (current.connection.isOpen && current.belongsTo(tv)) {
                if (!current.pointer.isOpen) current.replacePointer(openPointer(current.connection, tv))
                return current
            }
        }
        closeSession()

        val started = System.nanoTime()
        val connection = connector.connect(tv.ipAddress, tv.certificatePin, allowInsecure = !tv.secure)
        try {
            Log.d(TAG, "Socket open after ${elapsedMillis(started)} ms")
            connection.register(tv.clientKey, manifest.json) {
                throw TvConnectionException.PairingRejected("client key not accepted")
            }
            Log.d(TAG, "Registered after ${elapsedMillis(started)} ms")
            val pointer = openPointer(connection, tv)
            Log.d(TAG, "Pointer socket open after ${elapsedMillis(started)} ms")
            connection.subscribe(REMOTE_KEYBOARD) { payload ->
                payload.optJSONObject("currentWidget")?.let { widget ->
                    tvTextLength.value = widget.optInt("surroundingTextLength", 0)
                    if (widget.optBoolean("focus")) tvEnterKeyType = widget.optInt("enterKeyType", 0)
                    tvKeyboard.value = widget.optBoolean("focus")
                }
            }
            return TvSession(tv, connection, pointer).also { session = it }
        } catch (e: Exception) {
            connection.close()
            throw e
        }
    }

    private suspend fun openPointer(connection: WebOsConnection, tv: PairedTv): PointerSocket {
        val socketPath = connection.request(GET_POINTER_SOCKET).optString("socketPath")
        if (socketPath.isBlank()) throw TvConnectionException.RequestFailed("missing pointer socket")
        return connector.openPointerSocket(socketPath, tv.certificatePin)
    }

    private fun elapsedMillis(since: Long): Long = (System.nanoTime() - since) / 1_000_000

    private fun closeSession() {
        session?.close()
        session = null
        tvKeyboard.value = false
    }

    private fun Boolean.toResult(): SendResult =
        if (this) SendResult.SENT else throw TvConnectionException.ConnectionLost()

    private companion object {
        const val TAG = "EllyRemote"
        const val HIDE_POINTER_BUTTON = "UP"
        const val ATTEMPTS = 2
        const val LAUNCH_APP = "ssap://system.launcher/launch"
        const val TURN_OFF = "ssap://system/turnOff"
        const val GET_INPUTS = "ssap://tv/getExternalInputList"
        const val LIST_LAUNCH_POINTS = "ssap://com.webos.applicationManager/listLaunchPoints"
        val DEMO_APPS = listOf(
            TvApp("amazon", "Prime Video"),
            TvApp("com.disney.disneyplus-prod", "Disney+"),
            TvApp("netflix", "Netflix"),
            TvApp("ui30", "Rakuten TV"),
            TvApp("spotify-beehive", "Spotify"),
            TvApp("youtube.leanback.v4", "YouTube")
        )
        const val SWITCH_INPUT = "ssap://tv/switchInput"
        val DEMO_INPUTS = (1..3).map { TvInput(id = "HDMI_$it", label = "HDMI $it", connected = it == 1) }
        const val GET_POINTER_SOCKET = "ssap://com.webos.service.networkinput/getPointerInputSocket"
        const val CONNECTION_CHECK = "ssap://com.webos.applicationManager/getForegroundAppInfo"
        const val REMOTE_KEYBOARD = "ssap://com.webos.service.ime/registerRemoteKeyboard"
        const val INSERT_TEXT = "ssap://com.webos.service.ime/insertText"
        const val DELETE_CHARACTERS = "ssap://com.webos.service.ime/deleteCharacters"
        const val SEND_ENTER = "ssap://com.webos.service.ime/sendEnterKey"
        const val OK_BUTTON = "ENTER"
        const val ENTER_KEY_NEXT = 5
        const val KEYBOARD_CLOSE_WAIT_MILLIS = 1_000L
        const val NEXT_FIELD_WAIT_MILLIS = 400L
    }
}
