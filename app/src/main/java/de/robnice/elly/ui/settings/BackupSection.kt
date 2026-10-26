package de.robnice.elly.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elly.R
import de.robnice.elly.data.Backup
import java.time.LocalDate

@Composable
fun BackupSection(viewModel: SettingsViewModel, screensAvailable: Boolean) {
    val resolver = LocalContext.current.contentResolver
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    val screensFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportScreens(resolver, it) }
    }
    val appearanceFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportAppearance(resolver, it) }
    }
    val restoreFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.readBackup(resolver, it) }
    }
    val screensName = stringResource(R.string.settings_backup_screens_file)
    val appearanceName = stringResource(R.string.settings_backup_appearance_file)

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
            if (screensAvailable) {
                OutlinedButton(onClick = { screensFile.launch(fileName(screensName)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_backup_screens), textAlign = TextAlign.Center)
                }
            }
            OutlinedButton(onClick = { appearanceFile.launch(fileName(appearanceName)) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_backup_appearance), textAlign = TextAlign.Center)
            }
        }
        OutlinedButton(onClick = { restoreFile.launch(OPEN_TYPES) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_backup_restore))
        }
    }
}

@Composable
fun BackupDialogs(viewModel: SettingsViewModel) {
    val backup by viewModel.backupToRestore.collectAsStateWithLifecycle()
    val message by viewModel.backupMessage.collectAsStateWithLifecycle()
    backup?.let { pending ->
        val screens = pending is Backup.Screens
        AlertDialog(
            onDismissRequest = viewModel::dismissRestore,
            title = {
                Text(stringResource(if (screens) R.string.settings_backup_restore_screens_title else R.string.settings_backup_restore_appearance_title))
            },
            text = {
                Text(stringResource(if (screens) R.string.settings_backup_restore_screens_message else R.string.settings_backup_restore_appearance_message))
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) { Text(stringResource(R.string.settings_backup_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestore) { Text(stringResource(R.string.dialog_cancel)) }
            }
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
