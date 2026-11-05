package de.robnice.elly.wear.remote

import android.content.Context
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.wear.data.WearSettings
import de.robnice.elly.wear.network.WifiNetwork
import de.robnice.elly.wear.tile.EllyTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Sends quick keys without the app being open. Wi-Fi and the TV connection are kept for a while after the
// last press, so several presses in a row do not connect anew each time.
class QuickCommands(
    private val context: Context,
    private val scope: CoroutineScope,
    private val remoteControl: RemoteControl,
    private val wifiNetwork: WifiNetwork,
    private val settings: WearSettings,
    private val haptics: Haptics
) {

    private var holdsNetwork = false
    private var idleRelease: Job? = null

    // The TV does not tell whether something is playing, so play/pause alternates, starting with pause.
    private var paused = false

    // Result of the last press, shown on the tile for a few seconds.
    @Volatile
    var lastResult: QuickResult? = null
        private set

    // The returned job ends once the command is sent, so a broadcast receiver can wait for it.
    @Synchronized
    fun press(key: QuickKey): Job = press(key.action)

    @Synchronized
    fun press(action: WatchAction): Job {
        haptics.press()
        idleRelease?.cancel()
        if (!holdsNetwork) {
            wifiNetwork.acquire()
            holdsNetwork = true
        }
        val command: suspend () -> SendResult = when (action) {
            is WatchAction.Key -> ({ remoteControl.send(action.key) })
            WatchAction.PlayPause -> {
                val key = if (paused) RemoteKey.PLAY else RemoteKey.PAUSE
                paused = !paused
                ({ remoteControl.send(key) })
            }
            is WatchAction.App -> ({ remoteControl.launchApp(action.appId) })
        }
        return scope.launch {
            if (!settings.demoMode.first()) wifiNetwork.awaitAvailable()
            val result = command()
            if (result.failed) haptics.failure()
            lastResult = QuickResult(result, System.currentTimeMillis() + RESULT_SHOWN_MILLIS)
            EllyTileService.requestUpdate(context)
            scheduleRelease()
        }
    }

    @Synchronized
    private fun scheduleRelease() {
        idleRelease?.cancel()
        idleRelease = scope.launch {
            delay(IDLE_MILLIS)
            remoteControl.disconnect()
            synchronized(this@QuickCommands) {
                if (holdsNetwork) {
                    wifiNetwork.release()
                    holdsNetwork = false
                }
            }
        }
    }

    data class QuickResult(val result: SendResult, val shownUntil: Long)

    private val SendResult.failed: Boolean
        get() = this == SendResult.UNREACHABLE || this == SendResult.FAILED ||
            this == SendResult.PAIRING_REQUIRED || this == SendResult.NO_TV

    private companion object {
        const val IDLE_MILLIS = 30_000L
        const val RESULT_SHOWN_MILLIS = 4_000L
    }
}
