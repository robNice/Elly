package de.robnice.elly.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import de.robnice.elly.wear.WearApplication
import kotlinx.coroutines.launch

class WearActivity : ComponentActivity() {

    private val container get() = (application as WearApplication).container

    private val viewModel: WearRemoteViewModel by viewModels { WearRemoteViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearRemoteApp(viewModel, onDismissed = ::finish) }
    }

    override fun onStart() {
        super.onStart()
        container.wifiNetwork.acquire()
        container.applicationScope.launch {
            container.phoneState.refresh()
            container.wifiNetwork.awaitAvailable()
            container.remoteControl.checkConnection()
        }
    }

    // The session runs over the Wi-Fi network that is released here, so it is closed as well.
    override fun onStop() {
        super.onStop()
        viewModel.pointerShown(false)
        container.wifiNetwork.release()
        container.applicationScope.launch { container.remoteControl.disconnect() }
    }
}
