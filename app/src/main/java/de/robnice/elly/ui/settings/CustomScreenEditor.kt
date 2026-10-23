package de.robnice.elly.ui.settings

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import de.robnice.elly.R
import de.robnice.elly.data.model.ButtonGroup
import de.robnice.elly.data.model.CellShape
import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.ui.components.KeyStyle
import de.robnice.elly.ui.components.LightOverlay
import de.robnice.elly.ui.components.RockerKey
import de.robnice.elly.ui.components.ShadowLayer
import de.robnice.elly.ui.remote.CustomElementView
import de.robnice.elly.ui.remote.CustomGrid
import de.robnice.elly.ui.remote.CustomGridMetrics
import de.robnice.elly.ui.remote.KeyCatalog
import de.robnice.elly.ui.remote.LocalAppBackground
import de.robnice.elly.ui.remote.RemotePageShape
import de.robnice.elly.ui.remote.ScreenBackgroundImage
import de.robnice.elly.ui.remote.cell
import de.robnice.elly.ui.remote.layer
import de.robnice.elly.ui.remote.rememberCellShape
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// The hint says 2 s, but holding that long felt like 3 s in practice.
private const val APP_MENU_HOLD_MILLIS = 1_000L
private const val TRASH_HOVER_ALPHA = 0.2f
private const val DRAG_ALPHA = 0.7f
private const val MAJOR_LINE_STEP = 3
private const val MINOR_LINE_ALPHA = 0.35f
private const val TRASH_SCALE = 0.4f
private const val MARQUEE_FILL_ALPHA = 0.15f
private const val GROUP_CELL_ASPECT = 1.1f
private const val GROUP_ELEMENT_INSET = 0.25f

