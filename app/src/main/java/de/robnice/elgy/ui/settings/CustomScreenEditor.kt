package de.robnice.elgy.ui.settings

import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import de.robnice.elgy.R
import de.robnice.elgy.data.model.CustomElement
import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.remote.CustomElementView
import de.robnice.elgy.ui.remote.CustomGrid
import de.robnice.elgy.ui.remote.CustomGridMetrics
import de.robnice.elgy.ui.remote.KeyCatalog
import de.robnice.elgy.ui.remote.RemotePageShape
import de.robnice.elgy.ui.remote.cell
import kotlin.math.roundToInt

// The hint says 2 s, but holding that long felt like 3 s in practice.
private const val APP_MENU_HOLD_MILLIS = 1_000L
private const val TRASH_HOVER_ALPHA = 0.2f
private const val DRAG_ALPHA = 0.7f
private const val MAJOR_LINE_STEP = 3
private const val MINOR_LINE_ALPHA = 0.35f
private const val TRASH_SCALE = 0.4f

@Composable
fun CustomScreenEditor(
    title: String,
    screen: CustomScreen,
    onAdd: (CustomElementType, RemoteKey?) -> Boolean,
    onMove: (Long, Int, Int) -> Unit,
    onResize: (Long, Int, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onConfigureApp: (Long) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf<Long?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val pageAspect = RemotePageShape.aspectRatio
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var overTrash by remember { mutableStateOf(false) }
    var noSpace by remember { mutableStateOf(false) }
    val padding = dimensionResource(R.dimen.setup_padding)

    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.custom_editor_done)) }
        }
        Text(
            text = stringResource(if (noSpace) R.string.custom_editor_no_space else R.string.custom_editor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = if (noSpace) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = padding)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(dimensionResource(R.dimen.remote_page_padding)),
            contentAlignment = Alignment.Center
        ) {
            CustomGrid(
                modifier = if (pageAspect != null) Modifier.aspectRatio(pageAspect) else Modifier.fillMaxSize()
            ) { metrics ->
                GridLines(onTap = { selected = null })
                screen.elements.forEach { element ->
                    key(element.id) {
                        EditableElement(
                            element = element,
                            metrics = metrics,
                            screen = screen,
                            selected = selected == element.id,
                            onSelect = { selected = element.id },
                            onLongPress = { if (element.type == CustomElementType.APP) onConfigureApp(element.id) },
                            onMove = onMove,
                            onResize = onResize,
                            isOverTrash = { position -> trashBounds?.contains(position) == true },
                            onDragState = { active, hovering ->
                                dragging = active
                                overTrash = hovering
                            },
                            onRemove = {
                                selected = null
                                onRemove(element.id)
                            }
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = padding, vertical = dimensionResource(R.dimen.setup_item_spacing)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showPicker = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.custom_editor_button_icon_size))
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.custom_editor_button_icon_spacing)))
                Text(stringResource(R.string.custom_editor_add_short))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { confirmReset = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_restore),
                    contentDescription = stringResource(R.string.settings_screen_reset, title)
                )
            }
            Trash(
                dragging = dragging,
                hovering = overTrash,
                modifier = Modifier.onGloballyPositioned { trashBounds = it.boundsInRoot() }
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_screen_reset_title)) },
            text = { Text(stringResource(R.string.settings_screen_reset_message, title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    selected = null
                    onReset()
                }) { Text(stringResource(R.string.settings_screen_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.dialog_cancel)) } }
        )
    }

    if (showPicker) {
        ElementPickerDialog(
            onPick = { type, key ->
                showPicker = false
                noSpace = !onAdd(type, key)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun Trash(dragging: Boolean, hovering: Boolean, modifier: Modifier) {
    val color = when {
        hovering -> colorResource(R.color.custom_editor_invalid)
        dragging -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(dimensionResource(R.dimen.custom_editor_trash_size))
            .background(if (hovering) color.copy(alpha = TRASH_HOVER_ALPHA) else Color.Transparent, CircleShape)
            .border(dimensionResource(R.dimen.custom_editor_outline), color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.custom_editor_remove),
            tint = color
        )
    }
}

@Composable
private fun GridLines(onTap: () -> Unit) {
    val color = colorResource(R.color.custom_editor_grid)
    val width = with(LocalDensity.current) { dimensionResource(R.dimen.custom_editor_grid_line).toPx() }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    ) {
        for (column in 0..CustomScreen.COLUMNS) {
            val x = size.width * column / CustomScreen.COLUMNS
            val lineColor = if (column % MAJOR_LINE_STEP == 0) color else color.copy(alpha = color.alpha * MINOR_LINE_ALPHA)
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), width)
        }
        for (row in 0..CustomScreen.ROWS) {
            val y = size.height * row / CustomScreen.ROWS
            val lineColor = if (row % MAJOR_LINE_STEP == 0) color else color.copy(alpha = color.alpha * MINOR_LINE_ALPHA)
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), width)
        }
    }
}

