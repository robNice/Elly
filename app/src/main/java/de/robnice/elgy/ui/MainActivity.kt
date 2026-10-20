package de.robnice.elgy.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.robnice.elgy.ui.pager.RemotePager
import de.robnice.elgy.ui.theme.ElgyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElgyTheme {
                RemotePager()
            }
        }
    }
}
