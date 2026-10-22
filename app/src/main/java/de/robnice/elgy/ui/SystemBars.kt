package de.robnice.elgy.ui

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import de.robnice.elgy.R

// The app is always dark, so the system bars get light icons whatever the system theme is.
// Up to Android 11 the status bar keeps its own dark background instead of showing the
// remote's background behind it.
fun ComponentActivity.enableDarkEdgeToEdge() {
    val statusBar = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) getColor(R.color.remote_background) else Color.TRANSPARENT
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(statusBar),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )
}
