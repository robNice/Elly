package de.robnice.elly.ui.epg

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import de.robnice.elly.ui.components.currentPaletteColors
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import de.robnice.elly.R
import de.robnice.elly.data.model.EpgConfig
import de.robnice.elly.epg.EpgAction
import de.robnice.elly.epg.EpgChannel
import de.robnice.elly.epg.EpgGuide
import de.robnice.elly.epg.EpgReservation
import de.robnice.elly.epg.EpgVideoInfo
import de.robnice.elly.epg.FavoriteList
import de.robnice.elly.ui.settings.EpgPickerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Whether the program guide can be used; the editor greys the element out when it cannot.
val LocalEpgAvailable = compositionLocalOf { true }

// Guide data for the elements on the remote screens; absent in the editor.
class EpgGuides(
    val guide: (String) -> Flow<EpgGuide?>,
    val refreshing: StateFlow<Boolean>,
    val failed: StateFlow<Boolean>,
    val currentChannel: StateFlow<String?>,
    val switchChannel: (String) -> Unit,
    val reservations: StateFlow<List<EpgReservation>>,
    val canRecord: StateFlow<Boolean>,
    val perform: suspend (EpgAction) -> Unit,
    val videoInfo: suspend (EpgChannel) -> EpgVideoInfo?,
    val programImage: suspend (String) -> ByteArray?,
    val openAudioTracks: () -> Unit,
    val openSubtitles: () -> Unit
)

val LocalEpgGuides = compositionLocalOf<EpgGuides?> { null }

// Counts up each time the page comes into view or the app comes back; null while the page is not the one shown.
val LocalPageShown = compositionLocalOf<Int?> { null }

// On the remote screens the guide grid as soon as data is there; in the editor and while loading a stand-in.
@Composable
fun EpgElement(config: EpgConfig?, editing: Boolean, modifier: Modifier = Modifier) {
    val guides = LocalEpgGuides.current
    if (!editing && guides != null && config != null) {
        val flow = remember(guides, config.listId) { guides.guide(config.listId) }
        val guide by flow.collectAsState(initial = null)
        val refreshing by guides.refreshing.collectAsState()
        val currentChannel by guides.currentChannel.collectAsState()
        val reservations by guides.reservations.collectAsState()
        val canRecord by guides.canRecord.collectAsState()
        val current = guide
        if (current != null && current.channels.isNotEmpty()) {
            EpgGrid(
                current,
                loading = refreshing,
                currentChannelId = currentChannel,
                onSwitch = guides.switchChannel,
                reservations = reservations,
                canRecord = canRecord,
                onAction = guides.perform,
                loadVideoInfo = guides.videoInfo,
                loadImage = guides.programImage,
                onAudioTracks = guides.openAudioTracks,
                onSubtitles = guides.openSubtitles,
                modifier = modifier
            )
            return
        }
    }
    EpgPlaceholder(config, editing, guides.takeIf { !editing }, modifier)
}

// A guide touching the edge of the grid may use most of the page margin, which other elements keep free.
@Composable
fun Modifier.epgBleed(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean): Modifier {
    val bleed = dimensionResource(R.dimen.remote_page_padding) - dimensionResource(R.dimen.epg_edge_margin)
    return layout { measurable, constraints ->
        val amount = bleed.roundToPx()
        val start = if (left) amount else 0
        val above = if (top) amount else 0
        val width = constraints.maxWidth + start + (if (right) amount else 0)
        val height = constraints.maxHeight + above + (if (bottom) amount else 0)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-start, -above) }
    }
}

@Composable
private fun EpgPlaceholder(config: EpgConfig?, editing: Boolean, guides: EpgGuides?, modifier: Modifier) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.epg_panel_corner))
    val available = LocalEpgAvailable.current
    val colors = epgColors(currentPaletteColors())
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(if (available) 1f else UNAVAILABLE_ALPHA)
            .background(colors.panel, shape)
            .border(dimensionResource(R.dimen.epg_panel_border), colors.line, shape)
            .padding(dimensionResource(R.dimen.epg_panel_padding)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_epg),
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(dimensionResource(R.dimen.epg_placeholder_icon))
            )
            Text(
                text = config?.listName?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.epg_title_with_list, it) }
                    ?: stringResource(R.string.custom_element_epg),
                style = MaterialTheme.typography.titleSmall,
                color = colors.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (guides != null) EpgStatus(guides, colors)
            if (editing) {
                Text(
                    text = stringResource(if (available) R.string.epg_editor_hint else R.string.epg_unavailable_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EpgStatus(guides: EpgGuides, colors: EpgColors) {
    val refreshing by guides.refreshing.collectAsState()
    val failed by guides.failed.collectAsState()
    if (refreshing) {
        CircularProgressIndicator(color = colors.text, modifier = Modifier.size(dimensionResource(R.dimen.epg_placeholder_icon)))
        return
    }
    Text(
        text = stringResource(if (failed) R.string.epg_status_failed else R.string.epg_status_empty),
        style = MaterialTheme.typography.bodySmall,
        color = if (failed) colorResource(R.color.remote_power) else colors.muted,
        textAlign = TextAlign.Center
    )
}

@Composable
fun EpgListDialog(state: EpgPickerState, onPick: (FavoriteList) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.epg_list_title)) },
        text = {
            when {
                state.loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.failed -> Text(stringResource(R.string.epg_list_failed))
                state.lists.isEmpty() -> Text(stringResource(R.string.epg_list_empty))
                else -> LazyColumn {
                    items(state.lists) { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = list.id == state.current, role = Role.RadioButton, onClick = { onPick(list) })
                                .padding(vertical = dimensionResource(R.dimen.setup_item_spacing)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = list.id == state.current, onClick = null)
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = dimensionResource(R.dimen.setup_item_spacing))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

private const val UNAVAILABLE_ALPHA = 0.4f
