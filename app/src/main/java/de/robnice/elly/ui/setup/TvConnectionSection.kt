package de.robnice.elly.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elly.R
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.discovery.DiscoveredTv

@Composable
fun TvConnectionSection(viewModel: SetupViewModel, visible: Boolean, onPaired: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var removing by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) viewModel.scanIfUnpaired() else viewModel.stopScan()
    }

    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_section_spacing))) {
        if (state.showEmulatorHint) EmulatorHint()
        if (state.pairedTvs.isNotEmpty()) {
            PairedTvsSection(
                tvs = state.pairedTvs,
                activeTvId = state.activeTvId,
                onActivate = viewModel::activate,
                onRemove = { removing = it }
            )
        }
        DiscoverySection(state, onScan = viewModel::scan, onSelect = viewModel::pair)
        ManualIpSection(
            ip = state.manualIp,
            invalid = state.manualIpInvalid,
            onIpChange = viewModel::updateManualIp,
            onConnect = viewModel::connectManualIp
        )
    }

    PairingDialog(
        pairing = state.pairing,
        onCancel = viewModel::cancelPairing,
        onDismiss = viewModel::dismissPairingResult,
        onPaired = {
            viewModel.dismissPairingResult()
            onPaired()
        }
    )

    state.pairedTvs.firstOrNull { it.id == removing }?.let { tv ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.settings_disconnect_title)) },
            text = { Text(stringResource(R.string.settings_disconnect_message, tv.name)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    viewModel.remove(tv.id)
                }) { Text(stringResource(R.string.settings_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

@Composable
private fun EmulatorHint() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.setup_emulator_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding))
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun DiscoverySection(state: SetupUiState, onScan: () -> Unit, onSelect: (DiscoveredTv) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        SectionTitle(stringResource(R.string.setup_found_tvs))
        if (state.scanState == ScanState.SCANNING) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(dimensionResource(R.dimen.setup_progress_size)))
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.setup_item_spacing)))
                Text(stringResource(R.string.setup_scanning))
            }
        } else {
            Button(onClick = onScan) {
                Text(stringResource(if (state.scanState == ScanState.DONE) R.string.setup_scan_again else R.string.setup_scan))
            }
        }
        if (state.scanState == ScanState.DONE && state.foundTvs.isEmpty()) {
            Text(
                text = stringResource(R.string.setup_no_tvs_found),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.foundTvs.forEach { tv ->
            val paired = state.pairedTvs.any { it.ipAddress == tv.ipAddress }
            TvRow(
                title = tv.name ?: tv.ipAddress,
                subtitle = listOfNotNull(tv.modelName, tv.ipAddress.takeIf { tv.name != null }).joinToString(" · "),
                trailing = if (paired) stringResource(R.string.setup_already_paired) else null,
                onClick = { onSelect(tv) }
            )
        }
    }
}

@Composable
private fun TvRow(title: String, subtitle: String, trailing: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ManualIpSection(ip: String, invalid: Boolean, onIpChange: (String) -> Unit, onConnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        SectionTitle(stringResource(R.string.setup_manual_ip))
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.setup_ip_label)) },
            placeholder = { Text(stringResource(R.string.setup_ip_placeholder)) },
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.setup_ip_invalid)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onConnect() })
        )
        Button(onClick = onConnect, enabled = ip.isNotBlank()) {
            Text(stringResource(R.string.setup_connect))
        }
    }
}

@Composable
private fun PairedTvsSection(
    tvs: List<PairedTv>,
    activeTvId: String?,
    onActivate: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        SectionTitle(stringResource(R.string.setup_paired_tvs))
        tvs.forEach { tv ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .clickable { onActivate(tv.id) }
                        .padding(dimensionResource(R.dimen.setup_card_padding)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = tv.id == activeTvId, onClick = { onActivate(tv.id) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tv.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = listOfNotNull(tv.modelName, tv.ipAddress).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onRemove(tv.id) }) {
                        Text(stringResource(R.string.setup_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingDialog(
    pairing: PairingState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onPaired: () -> Unit
) {
    when (pairing) {
        PairingState.Idle -> Unit
        is PairingState.Connecting -> ProgressDialog(
            message = stringResource(R.string.pairing_connecting, pairing.ipAddress),
            onCancel = onCancel
        )
        is PairingState.AwaitingConfirmation -> ProgressDialog(
            message = stringResource(R.string.pairing_confirm_on_tv),
            onCancel = onCancel
        )
        is PairingState.Paired -> AlertDialog(
            onDismissRequest = onPaired,
            title = { Text(stringResource(R.string.pairing_success_title)) },
            text = { Text(stringResource(R.string.pairing_success_message, pairing.tv.name)) },
            confirmButton = { TextButton(onClick = onPaired) { Text(stringResource(R.string.dialog_ok)) } }
        )
        is PairingState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.pairing_failed_title)) },
            text = { Text(stringResource(pairing.error.messageRes())) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) } }
        )
    }
}

@Composable
private fun ProgressDialog(message: String, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(dimensionResource(R.dimen.setup_progress_size)))
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.setup_item_spacing)))
                Text(message)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

private fun PairingError.messageRes(): Int = when (this) {
    PairingError.UNREACHABLE -> R.string.pairing_error_unreachable
    PairingError.CERTIFICATE_CHANGED -> R.string.pairing_error_certificate_changed
    PairingError.REJECTED -> R.string.pairing_error_rejected
    PairingError.TIMEOUT -> R.string.pairing_error_timeout
    PairingError.UNKNOWN -> R.string.pairing_error_unknown
}
