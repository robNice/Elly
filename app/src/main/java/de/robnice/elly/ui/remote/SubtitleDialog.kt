package de.robnice.elly.ui.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.robnice.elly.R
import de.robnice.elly.epg.SubtitleTrack

// Like the audio tracks, with "Off" on top; onSelect gets null for off.
@Composable
fun SubtitleDialog(picker: SubtitlePicker, onSelect: (Int?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subtitles_title)) },
        text = {
            when (picker) {
                SubtitlePicker.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is SubtitlePicker.Message -> Text(stringResource(picker.text))
                is SubtitlePicker.Tracks -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    picker.notice?.let { notice ->
                        Text(text = stringResource(notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    SubtitleRow(
                        title = stringResource(R.string.subtitles_off),
                        details = emptyList(),
                        selected = picker.tracks.currentId == null,
                        onClick = { onSelect(null) }
                    )
                    picker.tracks.tracks.forEach { track ->
                        SubtitleRow(
                            title = track.language.ifBlank { stringResource(R.string.audio_track_unknown) },
                            details = details(track),
                            selected = track.id == picker.tracks.currentId,
                            onClick = { onSelect(track.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.epg_info_close)) } }
    )
}

@Composable
private fun details(track: SubtitleTrack): List<String> = buildList {
    if (track.hearingImpaired) add(stringResource(R.string.audio_track_hearing_impaired))
    if (track.teletext) add(stringResource(R.string.subtitle_teletext))
}

@Composable
private fun SubtitleRow(title: String, details: List<String>, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.audio_track_selected),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size))
                )
            }
        }
    }
}
