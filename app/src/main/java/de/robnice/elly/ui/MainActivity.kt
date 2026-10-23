package de.robnice.elly.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.robnice.elly.EllyApplication
import de.robnice.elly.ui.theme.EllyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container
        get() = (application as EllyApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        setContent {
            EllyTheme {
                EllyApp(container = container)
            }
        }
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
