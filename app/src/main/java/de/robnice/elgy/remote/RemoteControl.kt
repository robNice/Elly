package de.robnice.elgy.remote

import android.util.Log
import de.robnice.elgy.data.SettingsRepository
import de.robnice.elgy.data.TvRepository
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.tv.connection.PointerSocket
import de.robnice.elgy.tv.connection.TvConnectionException
import de.robnice.elgy.tv.connection.WebOsConnection
import de.robnice.elgy.tv.connection.WebOsConnector
import de.robnice.elgy.tv.pairing.PairingManifest
import de.robnice.elgy.tv.power.WakeOnLan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    val connectionState: Flow<ConnectionState> = combine(settingsRepository.demoMode, state) { demo, current ->
        if (demo) ConnectionState.DEMO else current
    }

    suspend fun send(key: RemoteKey): SendResult {
        val command = key.command
        if (command == RemoteCommand.Unsupported) return SendResult.UNSUPPORTED
        if (settingsRepository.demoMode.first()) return SendResult.DEMO
        val tv = tvRepository.activeTv.first() ?: return SendResult.NO_TV

        return when (command) {
            RemoteCommand.Power -> togglePower(tv)
            is RemoteCommand.Button -> withSession(tv) { it.pointer.button(command.name).toResult() }
            is RemoteCommand.Request -> withSession(tv) {
                it.connection.request(command.uri)
                SendResult.SENT
            }
            is RemoteCommand.LaunchApp -> withSession(tv) {
                it.connection.request(LAUNCH_APP, JSONObject().put("id", command.appId))
                SendResult.SENT
            }
            RemoteCommand.InputPicker, RemoteCommand.Unsupported -> SendResult.UNSUPPORTED
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

    private suspend fun togglePower(tv: PairedTv): SendResult {
        val result = withSession(tv) {
            it.connection.request(TURN_OFF)
            closeSession()
            SendResult.SENT
        }
        if (result != SendResult.UNREACHABLE) return result
        val mac = tv.macAddress ?: return SendResult.UNREACHABLE
        return if (wakeOnLan.wake(mac, tv.ipAddress)) SendResult.WAKE_SENT else SendResult.UNREACHABLE
    }

    private suspend fun withSession(tv: PairedTv, action: suspend (TvSession) -> SendResult): SendResult =
        mutex.withLock {
            runWithRetries(tv, action).also { result -> ConnectionState.after(result)?.let { state.value = it } }
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

        val connection = connector.connect(tv.ipAddress, tv.certificatePin, allowInsecure = !tv.secure)
        try {
            connection.register(tv.clientKey, manifest.json) {
                throw TvConnectionException.PairingRejected("client key not accepted")
            }
            val pointer = openPointer(connection, tv)
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

    private fun closeSession() {
        session?.close()
        session = null
    }

    private fun Boolean.toResult(): SendResult =
        if (this) SendResult.SENT else throw TvConnectionException.ConnectionLost()

    private companion object {
        const val TAG = "ElgyRemote"
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
    }
}
