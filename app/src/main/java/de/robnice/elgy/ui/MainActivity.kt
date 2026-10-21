package de.robnice.elgy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.robnice.elgy.ElgyApplication
import de.robnice.elgy.ui.theme.ElgyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container
        get() = (application as ElgyApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElgyTheme {
                ElgyApp(container = container)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            container.applicationScope.launch { container.remoteControl.disconnect() }
        }
    }
}
