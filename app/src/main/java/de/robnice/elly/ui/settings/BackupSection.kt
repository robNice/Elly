package de.robnice.elly.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elly.R
import java.time.LocalDate

@Composable
fun BackupSection(viewModel: SettingsViewModel, screensAvailable: Boolean) {
    val resolver = LocalContext.current.contentResolver
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    var choosing by remember { mutableStateOf(false) }
    var saveScreens by rememberSaveable { mutableStateOf(screensAvailable) }
    var saveAppearance by rememberSaveable { mutableStateOf(true) }
    val backupFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportBackup(resolver, it, saveScreens && screensAvailable, saveAppearance) }
    }
    val restoreFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.readBackup(resolver, it) }
    }
    val bothName = stringResource(R.string.settings_backup_file)
    val screensName = stringResource(R.string.settings_backup_screens_file)
    val appearanceName = stringResource(R.string.settings_backup_appearance_file)

    if (choosing) {
        val screens = saveScreens && screensAvailable
        BackupPartsDialog(
            title = stringResource(R.string.settings_backup_save_title),
            message = null,
            screens = if (screensAvailable) screens else null,
            appearance = saveAppearance,
            onScreens = { saveScreens = it },
            onAppearance = { saveAppearance = it },
            confirm = stringResource(R.string.settings_backup_save),
            onConfirm = {
                choosing = false
                val name = when {
                    screens && saveAppearance -> bothName
                    screens -> screensName
                    else -> appearanceName
                }
                backupFile.launch(fileName(name))
            },
            onDismiss = { choosing = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(
            text = stringResource(R.string.settings_backup),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_backup_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            BackupButton(R.drawable.ic_backup_save, stringResource(R.string.settings_backup_save), Modifier.weight(1f)) { choosing = true }
            BackupButton(R.drawable.ic_backup_load, stringResource(R.string.settings_backup_restore), Modifier.weight(1f)) {
                restoreFile.launch(OPEN_TYPES)
            }
        }
    }
}

@Composable
private fun BackupButton(icon: Int, text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.settings_backup_icon))
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.settings_backup_icon_spacing)))
        Text(text, textAlign = TextAlign.Center)
    }
}

// Switches for the parts of a backup; a part that is null is not offered. At least one part has to stay on.
@Composable
private fun BackupPartsDialog(
    title: String,
    message: String?,
    screens: Boolean?,
    appearance: Boolean?,
    onScreens: (Boolean) -> Unit,
    onAppearance: (Boolean) -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                screens?.let {
                    PartSwitch(
                        title = stringResource(R.string.settings_backup_part_screens),
                        description = stringResource(R.string.settings_backup_part_screens_description),
                        checked = it,
                        onCheckedChange = onScreens
                    )
                }
                appearance?.let {
                    PartSwitch(
                        title = stringResource(R.string.settings_backup_part_appearance),
                        description = stringResource(R.string.settings_backup_part_appearance_description),
                        checked = it,
                        onCheckedChange = onAppearance
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = screens == true || appearance == true) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun PartSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = dimensionResource(R.dimen.setup_item_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_card_padding)))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun BackupDialogs(viewModel: SettingsViewModel) {
    val backup by viewModel.backupToRestore.collectAsStateWithLifecycle()
    val message by viewModel.backupMessage.collectAsStateWithLifecycle()
    backup?.let { pending ->
        var screens by remember(pending) { mutableStateOf(pending.screens != null) }
        var appearance by remember(pending) { mutableStateOf(pending.appearance != null) }
        BackupPartsDialog(
            title = stringResource(R.string.settings_backup_restore_title),
            message = stringResource(R.string.settings_backup_restore_message),
            screens = screens.takeIf { pending.screens != null },
            appearance = appearance.takeIf { pending.appearance != null },
            onScreens = { screens = it },
            onAppearance = { appearance = it },
            confirm = stringResource(R.string.settings_backup_restore_confirm),
            onConfirm = { viewModel.confirmRestore(screens, appearance) },
            onDismiss = viewModel::dismissRestore
        )
    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = viewModel::dismissBackupMessage,
            text = { Text(stringResource(text)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBackupMessage) { Text(stringResource(R.string.dialog_ok)) }
            }
        )
    }
}

private fun fileName(base: String) = "$base-${LocalDate.now()}.json"

private const val MIME_TYPE = "application/json"

// File managers report .json files under different types.
private val OPEN_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")
