package de.robnice.elly.ui.epg

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.robnice.elly.epg.EpgAction
import de.robnice.elly.epg.EpgActionException
import de.robnice.elly.epg.EpgReservation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import de.robnice.elly.R
import de.robnice.elly.epg.EpgChannel
import de.robnice.elly.epg.EpgProgram
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EpgProgramDialog(
    channel: EpgChannel,
    program: EpgProgram,
    reminder: EpgReservation?,
    recording: EpgReservation?,
    canRecord: Boolean,
    onAction: suspend (EpgAction) -> Unit,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember(program) { mutableStateOf(false) }
    var error by remember(program) { mutableStateOf<String?>(null) }
    val failedText = stringResource(R.string.epg_action_failed)
    fun act(action: EpgAction) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                onAction(action)
            } catch (e: CancellationException) {
                throw e
            } catch (e: EpgActionException) {
                error = e.reason ?: failedText
            } catch (_: Exception) {
                error = failedText
            } finally {
                busy = false
            }
        }
    }
    val now = System.currentTimeMillis()
    val zone = remember { ZoneId.systemDefault() }
    val time = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val day = remember {
        val locale = Locale.getDefault()
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DAY_PATTERN), locale)
    }
    val start = Instant.ofEpochMilli(program.start).atZone(zone)
    val end = Instant.ofEpochMilli(program.end).atZone(zone)
    val minutes = ((program.end - program.start) / MINUTE_MILLIS).toInt()
    // The TV often repeats the short description at the start of the long one.
    val texts = listOf(program.subtitle, program.description, program.details)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(program.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = dimensionResource(R.dimen.epg_info_max_height))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                Text(
                    text = stringResource(
                        R.string.epg_info_when,
                        channel.name,
                        day.format(start),
                        time.format(start),
                        time.format(end),
                        minutes
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (program.end > now) {
                    if (program.start > now) {
                        OutlinedButton(
                            onClick = { act(if (reminder != null) EpgAction.Cancel(reminder) else EpgAction.Remind(channel, program)) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(if (reminder != null) R.string.epg_remind_cancel else R.string.epg_remind)) }
                    }
                    val recordData = program.offlineProgramId != null || channel.channelCode != null
                    val recordable = canRecord && recordData
                    OutlinedButton(
                        onClick = { act(if (recording != null) EpgAction.Cancel(recording) else EpgAction.Record(channel, program)) },
                        enabled = !busy && (recording != null || recordable),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(if (recording != null) R.string.epg_record_cancel else R.string.epg_record)) }
                    when {
                        busy -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        error != null -> Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        recording == null && !canRecord -> Hint(R.string.epg_record_no_storage)
                        recording == null && !recordData -> Hint(R.string.epg_record_no_data)
                    }
                }
                if (program.genre.isNotBlank()) {
                    Text(
                        text = program.genre,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                texts.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSwitch) { Text(stringResource(R.string.epg_info_switch)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.epg_info_close)) }
        }
    )
}

@Composable
private fun Hint(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val MINUTE_MILLIS = 60_000L
private const val DAY_PATTERN = "EEEEdMMMM"
