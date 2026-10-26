package de.robnice.elly.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elly.R

@Composable
fun RootAccessSection(viewModel: RootAccessViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.available) return

    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        SwitchSetting(
            title = stringResource(R.string.settings_root_ssh),
            description = stringResource(R.string.settings_root_ssh_description),
            checked = state.enabled,
            onCheckedChange = { if (!state.removing) viewModel.setEnabled(it) }
        )
        if (state.enabled || state.removing) {
            RootAccessStatus(state, onCheck = viewModel::check)
        }
        if (state.enabled && state.oldKey && !state.removing) {
            OldKeyNotice(onRemove = viewModel::askOldKeyRemoval)
        }
    }

    state.form?.let { form ->
        val resolver = LocalContext.current.contentResolver
        val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importKey(resolver, uri)
        }
        SshSetupDialog(
            form = form,
            onPortChange = { viewModel.updateForm(port = it) },
            onUserChange = { viewModel.updateForm(user = it) },
            onMethodChange = { viewModel.updateForm(method = it) },
            onPasswordChange = { viewModel.updateForm(password = it) },
            onKeyChange = { viewModel.updateForm(key = it) },
            onPickKeyFile = { keyPicker.launch(arrayOf(ANY_FILE)) },
            onPassphraseChange = { viewModel.updateForm(passphrase = it) },
            onSubmit = viewModel::submitForm,
            onDismiss = viewModel::dismissForm
        )
    }

    if (state.confirmRemove) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRemove,
            title = { Text(stringResource(R.string.settings_root_ssh_remove_title)) },
            text = { Text(stringResource(R.string.settings_root_ssh_remove_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRemove) { Text(stringResource(R.string.settings_root_ssh_remove)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemove) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    if (state.confirmOldKeyRemoval) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOldKeyRemoval,
            title = { Text(stringResource(R.string.settings_root_ssh_old_key_remove_title)) },
            text = { Text(stringResource(R.string.settings_root_ssh_old_key_remove_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOldKeyRemoval) { Text(stringResource(R.string.settings_root_ssh_old_key_remove)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOldKeyRemoval) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    if (state.oldKeyRemoved) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOldKeyRemoved,
            text = { Text(stringResource(R.string.settings_root_ssh_old_key_removed)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOldKeyRemoved) { Text(stringResource(R.string.dialog_ok)) }
            }
        )
    }
}

@Composable
private fun RootAccessStatus(state: RootAccessUiState, onCheck: () -> Unit) {
    val busy = state.removing || state.check == RootAccessCheck.CHECKING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.setup_card_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val text = when {
            state.removing -> stringResource(R.string.settings_root_ssh_removing)
            state.check == RootAccessCheck.CHECKING -> stringResource(R.string.settings_root_ssh_checking)
            state.check == RootAccessCheck.OK -> stringResource(R.string.settings_root_ssh_ok)
            state.check == RootAccessCheck.FAILED -> stringResource(state.checkError.checkMessage())
            else -> null
        }
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen.setup_item_spacing))
                    .size(dimensionResource(R.dimen.root_access_progress_size)),
                strokeWidth = dimensionResource(R.dimen.root_access_progress_stroke)
            )
        }
        Text(
            text = text.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.check == RootAccessCheck.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCheck, enabled = !busy) { Text(stringResource(R.string.settings_root_ssh_check)) }
    }
}

@Composable
private fun OldKeyNotice(onRemove: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.setup_card_padding))) {
        Text(
            text = stringResource(R.string.settings_root_ssh_old_key_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.settings_root_ssh_old_key_remove))
        }
    }
}

