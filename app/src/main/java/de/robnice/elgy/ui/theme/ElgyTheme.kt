package de.robnice.elgy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import de.robnice.elgy.R

/**
 * Dark-only theme of the remote. All color values live in res/values/colors.xml.
 */
@Composable
fun ElgyTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = colorResource(R.color.remote_accent),
        onPrimary = colorResource(R.color.remote_background),
        background = colorResource(R.color.remote_background),
        onBackground = colorResource(R.color.remote_on_surface),
        surface = colorResource(R.color.remote_surface),
        onSurface = colorResource(R.color.remote_on_surface),
        surfaceVariant = colorResource(R.color.remote_surface_highlight),
        onSurfaceVariant = colorResource(R.color.remote_on_surface_muted),
        error = colorResource(R.color.remote_power)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
