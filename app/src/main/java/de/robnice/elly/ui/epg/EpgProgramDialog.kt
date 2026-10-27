package de.robnice.elly.ui.epg

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import de.robnice.elly.epg.EpgVideoInfo
import androidx.compose.runtime.produceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    logoPath: String?,
    program: EpgProgram,
    reminder: EpgReservation?,
    recording: EpgReservation?,
    canRecord: Boolean,
    onAction: suspend (EpgAction) -> Unit,
    loadVideoInfo: (suspend (EpgChannel) -> EpgVideoInfo?)?,
    loadImage: suspend (String) -> ByteArray?,
    onAudioTracks: (() -> Unit)?,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember(program) { mutableStateOf(false) }
    var error by remember(program) { mutableStateOf<String?>(null) }
    val failedText = stringResource(R.string.epg_action_failed)
    val conflictText = stringResource(R.string.epg_action_conflict)
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
                error = if (e.reason == TV_CONFLICT) conflictText else e.reason ?: failedText
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
        title = {
            Row(verticalAlignment = Alignment.Top) {
                Text(program.title, modifier = Modifier.weight(1f))
                val inset = dimensionResource(R.dimen.epg_info_close_inset)
                IconButton(onClick = onDismiss, modifier = Modifier.offset(x = inset, y = -inset)) {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.epg_info_close))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = dimensionResource(R.dimen.epg_info_max_height))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                    ProgramImage(program.imageUrl, program.title, loadImage)
                    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                        ChannelLine(channel, logoPath)
                        Text(
                            text = stringResource(R.string.epg_info_when, day.format(start), time.format(start), time.format(end), minutes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        seasonEpisode(program)?.let {
                            Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (program.start <= now && program.end > now) {
                    LinearProgressIndicator(
                        progress = { (now - program.start).toFloat() / (program.end - program.start) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.epg_info_remaining, ((program.end - now) / MINUTE_MILLIS).toInt() + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val video by produceState<EpgVideoInfo?>(null, channel, program) { value = loadVideoInfo?.invoke(channel) }
                Badges(channel, program, video)
                if (program.end > now) {
                    if (program.start > now) {
                        ActionButton(
                            icon = if (reminder != null) R.drawable.ic_reminder_off else R.drawable.ic_reminder,
                            text = if (reminder != null) R.string.epg_remind_cancel else R.string.epg_remind,
                            enabled = !busy,
                            onClick = { act(if (reminder != null) EpgAction.Cancel(reminder) else EpgAction.Remind(channel, program)) }
                        )
                    }
                    val recordData = program.offlineProgramId != null || channel.channelCode != null
                    val recordable = canRecord && recordData
                    // A recording already running is ended and kept; a planned one is deleted.
                    val running = program.start <= now
                    ActionButton(
                        icon = when {
                            recording == null -> R.drawable.ic_record
                            running -> R.drawable.ic_stop
                            else -> R.drawable.ic_delete
                        },
                        text = when {
                            recording == null -> R.string.epg_record
                            running -> R.string.epg_record_stop
                            else -> R.string.epg_record_cancel
                        },
                        enabled = !busy && (recording != null || recordable),
                        onClick = {
                            act(
                                when {
                                    recording == null -> EpgAction.Record(channel, program)
                                    running -> EpgAction.StopRecording(channel, recording)
                                    else -> EpgAction.Cancel(recording)
                                }
                            )
                        }
                    )
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
                    // The TV only lists the tracks of what it shows right now.
                    if (running && onAudioTracks != null) {
                        ActionButton(icon = R.drawable.ic_audio_track, text = R.string.audio_tracks_title, enabled = true, onClick = onAudioTracks)
                    }
                }
                val genre = program.genres.joinToString(GENRE_SEPARATOR).ifBlank { program.genre }
                if (genre.isNotBlank()) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                texts.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSwitch) {
                ButtonIcon(R.drawable.ic_live_tv)
                Text(stringResource(R.string.epg_info_switch))
            }
        }
    )
}

@Composable
private fun ActionButton(@DrawableRes icon: Int, @StringRes text: Int, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        ButtonIcon(icon)
        Text(stringResource(text))
    }
}

@Composable
private fun ButtonIcon(@DrawableRes icon: Int) {
    Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
}

// While the picture loads, a plain square keeps its place so the text beside it does not jump.
@Composable
private fun ProgramImage(url: String?, title: String, loadImage: suspend (String) -> ByteArray?) {
    if (url == null) return
    val description = stringResource(R.string.epg_info_image_description, title)
    var loading by remember(url) { mutableStateOf(true) }
    var enlarged by remember(url) { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, url) {
        val bytes = loadImage(url)
        value = bytes?.let { withContext(Dispatchers.Default) { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() } }
        loading = false
    }
    if (image == null && !loading) return
    val shape = RoundedCornerShape(dimensionResource(R.dimen.epg_cell_corner))
    val modifier = Modifier
        .size(dimensionResource(R.dimen.epg_info_image_size))
        .clip(shape)
    image?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = modifier.clickable(onClickLabel = stringResource(R.string.epg_info_image_enlarge)) { enlarged = true }
        )
        if (enlarged) {
            Dialog(onDismissRequest = { enlarged = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Image(
                    bitmap = bitmap,
                    contentDescription = description,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.epg_info_image_dialog_padding))
                        .clip(shape)
                        .clickable(onClickLabel = stringResource(R.string.epg_info_close)) { enlarged = false }
                )
            }
        }
    } ?: Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun seasonEpisode(program: EpgProgram): String? {
    val season = program.season
    val episode = program.episode
    return when {
        season != null && episode != null -> stringResource(R.string.epg_info_season_episode, season, episode)
        season != null -> stringResource(R.string.epg_info_season, season)
        episode != null -> stringResource(R.string.epg_info_episode, episode)
        else -> null
    }
}

