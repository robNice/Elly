package de.robnice.elly.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import de.robnice.elly.EllyApplication
import de.robnice.elly.sync.WatchSyncContract
import de.robnice.elly.ui.theme.EllyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container
        get() = (application as EllyApplication).container

    // Counts the requests from the watch to show the privacy policy, so each one opens it again.
    private var privacyRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        if (savedInstanceState == null) handleLink(intent)
        setContent {
            EllyTheme {
                EllyApp(container = container, privacyRequests = privacyRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent) {
        if (intent.data?.host == WatchSyncContract.PHONE_LINK_PRIVACY_HOST) privacyRequests++
    }

    override fun onStart() {
        super.onStart()
        container.widgetController.appVisible = true
    }

    override fun onStop() {
        super.onStop()
        container.widgetController.appVisible = false
        if (!isChangingConfigurations) {
            container.applicationScope.launch { container.remoteControl.disconnect() }
        }
    }
}