@Composable
private fun EditableElement(
    element: CustomElement,
    metrics: CustomGridMetrics,
    screen: CustomScreen,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onMove: (Long, Int, Int) -> Unit,
    onResize: (Long, Int, Int) -> Unit,
    isOverTrash: (Offset) -> Boolean,
    onDragState: (dragging: Boolean, overTrash: Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val density = LocalDensity.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var overTrash by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    var dropping by remember { mutableStateOf(false) }
    val cellWidth = with(density) { metrics.cellWidth.toPx() }
    val cellHeight = with(density) { metrics.cellHeight.toPx() }
    var drag by remember { mutableStateOf<Offset?>(null) }
    var resize by remember { mutableStateOf<Offset?>(null) }
    val shape = RoundedCornerShape(dimensionResource(R.dimen.custom_editor_corner))
    val outlineWidth = dimensionResource(R.dimen.custom_editor_outline)
    val haptics = LocalHapticFeedback.current

    fun moved(offset: Offset) = element.copy(
        x = element.x + (offset.x / cellWidth).roundToInt(),
        y = element.y + (offset.y / cellHeight).roundToInt()
    )

    fun resized(offset: Offset) = element.copy(
        width = element.width + (offset.x / cellWidth).roundToInt(),
        height = element.height + (offset.y / cellHeight).roundToInt()
    )

    // Saving is asynchronous: keep a dropped element at its new spot until the updated layout arrives.
    LaunchedEffect(element) {
        if (dropping) {
            drag = null
            dropping = false
        }
    }

    val target = if (overTrash || dropping) null else drag?.let(::moved) ?: resize?.let(::resized)
    if (target != null) {
        Box(
            modifier = Modifier
                .cell(target, metrics)
                .zIndex(2f)
                .border(
                    outlineWidth,
                    colorResource(if (screen.canPlace(target)) R.color.custom_editor_valid else R.color.custom_editor_invalid),
                    shape
                )
        )
    }

    Box(
        modifier = Modifier
            .cell(element, metrics)
            .zIndex(if (drag != null || selected) 1f else 0f)
            .graphicsLayer {
                translationX = drag?.x ?: 0f
                translationY = drag?.y ?: 0f
                alpha = when {
                    removed -> 0f
                    drag != null && !dropping -> DRAG_ALPHA
                    else -> 1f
                }
                scaleX = if (overTrash) TRASH_SCALE else 1f
                scaleY = if (overTrash) TRASH_SCALE else 1f
            }
    ) {
        CustomElementView(element = element, onKey = {}, onLaunchApp = {}, editing = true)
        Box(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { coordinates = it }
                .then(if (selected) Modifier.border(outlineWidth, colorResource(R.color.custom_editor_selection), shape) else Modifier)
                .pointerInput(element.id) { detectTapGestures { onSelect() } }
                .pointerInput(element.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val released = withTimeoutOrNull(APP_MENU_HOLD_MILLIS) {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                val moved = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                if (!change.pressed || change.isConsumed || moved) break
                            }
                        }
                        if (released == null) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    }
                }
                .pointerInput(element) {
                    detectDragGestures(
                        onDragStart = { position ->
                            onSelect()
                            dragOrigin = coordinates?.localToRoot(position) ?: Offset.Zero
                            drag = Offset.Zero
                            onDragState(true, false)
                        },
                        onDragEnd = {
                            val dropped = drag?.let(::moved)
                            when {
                                overTrash -> {
                                    removed = true
                                    onRemove()
                                }
                                dropped != null && dropped != element && screen.canPlace(dropped) -> {
                                    drag = Offset((dropped.x - element.x) * cellWidth, (dropped.y - element.y) * cellHeight)
                                    dropping = true
                                    onMove(element.id, dropped.x, dropped.y)
                                }
                                else -> drag = null
                            }
                            overTrash = false
                            onDragState(false, false)
                        },
                        onDragCancel = {
                            drag = null
                            overTrash = false
                            onDragState(false, false)
                        }
                    ) { change, _ ->
                        change.consume()
                        // This box moves with the drag, so work with screen positions instead of summed local deltas.
                        val finger = coordinates?.localToRoot(change.position) ?: return@detectDragGestures
                        drag = finger - dragOrigin
                        overTrash = isOverTrash(finger)
                        onDragState(true, overTrash)
                    }
                }
        )
        if (selected && drag == null) {
            Handle(
                icon = R.drawable.ic_resize,
                description = R.string.custom_editor_resize,
                color = colorResource(R.color.custom_editor_selection),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .pointerInput(element) {
                        detectDragGestures(
                            onDragStart = { resize = Offset.Zero },
                            onDragEnd = {
                                resize?.let { resized(it) }?.let { onResize(element.id, it.width, it.height) }
                                resize = null
                            },
                            onDragCancel = { resize = null }
                        ) { change, amount ->
                            change.consume()
                            resize = (resize ?: Offset.Zero) + amount
                        }
                    }
            )
        }
    }
}

