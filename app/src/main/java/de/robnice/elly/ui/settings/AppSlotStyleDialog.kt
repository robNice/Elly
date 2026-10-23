package de.robnice.elly.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import de.robnice.elly.R
import de.robnice.elly.data.model.AppSlot

@Composable
fun AppSlotStyleDialog(
    title: String,
    slot: AppSlot,
    onSave: (showIcon: Boolean, showText: Boolean, customText: String, textColor: Long?) -> Unit,
    onDismiss: () -> Unit,
    @StringRes confirmLabel: Int = R.string.dialog_save
) {
    var showIcon by rememberSaveable { mutableStateOf(slot.showIcon) }
    var showText by rememberSaveable { mutableStateOf(slot.showText || !slot.showIcon) }
    var customText by rememberSaveable {
        mutableStateOf(slot.customText ?: if (showText) slot.title.orEmpty() else "")
    }
    var textColor by rememberSaveable { mutableStateOf(slot.textColor) }
    val palette = labelPalette()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                CheckRow(
                    text = stringResource(R.string.app_style_show_icon),
                    checked = showIcon,
                    enabled = showText,
                    onCheckedChange = { showIcon = it }
                )
                if (showIcon && slot.iconPath == null) {
                    Text(
                        text = stringResource(R.string.app_style_icon_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CheckRow(
                    text = stringResource(R.string.app_style_show_text),
                    checked = showText,
                    enabled = showIcon,
                    onCheckedChange = {
                        showText = it
                        if (it && customText.isBlank()) customText = slot.title.orEmpty()
                    }
                )
                if (showText) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.app_style_custom_text)) },
                        placeholder = { Text(slot.title.orEmpty()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = stringResource(R.string.app_style_text_color), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
                    ) {
                        ColorSwatch(color = null, selected = textColor == null) { textColor = null }
                        palette.forEach { color ->
                            val argb = color.toArgb().toLong() and ARGB_MASK
                            ColorSwatch(color = color, selected = textColor == argb) { textColor = argb }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(showIcon, showText, customText, textColor) }) {
                Text(stringResource(confirmLabel))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun CheckRow(text: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColorSwatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.color_swatch_size))
            .border(dimensionResource(R.dimen.color_swatch_border), borderColor, CircleShape)
            .background(color ?: colorResource(R.color.key_light_top), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Text(
                text = stringResource(R.string.app_style_color_auto),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.key_content_on_light)
            )
        }
    }
}

@Composable
private fun labelPalette(): List<Color> = listOf(
    colorResource(R.color.app_label_dark),
    colorResource(R.color.app_label_red),
    colorResource(R.color.app_label_orange),
    colorResource(R.color.app_label_green),
    colorResource(R.color.app_label_teal),
    colorResource(R.color.app_label_blue),
    colorResource(R.color.app_label_purple)
)

private const val ARGB_MASK = 0xFFFFFFFFL
