package de.robnice.elly.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.robnice.elly.R
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenLayout

@Composable
fun ScreensSection(
    layout: ScreenLayout,
    screens: Map<RemoteScreen, CustomScreen>,
    onToggle: (RemoteScreen) -> Unit,
    onReorder: (List<RemoteScreen>) -> Unit,
    onEdit: (RemoteScreen) -> Unit,
    onReset: (RemoteScreen) -> Unit
) {
    val spacing = dimensionResource(R.dimen.setup_item_spacing)

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(text = stringResource(R.string.settings_screens), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.settings_screens_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            ReorderableRow(
                items = layout.entries,
                onClick = { onToggle(it.screen) },
                onReorder = { entries -> onReorder(entries.map { it.screen }) },
                modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding))
            ) { entry, index ->
                ScreenThumbnail(
                    layout = screens[entry.screen] ?: CustomScreen.EMPTY,
                    enabled = entry.enabled,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.screen_thumbnail_checkbox_spacing)))
                Checkbox(checked = entry.enabled, onCheckedChange = null)
                val number = index + 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenAction(R.drawable.ic_edit, stringResource(R.string.settings_screen_edit, number)) {
                        onEdit(entry.screen)
                    }
                    ScreenAction(R.drawable.ic_restore, stringResource(R.string.settings_screen_reset, number)) {
                        onReset(entry.screen)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenAction(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(dimensionResource(R.dimen.screen_action_size))) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(dimensionResource(R.dimen.screen_action_icon_size))
        )
    }
}
