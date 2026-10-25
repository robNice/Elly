package de.robnice.elly.ui.epg

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import de.robnice.elly.R
import de.robnice.elly.ui.components.PaletteColors
import de.robnice.elly.ui.components.rememberKeyHaptic
import de.robnice.elly.ui.components.currentPaletteColors
import de.robnice.elly.epg.EpgChannel
import de.robnice.elly.epg.EpgGuide
import de.robnice.elly.epg.EpgAction
import de.robnice.elly.epg.EpgProgram
import de.robnice.elly.epg.EpgReservation
import de.robnice.elly.epg.EpgVideoInfo
import de.robnice.elly.epg.ReservationType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Program guide like the TV's: times along the top, channels down the left, programs scroll in both directions.
// Only the programs in view are composed; a week of a few dozen channels has thousands.
@Composable
fun EpgGrid(
    guide: EpgGuide,
    loading: Boolean,
    currentChannelId: String?,
    onSwitch: (String) -> Unit,
    reservations: List<EpgReservation>,
    canRecord: Boolean,
    onAction: suspend (EpgAction) -> Unit,
    loadVideoInfo: suspend (EpgChannel) -> EpgVideoInfo?,
    modifier: Modifier = Modifier
) {
    val colors = epgColors(currentPaletteColors())
    val haptic = rememberKeyHaptic()
    val planned = remember(reservations) { reservations.groupBy { it.signalId } }
    fun marksOf(program: EpgProgram) = planned[program.signalId].orEmpty().filter { it.belongsTo(program) }
    var selected by remember { mutableStateOf<Pair<EpgChannel, EpgProgram>?>(null) }
    selected?.let { (channel, program) ->
        val marks = marksOf(program)
        EpgProgramDialog(
            channel = channel,
            logoPath = guide.logos[channel.id],
            program = program,
            reminder = marks.firstOrNull { it.type == ReservationType.REMINDER },
            recording = marks.firstOrNull { it.type == ReservationType.RECORDING },
            canRecord = canRecord,
            onAction = onAction,
            // Picture details come from the received signal, so only running programs ask; the TV then checks the channel.
            loadVideoInfo = loadVideoInfo.takeIf { program.start <= System.currentTimeMillis() && program.end > System.currentTimeMillis() },
            onSwitch = {
                selected = null
                haptic()
                onSwitch(channel.id)
            },
            onDismiss = { selected = null }
        )
    }
    val activeRow = colorResource(R.color.epg_now_line).copy(alpha = ACTIVE_ROW_ALPHA)
    val now by rememberNow()
    val zone = remember { ZoneId.systemDefault() }
    val density = LocalDensity.current
    val minuteWidth = dimensionResource(R.dimen.epg_minute_width)
    val minutePx = with(density) { minuteWidth.toPx() }

    // The grid starts at the earliest program, at most 24 hours back, on a half hour.
    val origin = remember(guide.channels) {
        val earliest = guide.programs.values.mapNotNull { list -> list.firstOrNull()?.start }.minOrNull() ?: now
        floorToHalfHour(maxOf(earliest, now - EPG_PAST_MILLIS))
    }
    val end = remember(guide) {
        val latest = guide.programs.values.mapNotNull { list -> list.lastOrNull()?.end }.maxOrNull() ?: now
        maxOf(latest, now + EPG_MIN_AHEAD_MILLIS)
    }
    val contentWidth = with(density) { ((end - origin) / MINUTE_MILLIS * minutePx).toDp() }
    fun xOf(time: Long): Float = (time - origin) / MINUTE_MILLIS.toFloat() * minutePx

    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel, RoundedCornerShape(dimensionResource(R.dimen.epg_panel_corner)))
            .clipToBounds()
            .nestedScroll(KeepHorizontalScroll)
    ) {
        val channelWidth = dimensionResource(R.dimen.epg_channel_width)
        val headerHeight = dimensionResource(R.dimen.epg_header_height)
        val rowHeight = ((maxHeight - headerHeight) / VISIBLE_ROWS)
            .coerceIn(dimensionResource(R.dimen.epg_row_min_height), dimensionResource(R.dimen.epg_row_max_height))
            // Whole pixels, so the channel column and the programs stay in line row after row.
            .let { with(density) { it.roundToPx().toDp() } }
        val viewportPx = with(density) { (maxWidth - channelWidth).toPx() }

        // Opens with now in the left third.
        var positioned by remember { mutableStateOf(false) }
        LaunchedEffect(horizontal.maxValue) {
            if (!positioned && horizontal.maxValue > 0) {
                horizontal.scrollTo((xOf(now) - viewportPx / NOW_POSITION).toInt().coerceIn(0, horizontal.maxValue))
                positioned = true
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.height(headerHeight)) {
                NowCell(
                    loading = loading,
                    colors = colors,
                    onNow = { scope.launch { horizontal.animateScrollTo((xOf(now) - viewportPx / NOW_POSITION).toInt().coerceAtLeast(0)) } },
                    modifier = Modifier.width(channelWidth).fillMaxHeight()
                )
                TimeRuler(origin, end, minuteWidth, now, horizontal, zone, colors, Modifier.weight(1f).fillMaxHeight())
            }
            Row(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .width(channelWidth)
                        .fillMaxHeight()
                        .verticalScroll(vertical)
                ) {
                    guide.channels.forEach { channel ->
                        val rowModifier = Modifier.height(rowHeight).fillMaxWidth()
                        ChannelCell(
                            channel,
                            guide.logos[channel.id],
                            colors,
                            (if (channel.id == currentChannelId) rowModifier.background(activeRow) else rowModifier)
                                .clickable {
                                    haptic()
                                    onSwitch(channel.id)
                                }
                        )
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val viewportHeightPx = with(density) { maxHeight.toPx() }
                    val rowPx = with(density) { rowHeight.toPx() }
                    // Only a change of the visible rows recomposes the grid, not every scrolled pixel.
                    val firstRow by remember(rowPx) { derivedStateOf { (vertical.value / rowPx).toInt().coerceAtLeast(0) } }
                    val lastRow by remember(rowPx, viewportHeightPx, guide.channels.size) {
                        derivedStateOf { ((vertical.value + viewportHeightPx) / rowPx).toInt().coerceAtMost(guide.channels.lastIndex) }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vertical)
                            .horizontalScroll(horizontal)
                    ) {
                        Box(modifier = Modifier.size(contentWidth, rowHeight * guide.channels.size)) {
                            val left = horizontal.value.toFloat()
                            val right = left + viewportPx
                            val activeIndex = guide.channels.indexOfFirst { it.id == currentChannelId }
                            if (activeIndex >= 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(y = rowHeight * activeIndex)
                                        .size(contentWidth, rowHeight)
                                        .background(activeRow)
                                )
                            }
                            for (row in firstRow..lastRow) {
                                val rowChannel = guide.channels[row]
                                val programs = guide.programs[rowChannel.signalId].orEmpty()
                                programs.forEach { program ->
                                    val x0 = xOf(program.start)
                                    val x1 = xOf(program.end)
                                    if (x1 > left && x0 < right) {
                                        ProgramCell(
                                            program = program,
                                            now = now,
                                            zone = zone,
                                            colors = colors,
                                            marks = marksOf(program).map { it.type },
                                            hiddenStart = with(density) { (left - x0).coerceAtLeast(0f).toDp() },
                                            modifier = Modifier
                                                .offset(with(density) { x0.toDp() }, rowHeight * row)
                                                .size(with(density) { (x1 - x0).toDp() }, rowHeight)
                                                .clickable { selected = rowChannel to program }
                                        )
                                    }
                                }
                            }
                            val dayLine = colors.text.copy(alpha = DAY_LINE_ALPHA)
                            midnights(origin + (left / minutePx).toLong() * MINUTE_MILLIS, origin + (right / minutePx).toLong() * MINUTE_MILLIS, zone)
                                .forEach { midnight ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = with(density) { xOf(midnight).toDp() })
                                            .width(DAY_LINE_WIDTH.dp)
                                            .fillMaxHeight()
                                            .background(dayLine)
                                    )
                                }
                            NowLine(with(density) { xOf(now).toDp() }, Modifier.fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowCell(loading: Boolean, colors: EpgColors, onNow: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clickable(onClick = onNow)
            .padding(horizontal = dimensionResource(R.dimen.epg_cell_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.epg_now),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.epg_now_line),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (loading) {
            CircularProgressIndicator(
                color = colors.text,
                strokeWidth = dimensionResource(R.dimen.epg_loading_stroke),
                modifier = Modifier.size(dimensionResource(R.dimen.epg_loading_size))
            )
        }
    }
}

@Composable
private fun TimeRuler(origin: Long, end: Long, minuteWidth: Dp, now: Long, scroll: ScrollState, zone: ZoneId, colors: EpgColors, modifier: Modifier) {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val density = LocalDensity.current
    val slotWidth = minuteWidth * SLOT_MINUTES
    val slotPx = with(density) { slotWidth.toPx() }
    val minutePx = with(density) { minuteWidth.toPx() }
    val tickColor = colors.line
    val nowColor = colorResource(R.color.epg_now_line)
    val viewStart = origin + (scroll.value / minutePx).toLong() * MINUTE_MILLIS
    val viewEnd = viewStart + (scroll.viewportSize / minutePx).toLong() * MINUTE_MILLIS
    var dateWidth by remember { mutableIntStateOf(0) }
    // The next day's date pushes the fixed date out instead of sliding under it.
    val nextDayX = midnights(viewStart, viewEnd, zone).firstOrNull()?.let { (it - origin) / MINUTE_MILLIS * minutePx - scroll.value }
    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(scroll, enabled = false)) {
            Box(modifier = Modifier.width(minuteWidth * ((end - origin) / MINUTE_MILLIS).toInt()).fillMaxHeight()) {
                val first = (scroll.value / slotPx).toInt()
                val count = (scroll.viewportSize / slotPx).toInt() + 2
                for (slot in first until first + count) {
                    val time = origin + slot * SLOT_MINUTES * MINUTE_MILLIS
                    if (time >= end) break
                    // Times starting under the fixed date would peek out behind it.
                    if (slot * slotPx - scroll.value < dateWidth) continue
                    Text(
                        text = formatter.format(Instant.ofEpochMilli(time).atZone(zone)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        modifier = Modifier
                            .offset(x = slotWidth * slot + dimensionResource(R.dimen.epg_cell_padding))
                            .align(Alignment.CenterStart)
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (slot in first until first + count) {
                        val x = slot * slotPx
                        drawLine(tickColor, Offset(x, size.height * TICK_START), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    }
                    val nowX = (now - origin) / MINUTE_MILLIS.toFloat() * minutePx
                    drawLine(nowColor, Offset(nowX, 0f), Offset(nowX, size.height), strokeWidth = NOW_LINE_WIDTH.dp.toPx())
                }
                // A new day starts with a line and its date.
                midnights(viewStart, viewEnd, zone).forEach { midnight ->
                    DayMark(
                        midnight,
                        zone,
                        colors,
                        Modifier.offset(x = minuteWidth * ((midnight - origin) / MINUTE_MILLIS).toInt()).fillMaxHeight()
                    )
                }
            }
        }
        // The date of the left edge stays in place while the times scroll by.
        Text(
            text = shortDate(Instant.ofEpochMilli(viewStart).atZone(zone).toLocalDate()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(nextDayX?.let { minOf(0f, it - dateWidth).toInt() } ?: 0, 0) }
                .onSizeChanged { dateWidth = it.width }
                .background(colors.panel)
                .padding(horizontal = dimensionResource(R.dimen.epg_cell_padding))
        )
    }
}

@Composable
private fun DayMark(midnight: Long, zone: ZoneId, colors: EpgColors, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(DAY_LINE_WIDTH.dp)
                .fillMaxHeight()
                .background(colors.text)
        )
        Text(
            text = shortDate(Instant.ofEpochMilli(midnight).atZone(zone).toLocalDate()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            maxLines = 1,
            modifier = Modifier
                .background(colors.panel)
                .padding(horizontal = dimensionResource(R.dimen.epg_cell_padding))
        )
    }
}

private fun midnights(from: Long, to: Long, zone: ZoneId): List<Long> {
    var day = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().plusDays(1)
    val result = mutableListOf<Long>()
    while (true) {
        val midnight = day.atStartOfDay(zone).toInstant().toEpochMilli()
        if (midnight > to) return result
        result += midnight
        day = day.plusDays(1)
    }
}

@Composable
private fun ChannelCell(channel: EpgChannel, logoPath: String?, colors: EpgColors, modifier: Modifier) {
    val logo by rememberLogo(logoPath)
    Box(
        modifier = modifier
            .padding(dimensionResource(R.dimen.epg_cell_gap))
            .background(colors.channel, RoundedCornerShape(dimensionResource(R.dimen.epg_cell_corner)))
            .padding(dimensionResource(R.dimen.epg_logo_padding)),
        contentAlignment = Alignment.Center
    ) {
        val image = logo
        if (image != null) {
            Image(bitmap = image, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgramCell(
    program: EpgProgram,
    now: Long,
    zone: ZoneId,
    colors: EpgColors,
    marks: List<ReservationType>,
    hiddenStart: Dp,
    modifier: Modifier
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val color = when {
        program.end <= now -> colors.past
        program.start <= now -> colors.running
        else -> colors.program
    }
    Box(
        modifier = modifier
            .padding(dimensionResource(R.dimen.epg_cell_gap))
            .background(color, RoundedCornerShape(dimensionResource(R.dimen.epg_cell_corner)))
            .clipToBounds()
            .padding(start = hiddenStart)
            .padding(horizontal = dimensionResource(R.dimen.epg_cell_padding)),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = program.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (program.end <= now) colors.muted else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ReservationType.RECORDING in marks) {
                    Box(
                        modifier = Modifier
                            .padding(end = dimensionResource(R.dimen.epg_mark_spacing))
                            .size(dimensionResource(R.dimen.epg_record_dot))
                            .background(colorResource(R.color.key_color_red), CircleShape)
                    )
                }
                if (ReservationType.REMINDER in marks) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reminder),
                        contentDescription = stringResource(R.string.epg_reminder_set),
                        tint = colors.reminder,
                        modifier = Modifier
                            .padding(end = dimensionResource(R.dimen.epg_mark_spacing))
                            .size(dimensionResource(R.dimen.epg_mark_icon))
                    )
                }
                Text(
                    text = formatter.format(Instant.ofEpochMilli(program.start).atZone(zone)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    maxLines = 1
                )
            }
        }
    }
}

// The guide takes its colors from the key palette, so it matches the keys around it.
class EpgColors(
    val panel: Color,
    val channel: Color,
    val program: Color,
    val past: Color,
    val running: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val reminder: Color
)

// Light palettes have dark text on light keys; there a deeper yellow keeps the reminder bell visible.
@Composable
fun epgColors(palette: PaletteColors) = EpgColors(
    panel = palette.keyBottom.copy(alpha = PANEL_ALPHA),
    channel = palette.keyTop.copy(alpha = CHANNEL_ALPHA),
    program = palette.keyTop.copy(alpha = PROGRAM_ALPHA),
    past = palette.keyTop.copy(alpha = PAST_ALPHA),
    running = palette.ringTop,
    line = palette.content.copy(alpha = LINE_ALPHA),
    text = palette.content,
    muted = palette.content.copy(alpha = MUTED_ALPHA),
    reminder = colorResource(if (palette.content.luminance() < LIGHT_PALETTE_TEXT) R.color.epg_reminder_on_light else R.color.epg_reminder)
)

@Composable
private fun NowLine(x: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .offset(x = x - (NOW_LINE_WIDTH / 2).dp)
            .width(NOW_LINE_WIDTH.dp)
            .background(colorResource(R.color.epg_now_line))
    )
}

