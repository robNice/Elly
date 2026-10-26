package de.robnice.elly.ui.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.robnice.elly.R
import de.robnice.elly.epg.AudioTrack

@Composable
fun AudioTrackDialog(
    picker: AudioTrackPicker,
    onSelect: (AudioTrack) -> Unit,
    onDescriptionChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_tracks_title)) },
        text = {
            when (picker) {
                AudioTrackPicker.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AudioTrackPicker.Message -> Text(stringResource(picker.text))
                is AudioTrackPicker.Tracks -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    picker.notice?.let { notice ->
                        Text(text = stringResource(notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    picker.tracks.tracks.forEach { track ->
                        TrackRow(track, selected = track.id == picker.tracks.currentId, onClick = { onSelect(track) })
                    }
                    picker.tracks.descriptionEnabled?.let { enabled ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.setup_item_spacing)))
                        DescriptionSwitch(enabled, onDescriptionChange)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.epg_info_close)) } }
    )
}

@Composable
private fun DescriptionSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(dimensionResource(R.dimen.setup_item_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.audio_tracks_description_setting), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.audio_tracks_description_setting_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_item_spacing)))
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun TrackRow(track: AudioTrack, selected: Boolean, onClick: () -> Unit) {
    val details = buildList {
        if (track.description) add(stringResource(R.string.audio_track_description))
        if (track.hearingImpaired) add(stringResource(R.string.audio_track_hearing_impaired))
        if (track.dolby) add(stringResource(R.string.audio_track_dolby))
    }
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.language.ifBlank { stringResource(R.string.audio_track_unknown) },
                    style = MaterialTheme.typography.bodyLarge
                )
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
