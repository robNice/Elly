package de.robnice.elly.wear.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import de.robnice.elly.data.PairedTvJson
import de.robnice.elly.data.TvRepository
import de.robnice.elly.data.WatchLayoutJson
import de.robnice.elly.sync.WatchSyncContract
import de.robnice.elly.wear.data.AppIconStore
import de.robnice.elly.wear.data.WearSettings
import de.robnice.elly.wear.tile.EllyTileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

// Takes over the TVs and the demo mode the phone publishes.
class PhoneState(
    private val context: Context,
    private val tvRepository: TvRepository,
    private val settings: WearSettings,
    private val appIcons: AppIconStore
) {

    private val dataClient = Wearable.getDataClient(context.applicationContext)
    private val messageClient = Wearable.getMessageClient(context.applicationContext)
    private val capabilityClient = Wearable.getCapabilityClient(context.applicationContext)
    private val nodeClient = Wearable.getNodeClient(context.applicationContext)

    // Every phone with Elly on the same Google account publishes its own state, and the watch sees all of them.
    // Only the phone it is paired with counts; it is the nearby one, the others only reach it through the cloud.
    suspend fun accepts(sourceNodeId: String?): Boolean {
        val paired = pairedPhoneId() ?: return true
        return sourceNodeId == paired
    }

    private suspend fun pairedPhoneId(): String? = try {
        nodeClient.connectedNodes.await().firstOrNull { it.isNearby }?.id
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun apply(dataMap: DataMap) {
        val tvs = PairedTvJson.decode(dataMap.getString(WatchSyncContract.KEY_PAIRED_TVS))
        // A TV chosen on the watch stays active as long as the phone still knows it.
        val watchChoice = settings.watchTvId.first()?.takeIf { id -> tvs.any { it.id == id } }
        tvRepository.replaceAll(tvs, watchChoice ?: dataMap.getString(WatchSyncContract.KEY_ACTIVE_TV_ID))
        settings.setDemoMode(dataMap.getBoolean(WatchSyncContract.KEY_DEMO_MODE))
        val layout = dataMap.getString(WatchSyncContract.KEY_WATCH_LAYOUT)
        storeIcons(dataMap, WatchLayoutJson.decode(layout).apps.map { it.appId }.toSet())
        settings.setLayout(layout)
        EllyTileService.requestUpdate(context)
    }

    private suspend fun storeIcons(dataMap: DataMap, appIds: Set<String>) {
        withContext(Dispatchers.IO) {
            appIds.forEach { appId ->
                val asset = dataMap.getAsset(WatchSyncContract.KEY_APP_ICON_PREFIX + appId) ?: return@forEach
                try {
                    val bytes = dataClient.getFdForAsset(asset).await().inputStream.use { it.readBytes() }
                    appIcons.save(appId, bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.i(TAG, "Reading the icon of $appId failed: ${e.message}")
                }
            }
            appIcons.retain(appIds)
        }
        settings.iconsChanged()
    }

    // The phone removes the state when it no longer knows a watch with Elly.
    suspend fun clear() {
        tvRepository.replaceAll(emptyList(), null)
        settings.setDemoMode(false)
        settings.setLayout(null)
        withContext(Dispatchers.IO) { appIcons.retain(emptySet()) }
        EllyTileService.requestUpdate(context)
    }

    // A data item that arrived while the app was not installed yet raises no change event, so it is read on start.
    // Without one the phone is asked to publish it.
    suspend fun refresh() {
        try {
            val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(WatchSyncContract.STATE_PATH).build()
            val items = dataClient.getDataItems(uri).await()
            val paired = pairedPhoneId()
            val dataMap = try {
                items.firstOrNull { paired == null || it.uri.host == paired }?.let { DataMapItem.fromDataItem(it).dataMap }
            } finally {
                items.release()
            }
            if (dataMap != null) apply(dataMap) else requestFromPhone()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Reading the phone state failed: ${e.message}")
        }
    }

    private suspend fun requestFromPhone() {
        val phones = capabilityClient
            .getCapability(WatchSyncContract.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
        phones.forEach { node ->
            messageClient.sendMessage(node.id, WatchSyncContract.REQUEST_STATE_PATH, null).await()
        }
    }

    private companion object {
        const val TAG = "EllyWatchSync"
    }
}
