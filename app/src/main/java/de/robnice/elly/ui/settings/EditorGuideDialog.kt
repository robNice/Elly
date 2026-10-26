package de.robnice.elly.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import de.robnice.elly.R
import de.robnice.elly.ui.components.KeyIcon
import de.robnice.elly.ui.components.KeyStyle
import de.robnice.elly.ui.components.LocalKeyRounding
import de.robnice.elly.ui.components.keyBody
import de.robnice.elly.ui.components.keyShape
import de.robnice.elly.ui.components.palette

// How to edit a screen, each step next to a small picture drawn with the editor's own parts.
@Composable
fun EditorGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_editor_guide_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.custom_guide_spacing))
            ) {
                GuideStep(R.string.custom_editor_guide_move) { MoveArt() }
                GuideStep(R.string.custom_editor_guide_resize) { ResizeArt() }
                GuideStep(R.string.custom_editor_guide_select) { SelectArt() }
                GuideStep(R.string.custom_editor_guide_app) { AppArt() }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) } }
    )
}

@Composable
private fun GuideStep(@StringRes text: Int, art: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.custom_guide_spacing))
    ) {
        Box(
            modifier = Modifier.size(dimensionResource(R.dimen.custom_guide_art_width), dimensionResource(R.dimen.custom_guide_art_height)),
            contentAlignment = Alignment.Center
        ) { art() }
        Text(text = stringResource(text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SampleKey(
    size: Dp,
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.DARK,
    icon: Int = R.drawable.ic_power,
    width: Dp = size
) {
    val palette = style.palette()
    Box(
        modifier = modifier.size(width, size).keyBody(keyShape(LocalKeyRounding.current), palette, pressed = false),
        contentAlignment = Alignment.Center
    ) {
        KeyIcon(icon, palette.content, Modifier.size(size / 2))
    }
}

@Composable
private fun MoveArt() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SampleKey(dimensionResource(R.dimen.custom_guide_small_key))
        Text(text = stringResource(R.string.custom_editor_guide_arrow), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Trash(dragging = false, hovering = true, modifier = Modifier)
    }
}

@Composable
private fun ResizeArt() {
    val key = dimensionResource(R.dimen.custom_guide_key)
    val handle = dimensionResource(R.dimen.custom_editor_handle_size)
    Box(modifier = Modifier.size(key + handle / 2)) {
        SampleKey(
            key,
            Modifier.border(
                dimensionResource(R.dimen.custom_editor_outline),
                colorResource(R.color.custom_editor_selection),
                keyShape(LocalKeyRounding.current)
            )
        )
        Handle(
            icon = R.drawable.ic_resize,
            description = R.string.custom_editor_resize,
            color = colorResource(R.color.custom_editor_selection),
            modifier = Modifier.offset(key - handle / 2, key - handle / 2)
        )
    }
}

@Composable
private fun SelectArt() {
    val color = colorResource(R.color.custom_editor_selection)
    val stroke = with(LocalDensity.current) { dimensionResource(R.dimen.custom_editor_outline).toPx() }
    val small = dimensionResource(R.dimen.custom_guide_small_key)
    Box(modifier = Modifier.fillMaxSize()) {
        SampleKey(small, Modifier.align(Alignment.CenterStart).offset(x = small / 3))
        SampleKey(small, Modifier.align(Alignment.CenterEnd).offset(x = -small / 3), icon = R.drawable.ic_volume_off)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color.copy(alpha = MARQUEE_FILL_ALPHA))
            drawRect(color, style = Stroke(stroke))
        }
    }
}

@Composable
private fun AppArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SampleKey(
            dimensionResource(R.dimen.custom_guide_app_height),
            style = KeyStyle.LIGHT,
            icon = R.drawable.ic_apps,
            width = dimensionResource(R.dimen.custom_guide_app_width)
        )
        Text(
            text = stringResource(R.string.custom_editor_guide_hold),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
