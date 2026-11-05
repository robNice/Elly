package de.robnice.elly.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import de.robnice.elly.EllyApplication
import kotlinx.coroutines.runBlocking

// Started by Google Play services when the watch app finds no state, e.g. right after it was installed.
class WatchRequestService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchSyncContract.REQUEST_STATE_PATH) return
        val publisher = (application as EllyApplication).container.watchStatePublisher
        runBlocking { publisher.publishNow() }
    }
}