@Composable
private fun rememberNow() = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(MINUTE_MILLIS - System.currentTimeMillis() % MINUTE_MILLIS)
        value = System.currentTimeMillis()
    }
}

@Composable
internal fun rememberLogo(path: String?) = produceState<ImageBitmap?>(null, path) {
    value = path?.let { withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
}

private fun shortDate(day: LocalDate): String {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DAY_PATTERN), locale).format(day)
}

private fun floorToHalfHour(time: Long): Long = time - Math.floorMod(time, SLOT_MINUTES * MINUTE_MILLIS)

// Leftover horizontal scrolling is swallowed, so dragging the guide never turns the screen page.
private object KeepHorizontalScroll : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
}

private const val MINUTE_MILLIS = 60_000L
private const val SLOT_MINUTES = 30
private const val EPG_PAST_MILLIS = 24 * 60 * MINUTE_MILLIS
private const val EPG_MIN_AHEAD_MILLIS = 3 * 60 * MINUTE_MILLIS
private const val VISIBLE_ROWS = 6
private const val NOW_POSITION = 3f
private const val NOW_LINE_WIDTH = 2
private const val TICK_START = 0.7f
private const val DAY_PATTERN = "EEEdM"
private const val DAY_LINE_WIDTH = 1
private const val DAY_LINE_ALPHA = 0.5f
private const val PANEL_ALPHA = 0.94f
private const val CHANNEL_ALPHA = 0.45f
private const val PROGRAM_ALPHA = 0.75f
private const val PAST_ALPHA = 0.3f
private const val LINE_ALPHA = 0.3f
private const val MUTED_ALPHA = 0.6f
private const val ACTIVE_ROW_ALPHA = 0.28f
private const val LIGHT_PALETTE_TEXT = 0.5f