@Composable
private fun Handle(icon: Int, description: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensionResource(R.dimen.custom_editor_handle_size))
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = colorResource(R.color.custom_editor_handle_icon),
            modifier = Modifier.size(dimensionResource(R.dimen.custom_editor_handle_icon_size))
        )
    }
}

private sealed interface PickerItem {
    data class Header(val title: Int) : PickerItem
    data class Element(val type: CustomElementType, val title: Int) : PickerItem
    data class Key(val key: RemoteKey) : PickerItem
}

@Composable
private fun ElementPickerDialog(
    onPick: (CustomElementType, RemoteKey?) -> Unit,
    onDismiss: () -> Unit
) {
    val items = buildList {
        add(PickerItem.Header(R.string.custom_picker_elements))
        add(PickerItem.Element(CustomElementType.VOLUME, R.string.custom_element_volume))
        add(PickerItem.Element(CustomElementType.CHANNEL, R.string.custom_element_channel))
        add(PickerItem.Element(CustomElementType.DPAD, R.string.custom_element_dpad))
        add(PickerItem.Element(CustomElementType.APP, R.string.custom_element_app))
        add(PickerItem.Header(R.string.custom_picker_keys))
        RemoteKey.entries.forEach { add(PickerItem.Key(it)) }
    }
    val itemPadding = dimensionResource(R.dimen.custom_picker_item_padding)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_editor_add)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = dimensionResource(R.dimen.custom_picker_max_height))) {
                items(items) { item ->
                    when (item) {
                        is PickerItem.Header -> Text(
                            text = stringResource(item.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = itemPadding, bottom = itemPadding / 2)
                        )
                        is PickerItem.Element -> PickerRow(stringResource(item.title), null) {
                            onPick(item.type, null)
                        }
                        is PickerItem.Key -> PickerRow(KeyCatalog.name(item.key), KeyCatalog.visual(item.key).icon) {
                            onPick(CustomElementType.KEY, item.key)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun PickerRow(title: String, icon: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.custom_picker_item_padding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.custom_picker_item_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(dimensionResource(R.dimen.custom_picker_icon_size)), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
