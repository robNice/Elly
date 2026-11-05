package de.robnice.elly.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import de.robnice.elly.sync.WatchSyncContract
import de.robnice.elly.wear.WearApplication
import kotlinx.coroutines.runBlocking

// Started by Google Play services whenever the phone changes its published state, even while the app is closed.
class PhoneStateListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val phoneState = (application as WearApplication).container.phoneState
        val event = dataEvents.lastOrNull { it.dataItem.uri.path == WatchSyncContract.STATE_PATH } ?: return
        val dataMap = if (event.type == DataEvent.TYPE_CHANGED) DataMapItem.fromDataItem(event.dataItem).dataMap else null
        runBlocking {
            if (!phoneState.accepts(event.dataItem.uri.host)) return@runBlocking
            if (dataMap != null) phoneState.apply(dataMap) else phoneState.clear()
        }
    }
}
