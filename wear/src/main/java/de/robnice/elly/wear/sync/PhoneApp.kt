package de.robnice.elly.wear.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import de.robnice.elly.sync.WatchSyncContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

// Whether the phone has Elly. The watch app can be installed first from the Play Store on the watch, and then
// it has to send the user to the phone.
class PhoneApp(context: Context) {

    private val context = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(this.context)

    val status: Flow<Status> = callbackFlow {
        // There is no Play Store on an iPhone and Elly exists only for Android.
        if (PhoneTypeHelper.getPhoneDeviceType(context) == PhoneTypeHelper.DEVICE_TYPE_IOS) {
            trySend(Status.NOT_ANDROID)
            awaitClose()
            return@callbackFlow
        }
        val listener = CapabilityClient.OnCapabilityChangedListener { trySend(it.nodes.status()) }
        try {
            capabilityClient.addListener(listener, WatchSyncContract.PHONE_CAPABILITY).await()
            val info = capabilityClient.getCapability(WatchSyncContract.PHONE_CAPABILITY, CapabilityClient.FILTER_ALL).await()
            trySend(info.nodes.status())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Wearable API not available: ${e.message}")
            trySend(Status.INSTALLED)
        }
        awaitClose { capabilityClient.removeListener(listener, WatchSyncContract.PHONE_CAPABILITY) }
    }

    // Each returns false when the phone could not be reached.
    suspend fun openStoreOnPhone(): Boolean = openOnPhone(STORE_URI + context.packageName)

    suspend fun openAppOnPhone(): Boolean = openOnPhone(WatchSyncContract.PHONE_LINK_OPEN)

    suspend fun openPrivacyOnPhone(): Boolean = openOnPhone(WatchSyncContract.PHONE_LINK_PRIVACY)

    private suspend fun openOnPhone(uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(uri))
        val future = RemoteActivityHelper(context).startRemoteActivity(intent)
        return suspendCancellableCoroutine { continuation ->
            future.addListener({
                val opened = try {
                    future.get()
                    true
                } catch (e: Exception) {
                    Log.i(TAG, "Opening $uri on the phone failed: ${e.message}")
                    false
                }
                continuation.resume(opened)
            }, Runnable::run)
            continuation.invokeOnCancellation { future.cancel(false) }
        }
    }

    private fun Collection<*>.status(): Status = if (isEmpty()) Status.MISSING else Status.INSTALLED

    enum class Status { CHECKING, INSTALLED, MISSING, NOT_ANDROID }

    private companion object {
        const val TAG = "EllyWatchSync"
        const val STORE_URI = "market://details?id="
    }
}
