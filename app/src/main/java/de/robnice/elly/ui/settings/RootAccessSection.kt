package de.robnice.elly.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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
    }

    state.form?.let { form ->
        SshSetupDialog(
            form = form,
            onPortChange = { viewModel.updateForm(port = it) },
            onUserChange = { viewModel.updateForm(user = it) },
            onPasswordChange = { viewModel.updateForm(password = it) },
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

    if (state.removedOnlyLocally) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRemovedNotice,
            text = { Text(stringResource(R.string.settings_root_ssh_removed_locally)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissRemovedNotice) { Text(stringResource(R.string.dialog_ok)) }
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
private fun SshSetupDialog(
    form: SshSetupForm,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_root_ssh_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
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
                form.error?.let {
                    Text(
                        text = stringResource(it.setupMessage()),
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

@StringRes
private fun SshError.setupMessage(): Int = when (this) {
    SshError.AUTH_FAILED -> R.string.settings_root_ssh_error_auth
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
    SshError.AUTH_FAILED, SshError.UNKNOWN -> R.string.settings_root_ssh_error_unknown
}
