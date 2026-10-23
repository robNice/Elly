package de.robnice.elly.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.colorResource
import de.robnice.elly.R

@Composable
fun EllyTheme(content: @Composable () -> Unit) {
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

// Elly's warm colors for the help, settings and policy pages; the remote keeps EllyTheme.
@Composable
fun EllyWarmTheme(content: @Composable () -> Unit) {
    val text = colorResource(R.color.help_text)
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = colorResource(R.color.help_heading),
            onBackground = text,
            onSurface = text,
            onSurfaceVariant = colorResource(R.color.help_text_muted)
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides text, content = content)
    }
}
