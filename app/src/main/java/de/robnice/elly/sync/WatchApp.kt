package de.robnice.elly.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class WatchPresence { NONE, WITHOUT_APP, WITH_APP }

// Whether a watch is connected and has Elly. Without Elly the phone offers to open its Play Store page on the
// watch, the counterpart of the watch offering the phone app.
class WatchApp(context: Context) {

    private val context = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(this.context)
    private val nodeClient = Wearable.getNodeClient(this.context)

    val presence: Flow<WatchPresence> = callbackFlow {
        val scope = this
        val listener = CapabilityClient.OnCapabilityChangedListener { info ->
            if (info.nodes.isNotEmpty()) {
                trySend(WatchPresence.WITH_APP)
            } else {
                scope.launch { trySend(runCatching { current() }.getOrDefault(WatchPresence.NONE)) }
            }
        }
        try {
            capabilityClient.addListener(listener, WatchSyncContract.WATCH_CAPABILITY).await()
            trySend(current())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Wearable API not available: ${e.message}")
            trySend(WatchPresence.NONE)
        }
        awaitClose { capabilityClient.removeListener(listener, WatchSyncContract.WATCH_CAPABILITY) }
    }

    // Opens Elly's Play Store page on every connected watch; false when none could be reached. The watch does not
    // always confirm, so a request without an answer counts as sent.
    suspend fun openStoreOnWatch(): Boolean {
        val nodes = try {
            nodeClient.connectedNodes.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Listing the watches failed: ${e.message}")
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(STORE_URI + context.packageName))
        val helper = RemoteActivityHelper(context)
        return nodes.map { node -> withTimeoutOrNull(CONFIRM_TIMEOUT_MILLIS) { open(helper, intent, node.id) } ?: true }.any { it }
    }

    private suspend fun current(): WatchPresence {
        val withApp = capabilityClient.getCapability(WatchSyncContract.WATCH_CAPABILITY, CapabilityClient.FILTER_ALL).await()
        if (withApp.nodes.isNotEmpty()) return WatchPresence.WITH_APP
        return if (nodeClient.connectedNodes.await().isNotEmpty()) WatchPresence.WITHOUT_APP else WatchPresence.NONE
    }

    private suspend fun open(helper: RemoteActivityHelper, intent: Intent, nodeId: String): Boolean {
        val future = helper.startRemoteActivity(intent, nodeId)
        return suspendCancellableCoroutine { continuation ->
            future.addListener({
                val opened = try {
                    future.get()
                    true
                } catch (e: Exception) {
                    Log.i(TAG, "Opening the Play Store on the watch failed: ${e.message}")
                    false
                }
                continuation.resume(opened)
            }, Runnable::run)
            continuation.invokeOnCancellation { future.cancel(false) }
        }
    }

    private companion object {
        const val TAG = "EllyWatchSync"
        const val STORE_URI = "market://details?id="
        const val CONFIRM_TIMEOUT_MILLIS = 3_000L
    }
}