// Channel number, logo and name, as in the TV's info banner.
@Composable
private fun ChannelLine(channel: EpgChannel, logoPath: String?) {
    val logo by rememberLogo(logoPath)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
    ) {
        if (channel.number.isNotBlank()) {
            Text(text = channel.number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        logo?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(dimensionResource(R.dimen.epg_info_logo_height))
            )
        }
        Text(text = channel.name, style = MaterialTheme.typography.titleSmall)
    }
}

// Small marks like on the TV: picture quality, access services and where the channel is received.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Badges(channel: EpgChannel, program: EpgProgram, video: EpgVideoInfo?) {
    val badges = buildList {
        // Broadcasters send 3 for nearly everything, children's shows included, so only real age limits are shown.
        program.rating?.takeIf { it >= MIN_SHOWN_AGE }?.let { add(stringResource(R.string.epg_badge_age, it)) }
        if (channel.hd) add(stringResource(R.string.epg_badge_hd))
        video?.resolution?.let { add(it) }
        video?.aspectRatio?.let { add(it) }
        video?.hdr?.let { add(it) }
        if (program.subtitles) add(stringResource(R.string.epg_badge_subtitles))
        if (program.audioDescription) add(stringResource(R.string.epg_badge_audio_description))
        if (program.signLanguage) add(stringResource(R.string.epg_badge_sign_language))
        channel.satellite?.let { satellite ->
            add(channel.frequency?.let { stringResource(R.string.epg_badge_satellite, satellite, it) } ?: satellite)
        }
    }
    if (badges.isEmpty()) return
    val spacing = dimensionResource(R.dimen.epg_badge_spacing)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        badges.forEach { badge ->
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .border(
                        dimensionResource(R.dimen.epg_panel_border),
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(dimensionResource(R.dimen.epg_cell_corner))
                    )
                    .padding(horizontal = dimensionResource(R.dimen.epg_badge_padding))
            )
        }
    }
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
private const val GENRE_SEPARATOR = " · "
private const val MIN_SHOWN_AGE = 6

// The TV's text when a new reminder or recording overlaps one already planned.
private const val TV_CONFLICT = "Crash schedule"
private const val DAY_PATTERN = "EEEEdMMMM"
