package de.robnice.elly.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import de.robnice.elly.data.PairedTvJson
import de.robnice.elly.data.TvRepository
import de.robnice.elly.data.WatchLayoutJson
import de.robnice.elly.data.model.WatchLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Publishes the paired TVs, the active TV, the demo mode and the watch layout with the icons of its apps for the
// watch app. The data item only exists while a watch with Elly is known, so the pairing data does not sit in
// Google Play services without need.
class WatchStatePublisher(
    context: Context,
    private val tvRepository: TvRepository,
    private val demoMode: Flow<Boolean>,
    private val watchLayout: Flow<WatchLayout>,
    private val appIcon: suspend (String) -> ByteArray?
) {


    private val dataClient = Wearable.getDataClient(context.applicationContext)
    private val capabilityClient = Wearable.getCapabilityClient(context.applicationContext)

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(watchInstalled(), state()) { installed, state -> state.takeIf { installed } }
                .distinctUntilChanged()
                .collect { state -> if (state != null) publish(state) else remove() }
        }
    }

    // A watch asked for the state, so it has Elly even if the capability has not reached the phone yet.
    suspend fun publishNow() {
        publish(state().first())
    }

    private fun state(): Flow<WatchState> =
        combine(tvRepository.pairedTvs, tvRepository.activeTvId, demoMode, watchLayout) { tvs, activeId, demo, layout ->
            WatchState(PairedTvJson.encode(tvs), activeId, demo, WatchLayoutJson.encode(layout), layout.apps.map { it.appId })
        }

    private fun watchInstalled(): Flow<Boolean> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { trySend(it.nodes.isNotEmpty()) }
        try {
            capabilityClient.addListener(listener, WatchSyncContract.WATCH_CAPABILITY).await()
            val info = capabilityClient.getCapability(WatchSyncContract.WATCH_CAPABILITY, CapabilityClient.FILTER_ALL).await()
            trySend(info.nodes.isNotEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Wearable API not available: ${e.message}")
            trySend(false)
        }
        awaitClose { capabilityClient.removeListener(listener, WatchSyncContract.WATCH_CAPABILITY) }
    }

    private suspend fun publish(state: WatchState) {
        val request = PutDataMapRequest.create(WatchSyncContract.STATE_PATH).apply {
            dataMap.putString(WatchSyncContract.KEY_PAIRED_TVS, state.pairedTvs)
            state.activeTvId?.let { dataMap.putString(WatchSyncContract.KEY_ACTIVE_TV_ID, it) }
            dataMap.putBoolean(WatchSyncContract.KEY_DEMO_MODE, state.demoMode)
            dataMap.putString(WatchSyncContract.KEY_WATCH_LAYOUT, state.watchLayout)
            // Assets with unchanged bytes are not sent again.
            state.appIds.forEach { appId ->
                appIcon(appId)?.let { dataMap.putAsset(WatchSyncContract.KEY_APP_ICON_PREFIX + appId, Asset.createFromBytes(it)) }
            }
        }.asPutDataRequest().setUrgent()
        runCatchingApi("publish") { dataClient.putDataItem(request).await() }
    }

    private suspend fun remove() {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(WatchSyncContract.STATE_PATH).build()
        runCatchingApi("remove") { dataClient.deleteDataItems(uri).await() }
    }

    private suspend fun runCatchingApi(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Watch state $action failed: ${e.message}")
        }
    }

    private data class WatchState(
        val pairedTvs: String,
        val activeTvId: String?,
        val demoMode: Boolean,
        val watchLayout: String,
        val appIds: List<String>
    )

    private companion object {
        const val TAG = "EllyWatchSync"
    }
}