@Composable
fun CustomScreenEditor(
    screen: CustomScreen,
    onAdd: (CustomElementType, RemoteKey?) -> Boolean,
    onAddGroup: (ButtonGroup) -> Boolean,
    pickerTab: Int,
    onPickerTab: (Int) -> Unit,
    onMove: (Long, Int, Int, CellShape) -> Unit,
    onResize: (Long, Int, Int, CellShape) -> Unit,
    onRemove: (Long) -> Unit,
    onMoveAll: (Set<Long>, Int, Int, CellShape) -> Unit,
    onRemoveAll: (Set<Long>) -> Unit,
    onConfigureApp: (Long) -> Unit,
    onReset: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    val groupDrag = remember { GroupDrag() }
    var marquee by remember { mutableStateOf<Rect?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val background = LocalAppBackground.current
    val pageAspect = RemotePageShape.aspectRatio
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var overTrash by remember { mutableStateOf(false) }
    var noSpace by remember { mutableStateOf(false) }
    val padding = dimensionResource(R.dimen.setup_padding)

    // Saving is asynchronous: keep dropped elements at their new spot until the updated layout arrives.
    LaunchedEffect(screen) {
        if (groupDrag.dropping) {
            groupDrag.offset = null
            groupDrag.dropping = false
        }
    }

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
            Spacer(modifier = Modifier.weight(1f))
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
                val density = LocalDensity.current
                val cellWidth = with(density) { metrics.cellWidth.toPx() }
                val cellHeight = with(density) { metrics.cellHeight.toPx() }
                val cells = rememberCellShape(metrics)
                val multiple = selected.size > 1
                val members = screen.elements.filter { it.id in selected }
                val groupDelta = groupDrag.offset?.takeIf { multiple && members.isNotEmpty() }?.let { offset ->
                    val dx = (offset.x / cellWidth).roundToInt()
                        .coerceIn(-members.minOf { it.x }, CustomScreen.COLUMNS - members.maxOf { it.x + it.width })
                    val dy = (offset.y / cellHeight).roundToInt()
                        .coerceIn(-members.minOf { it.y }, CustomScreen.ROWS - members.maxOf { it.y + it.height })
                    dx to dy
                }
                val groupValid = groupDelta?.let { (dx, dy) -> screen.moveAll(selected, dx, dy, cells) != null } == true
                ScreenBackgroundImage(background.type, background.image)
                LightOverlay()
                ShadowLayer(screen.elements, cells, metrics.cellHeight)
                GridLines(
                    onTap = { selected = emptySet() },
                    onMarquee = { marquee = it },
                    onMarqueeEnd = { area ->
                        marquee = null
                        selected = screen.elements.filter { element ->
                            area.contains(
                                Offset((element.x + element.width / 2f) * cellWidth, (element.y + element.height / 2f) * cellHeight)
                            )
                        }.map { it.id }.toSet()
                    }
                )
                marquee?.let { Marquee(it) }
                screen.elements.forEach { element ->
                    key(element.id) {
                        EditableElement(
                            element = element,
                            metrics = metrics,
                            screen = screen,
                            selected = element.id in selected,
                            showHandle = !multiple,
                            group = groupDrag.takeIf { multiple && element.id in selected },
                            groupDelta = groupDelta,
                            groupValid = groupValid,
                            onGroupDrop = {
                                val delta = groupDelta
                                when {
                                    groupDrag.overTrash -> {
                                        groupDrag.offset = null
                                        onRemoveAll(selected)
                                        selected = emptySet()
                                    }
                                    delta != null && groupValid && delta != (0 to 0) -> {
                                        groupDrag.offset = Offset(delta.first * cellWidth, delta.second * cellHeight)
                                        groupDrag.dropping = true
                                        onMoveAll(selected, delta.first, delta.second, cells)
                                    }
                                    else -> groupDrag.offset = null
                                }
                                groupDrag.overTrash = false
                            },
                            onSelect = { if (element.id !in selected || !multiple) selected = setOf(element.id) },
                            onLongPress = { if (element.type == CustomElementType.APP) onConfigureApp(element.id) },
                            onMove = onMove,
                            onResize = onResize,
                            isOverTrash = { position -> trashBounds?.contains(position) == true },
                            onDragState = { active, hovering ->
                                dragging = active
                                overTrash = hovering
                            },
                            onRemove = {
                                selected = emptySet()
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
            IconButton(onClick = {
                selected = emptySet()
                onUndo()
            }, enabled = canUndo) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.custom_editor_undo)
                )
            }
            IconButton(onClick = { confirmReset = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_restore),
                    contentDescription = stringResource(R.string.custom_editor_reset)
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
            text = { Text(stringResource(R.string.settings_screen_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    selected = emptySet()
                    onReset()
                }) { Text(stringResource(R.string.settings_screen_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.dialog_cancel)) } }
        )
    }

    if (showPicker) {
        ElementPickerDialog(
            initialTab = pickerTab,
            onTabChange = onPickerTab,
            onPick = { type, key ->
                showPicker = false
                noSpace = !onAdd(type, key)
            },
            onPickGroup = { group ->
                showPicker = false
                noSpace = !onAddGroup(group)
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
private fun GridLines(onTap: () -> Unit, onMarquee: (Rect) -> Unit, onMarqueeEnd: (Rect) -> Unit) {
    val color = colorResource(R.color.custom_editor_grid)
    val width = with(LocalDensity.current) { dimensionResource(R.dimen.custom_editor_grid_line).toPx() }
    var start by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(Rect.Zero) }
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMarquee by rememberUpdatedState(onMarquee)
    val currentOnMarqueeEnd by rememberUpdatedState(onMarqueeEnd)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { currentOnTap() } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        start = it
                        area = Rect(it, it)
                        currentOnMarquee(area)
                    },
                    onDragEnd = { currentOnMarqueeEnd(area) },
                    onDragCancel = { currentOnMarqueeEnd(area) }
                ) { change, _ ->
                    change.consume()
                    area = Rect(
                        Offset(minOf(start.x, change.position.x), minOf(start.y, change.position.y)),
                        Offset(maxOf(start.x, change.position.x), maxOf(start.y, change.position.y))
                    )
                    currentOnMarquee(area)
                }
            }
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
private fun Marquee(area: Rect) {
    val color = colorResource(R.color.custom_editor_selection)
    val stroke = with(LocalDensity.current) { dimensionResource(R.dimen.custom_editor_outline).toPx() }
    Canvas(modifier = Modifier.fillMaxSize().zIndex(3f)) {
        drawRect(color.copy(alpha = MARQUEE_FILL_ALPHA), area.topLeft, area.size)
        drawRect(color, area.topLeft, area.size, style = Stroke(stroke))
    }
}

// Shared state while several selected elements are dragged together.
private class GroupDrag {
    var offset by mutableStateOf<Offset?>(null)
    var overTrash by mutableStateOf(false)
    var dropping by mutableStateOf(false)
}

@Composable
private fun EditableElement(
    element: CustomElement,
    metrics: CustomGridMetrics,
    screen: CustomScreen,
    selected: Boolean,
    showHandle: Boolean,
    group: GroupDrag?,
    groupDelta: Pair<Int, Int>?,
    groupValid: Boolean,
    onGroupDrop: () -> Unit,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onMove: (Long, Int, Int, CellShape) -> Unit,
    onResize: (Long, Int, Int, CellShape) -> Unit,
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

    val cells = rememberCellShape(metrics)
    // The gesture handlers outlive recompositions, so they read the latest selection through these.
    val currentGroup by rememberUpdatedState(group)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnGroupDrop by rememberUpdatedState(onGroupDrop)

    // The target stops at the edge, so an element can be slid along it without landing outside.
    fun moved(offset: Offset) = element.copy(
        x = (element.x + (offset.x / cellWidth).roundToInt()).coerceIn(0, CustomScreen.COLUMNS - element.width),
        y = (element.y + (offset.y / cellHeight).roundToInt()).coerceIn(0, CustomScreen.ROWS - element.height)
    )

    fun resized(offset: Offset) = element.copy(
        width = (element.width + (offset.x / cellWidth).roundToInt()).coerceIn(element.minWidth, CustomScreen.COLUMNS - element.x),
        height = (element.height + (offset.y / cellHeight).roundToInt()).coerceIn(element.minHeight, CustomScreen.ROWS - element.y)
    )

    // Saving is asynchronous: keep a dropped element at its new spot until the updated layout arrives.
    LaunchedEffect(element) {
        if (dropping) {
            drag = null
            dropping = false
        }
    }

    val shownDrag = if (group != null) group.offset else drag
    val trashed = if (group != null) group.overTrash else overTrash
    val target = when {
        group != null -> groupDelta?.takeIf { !group.overTrash && !group.dropping }
            ?.let { (dx, dy) -> element.copy(x = element.x + dx, y = element.y + dy) }
        overTrash || dropping -> null
        else -> drag?.let(::moved) ?: resize?.let(::resized)
    }
    if (target != null) {
        val valid = if (group != null) groupValid else screen.canPlace(target, cells)
        Box(
            modifier = Modifier
                .cell(target, metrics)
                .zIndex(2f)
                .border(
                    outlineWidth,
                    colorResource(if (valid) R.color.custom_editor_valid else R.color.custom_editor_invalid),
                    shape
                )
        )
    }

    Box(
        modifier = Modifier
            .cell(element, metrics)
            .zIndex(if (shownDrag != null || selected) 1f else element.layer)
            .graphicsLayer {
                translationX = shownDrag?.x ?: 0f
                translationY = shownDrag?.y ?: 0f
                alpha = when {
                    removed -> 0f
                    shownDrag != null && !(dropping || group?.dropping == true) -> DRAG_ALPHA
                    else -> 1f
                }
                scaleX = if (trashed) TRASH_SCALE else 1f
                scaleY = if (trashed) TRASH_SCALE else 1f
            }
    ) {
        CustomElementView(element = element, onKey = {}, onLaunchApp = {}, editing = true)
        Box(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { coordinates = it }
                .then(if (selected) Modifier.border(outlineWidth, colorResource(R.color.custom_editor_selection), shape) else Modifier)
                .pointerInput(element.id) { detectTapGestures { currentOnSelect() } }
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
                            currentOnSelect()
                            dragOrigin = coordinates?.localToRoot(position) ?: Offset.Zero
                            currentGroup.let { if (it != null) it.offset = Offset.Zero else drag = Offset.Zero }
                            onDragState(true, false)
                        },
                        onDragEnd = {
                            if (currentGroup != null) {
                                currentOnGroupDrop()
                                onDragState(false, false)
                                return@detectDragGestures
                            }
                            val dropped = drag?.let(::moved)
                            when {
                                overTrash -> {
                                    removed = true
                                    onRemove()
                                }
                                dropped != null && dropped != element && screen.canPlace(dropped, cells) -> {
                                    drag = Offset((dropped.x - element.x) * cellWidth, (dropped.y - element.y) * cellHeight)
                                    dropping = true
                                    onMove(element.id, dropped.x, dropped.y, cells)
                                }
                                else -> drag = null
                            }
                            overTrash = false
                            onDragState(false, false)
                        },
                        onDragCancel = {
                            drag = null
                            overTrash = false
                            currentGroup?.offset = null
                            currentGroup?.overTrash = false
                            onDragState(false, false)
                        }
                    ) { change, _ ->
                        change.consume()
                        // This box moves with the drag, so work with screen positions instead of summed local deltas.
                        val finger = coordinates?.localToRoot(change.position) ?: return@detectDragGestures
                        val hovering = isOverTrash(finger)
                        val active = currentGroup
                        if (active != null) {
                            active.offset = finger - dragOrigin
                            active.overTrash = hovering
                        } else {
                            drag = finger - dragOrigin
                            overTrash = hovering
                        }
                        onDragState(true, hovering)
                    }
                }
        )
        if (selected && showHandle && drag == null) {
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
                                resize?.let { resized(it) }?.let { onResize(element.id, it.width, it.height, cells) }
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
    initialTab: Int,
    onTabChange: (Int) -> Unit,
    onPick: (CustomElementType, RemoteKey?) -> Unit,
    onPickGroup: (ButtonGroup) -> Unit,
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

    val pagerState = rememberPagerState(initialPage = initialTab) { PICKER_TABS.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onTabChange)
    }
    val dialogPadding = dimensionResource(R.dimen.dialog_content_padding)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_editor_add)) },
        text = {
            Column {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    // The dialog pads its content; the tab bar reaches out to both edges.
                    modifier = Modifier.layout { measurable, constraints ->
                        val extra = dialogPadding.roundToPx() * 2
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra)
                        )
                        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                    }
                ) {
                    PICKER_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(title)) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(dimensionResource(R.dimen.custom_picker_max_height)),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    if (page == 0) {
                        SinglePicker(items, itemPadding, onPick)
                    } else {
                        GroupPicker(onPickGroup)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun SinglePicker(items: List<PickerItem>, itemPadding: Dp, onPick: (CustomElementType, RemoteKey?) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { item ->
            when (item) {
                is PickerItem.Header -> Text(
                    text = stringResource(item.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = itemPadding, bottom = itemPadding / 2)
                )
                is PickerItem.Element -> PickerRow(
                    title = stringResource(item.title),
                    leading = { ElementLeading(item.type) { onPick(item.type, null) } },
                    onClick = { onPick(item.type, null) }
                )
                is PickerItem.Key -> PickerRow(
                    title = KeyCatalog.name(item.key),
                    leading = { KeyLeading(item.key) },
                    onClick = { onPick(CustomElementType.KEY, item.key) }
                )
            }
        }
    }
}

