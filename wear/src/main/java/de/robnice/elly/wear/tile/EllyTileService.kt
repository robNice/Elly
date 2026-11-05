package de.robnice.elly.wear.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.SendResult
import de.robnice.elly.wear.R
import de.robnice.elly.wear.WearApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class EllyTileService : TileService() {

    private val container get() = (application as WearApplication).container

    // A tap on a key reloads the tile, and the request names the key that was tapped. The answer waits a moment
    // for the result, because an update requested right after this one is merged into it and never arrives.
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val slot = WatchSlot.entries.firstOrNull { it.name == requestParams.currentState.lastClickableId }
        return CallbackToFutureAdapter.getFuture { completer ->
            container.applicationScope.launch {
                try {
                    val keys = container.settings.layout.first()
                    val press = slot?.let { container.quickCommands.press(keys.action(it)) }
                    val done = press == null || withTimeoutOrNull(RESULT_WAIT_MILLIS) { press.join() } != null
                    val tvName = when {
                        container.settings.demoMode.first() -> getString(R.string.demo_tv_name)
                        else -> container.tvRepository.activeTv.first()?.name
                    }
                    val last = container.quickCommands.lastResult?.takeIf { it.shownUntil > System.currentTimeMillis() }
                    val layout = TileLayout(this@EllyTileService, requestParams.deviceConfiguration, keys, resourcesVersion(keys), container.appIcons)
                    completer.set(
                        when {
                            !done -> layout.tile(
                                tvName,
                                TileLayout.Feedback(null, R.color.status_checking, 0L, animated = false),
                                System.currentTimeMillis() + SENDING_SHOWN_MILLIS
                            )
                            last != null -> layout.tile(tvName, feedbackFor(last.result), last.shownUntil)
                            else -> layout.tile(tvName)
                        }
                    )
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            TILE_REQUEST
        }
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = CallbackToFutureAdapter.getFuture { completer ->
        container.applicationScope.launch {
            try {
                val keys = container.settings.layout.first()
                val layout = TileLayout(this@EllyTileService, requestParams.deviceConfiguration, keys, resourcesVersion(keys), container.appIcons)
                completer.set(layout.resources())
            } catch (e: Exception) {
                completer.setException(e)
            }
        }
        RESOURCES_REQUEST
    }

    // A new version makes the renderer fetch the images again, after the keys or the app icons changed.
    private suspend fun resourcesVersion(keys: WatchLayout): String =
        "${WatchSlot.TILE.map(keys::action).hashCode()}-${container.settings.iconsVersion.first()}"

    // A plain "sent" is only the ring; errors and the switch-on signal also show their message.
    private fun feedbackFor(result: SendResult): TileLayout.Feedback = when (result) {
        SendResult.UNREACHABLE, SendResult.NO_TV -> failure(R.string.result_unreachable)
        SendResult.PAIRING_REQUIRED -> failure(R.string.result_pairing_required)
        SendResult.FAILED, SendResult.UNSUPPORTED -> failure(R.string.result_failed)
        SendResult.WAKE_SENT -> TileLayout.Feedback(getString(R.string.result_wake_sent), R.color.status_demo, MESSAGE_HOLD_MILLIS)
        SendResult.DEMO -> TileLayout.Feedback(null, R.color.status_demo, SUCCESS_HOLD_MILLIS)
        SendResult.SENT -> TileLayout.Feedback(null, R.color.status_connected, SUCCESS_HOLD_MILLIS)
    }

    private fun failure(text: Int) = TileLayout.Feedback(getString(text), R.color.status_disconnected, MESSAGE_HOLD_MILLIS)

    companion object {
        private const val TILE_REQUEST = "EllyTile"
        private const val RESOURCES_REQUEST = "EllyTileResources"
        private const val RESULT_WAIT_MILLIS = 2_500L
        private const val SENDING_SHOWN_MILLIS = 8_000L
        private const val SUCCESS_HOLD_MILLIS = 1_000L
        private const val MESSAGE_HOLD_MILLIS = 2_500L

        fun requestUpdate(context: Context) {
            getUpdater(context).requestUpdate(EllyTileService::class.java)
        }
    }
}