@Composable
private fun SshSetupDialog(
    form: SshSetupForm,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onMethodChange: (SshMethod) -> Unit,
    onPasswordChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onPickKeyFile: () -> Unit,
    onPassphraseChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_root_ssh_setup_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                Text(
                    text = stringResource(R.string.settings_root_ssh_setup_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                    OutlinedTextField(
                        value = form.user,
                        onValueChange = onUserChange,
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        enabled = !form.busy,
                        label = { Text(stringResource(R.string.settings_root_ssh_user)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = form.port,
                        onValueChange = onPortChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !form.busy,
                        isError = form.portNumber == null,
                        label = { Text(stringResource(R.string.settings_root_ssh_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }
                Column {
                    MethodOption(
                        title = stringResource(R.string.settings_root_ssh_method_password),
                        selected = form.method == SshMethod.PASSWORD,
                        enabled = !form.busy,
                        onSelect = { onMethodChange(SshMethod.PASSWORD) }
                    )
                    MethodOption(
                        title = stringResource(R.string.settings_root_ssh_method_key),
                        selected = form.method == SshMethod.KEY,
                        enabled = !form.busy,
                        onSelect = { onMethodChange(SshMethod.KEY) }
                    )
                    Text(
                        text = stringResource(R.string.settings_root_ssh_method_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (form.method == SshMethod.PASSWORD) {
                    OutlinedTextField(
                        value = form.password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !form.busy,
                        label = { Text(stringResource(R.string.settings_root_ssh_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                } else {
                    OutlinedTextField(
                        value = form.key,
                        onValueChange = onKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !form.busy,
                        minLines = KEY_MIN_LINES,
                        maxLines = KEY_MAX_LINES,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        label = { Text(stringResource(R.string.settings_root_ssh_key)) },
                        placeholder = { Text(stringResource(R.string.settings_root_ssh_key_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    TextButton(onClick = onPickKeyFile, enabled = !form.busy) {
                        Text(stringResource(R.string.settings_root_ssh_key_file))
                    }
                    OutlinedTextField(
                        value = form.passphrase,
                        onValueChange = onPassphraseChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !form.busy,
                        label = { Text(stringResource(R.string.settings_root_ssh_passphrase)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                }
                form.error?.let {
                    Text(
                        text = stringResource(it.setupMessage(form.method)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (form.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(R.dimen.root_access_progress_size)),
                    strokeWidth = dimensionResource(R.dimen.root_access_progress_stroke)
                )
            } else {
                TextButton(onClick = onSubmit, enabled = form.canSubmit) {
                    Text(stringResource(R.string.settings_root_ssh_setup_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.busy) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun MethodOption(title: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.setup_item_spacing))
        )
    }
}

private const val ANY_FILE = "*/*"
private const val KEY_MIN_LINES = 3
private const val KEY_MAX_LINES = 6

@StringRes
private fun SshError.setupMessage(method: SshMethod): Int = when (this) {
    SshError.AUTH_FAILED -> if (method == SshMethod.PASSWORD) R.string.settings_root_ssh_error_auth else R.string.settings_root_ssh_error_auth_key
    SshError.KEY_INVALID -> R.string.settings_root_ssh_error_key_invalid
    SshError.KEY_PASSPHRASE -> R.string.settings_root_ssh_error_passphrase
    SshError.KEY_FILE -> R.string.settings_root_ssh_error_key_file
    else -> commonMessage()
}

@StringRes
private fun SshError?.checkMessage(): Int = when (this) {
    SshError.AUTH_FAILED -> R.string.settings_root_ssh_error_key
    null -> R.string.settings_root_ssh_error_unknown
    else -> commonMessage()
}

@StringRes
private fun SshError.commonMessage(): Int = when (this) {
    SshError.UNREACHABLE -> R.string.settings_root_ssh_error_unreachable
    SshError.HOST_KEY_CHANGED -> R.string.settings_root_ssh_error_host_key
    SshError.NO_LUNA -> R.string.settings_root_ssh_error_luna
    SshError.KEY_INVALID -> R.string.settings_root_ssh_error_key_invalid
    SshError.KEY_PASSPHRASE -> R.string.settings_root_ssh_error_passphrase
    SshError.KEY_FILE -> R.string.settings_root_ssh_error_key_file
    SshError.AUTH_FAILED, SshError.UNKNOWN -> R.string.settings_root_ssh_error_unknown
}