@Composable
private fun GroupPicker(onPick: (ButtonGroup) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(ButtonGroup.entries) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(group) }
                    .padding(vertical = dimensionResource(R.dimen.custom_picker_item_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.custom_picker_item_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupPreview(
                    group = group,
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.custom_group_preview_width),
                        dimensionResource(R.dimen.custom_group_preview_height)
                    )
                )
                Text(text = stringResource(group.title()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// The group's layout scaled into the preview box, each element as a rounded block.
@Composable
private fun GroupPreview(group: ButtonGroup, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val cell = minOf(size.width / (group.width * GROUP_CELL_ASPECT), size.height / group.height)
        val cellWidth = cell * GROUP_CELL_ASPECT
        val left = (size.width - group.width * cellWidth) / 2
        val top = (size.height - group.height * cell) / 2
        val inset = cell * GROUP_ELEMENT_INSET
        group.elements.forEach { item ->
            val width = item.width * cellWidth - 2 * inset
            val height = item.height * cell - 2 * inset
            drawRoundRect(
                color = color,
                topLeft = Offset(left + item.x * cellWidth + inset, top + item.y * cell + inset),
                size = Size(width, height),
                cornerRadius = CornerRadius(minOf(width, height) / 2)
            )
        }
    }
}

private fun ButtonGroup.title(): Int = when (this) {
    ButtonGroup.NUMBERS -> R.string.group_numbers
    ButtonGroup.APPS -> R.string.group_apps
    ButtonGroup.PLAYBACK -> R.string.group_playback
    ButtonGroup.COLORS -> R.string.group_colors
    ButtonGroup.VOLUME_CHANNEL -> R.string.group_volume_channel
    ButtonGroup.NAVIGATION -> R.string.group_navigation
    ButtonGroup.POWER -> R.string.group_power
}

private val PICKER_TABS = listOf(R.string.custom_picker_tab_single, R.string.custom_picker_tab_groups)

@Composable
private fun PickerRow(title: String, leading: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.custom_picker_item_padding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.custom_picker_item_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(R.dimen.custom_picker_leading_width))
                .heightIn(min = dimensionResource(R.dimen.custom_picker_icon_size)),
            contentAlignment = Alignment.Center
        ) { leading() }
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ElementLeading(type: CustomElementType, onClick: () -> Unit) {
    when (type) {
        CustomElementType.VOLUME -> RockerPreview(volume = true, onClick = onClick)
        CustomElementType.CHANNEL -> RockerPreview(volume = false, onClick = onClick)
        CustomElementType.DPAD -> PickerIcon(R.drawable.ic_dpad_background)
        CustomElementType.APP -> PickerIcon(R.drawable.ic_apps)
        CustomElementType.KEY -> Unit
    }
}

