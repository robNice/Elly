package de.robnice.elgy.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.robnice.elgy.R
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.ui.remote.AppKey

@Composable
fun CustomAppDialog(
    app: AppSlot?,
    onChooseApp: () -> Unit,
    onStyle: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val usable = app?.isUsable == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_app_menu_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                if (app != null && usable) {
                    AppKey(
                        slot = app,
                        onPress = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(R.dimen.custom_app_preview_height))
                    )
                } else {
                    Text(
                        text = stringResource(R.string.custom_app_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MenuButton(R.drawable.ic_apps, R.string.custom_app_menu_choose, enabled = true, onClick = onChooseApp)
                MenuButton(R.drawable.ic_palette, R.string.custom_app_menu_style, enabled = usable, onClick = onStyle)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun MenuButton(@DrawableRes icon: Int, @StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.custom_picker_icon_size))
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.custom_picker_item_padding)))
        Text(text = stringResource(label), modifier = Modifier.weight(1f))
    }
}