// The real rocker drawn at its normal size and scaled down, so it follows palette and rounding.
@Composable
private fun RockerPreview(volume: Boolean, onClick: () -> Unit) {
    val width = dimensionResource(R.dimen.rocker_width)
    val height = dimensionResource(R.dimen.rocker_height)
    val previewHeight = dimensionResource(R.dimen.custom_picker_rocker_height)
    val scale = previewHeight / height
    Box(
        modifier = Modifier
            .size(width * scale, previewHeight)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        RockerKey(
            label = if (volume) null else stringResource(R.string.key_channel),
            labelIcon = if (volume) R.drawable.ic_volume else null,
            topIcon = if (volume) R.drawable.ic_plus else R.drawable.ic_chevron_up,
            bottomIcon = if (volume) R.drawable.ic_minus else R.drawable.ic_chevron_down,
            topDescription = "",
            bottomDescription = "",
            onTop = {},
            onBottom = {},
            modifier = Modifier
                .requiredSize(width, height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        // Taps add the element instead of pressing the preview's keys.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = null, indication = null, onClick = onClick)
        )
    }
}

// Icon of the key, or its label drawn like a small key, or the color of a color key.
@Composable
private fun KeyLeading(key: RemoteKey) {
    val visual = KeyCatalog.visual(key)
    val icon = visual.icon
    val label = visual.label?.let { stringResource(it) } ?: visual.text
    when {
        icon != null -> PickerIcon(icon, visual.iconTint?.let { colorResource(it) })
        label != null -> PickerLabel(label)
        else -> {
            val color = when (visual.style) {
                KeyStyle.RED -> R.color.key_color_red
                KeyStyle.GREEN -> R.color.key_color_green
                KeyStyle.YELLOW -> R.color.key_color_yellow
                KeyStyle.BLUE -> R.color.key_color_blue
                else -> null
            }
            if (color != null) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.custom_picker_color_size))
                        .background(colorResource(color), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PickerIcon(icon: Int, tint: Color? = null) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(dimensionResource(R.dimen.custom_picker_icon_size))
    )
}

@Composable
private fun PickerLabel(text: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .border(dimensionResource(R.dimen.custom_editor_outline), color, RoundedCornerShape(percent = 50))
            .padding(horizontal = dimensionResource(R.dimen.custom_picker_label_padding)),
        contentAlignment = Alignment.Center
    ) {
        FittedLabel(text = text, color = color)
    }
}
