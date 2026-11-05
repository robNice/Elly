package de.robnice.elly.ui.settings

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elly.R
import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchPage
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.sync.WatchPresence
import de.robnice.elly.ui.components.KeyStyle
import de.robnice.elly.ui.remote.KeyCatalog
import de.robnice.elly.ui.remote.rememberAppIcon

@Composable
fun WatchSection(viewModel: WatchSettingsViewModel) {
    val presence by viewModel.presence.collectAsStateWithLifecycle()
    if (presence == WatchPresence.WITHOUT_APP) {
        InstallOnWatch(viewModel)
        return
    }
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    Text(
        text = stringResource(R.string.watch_settings_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val icons by viewModel.appIcons.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf<WatchEditTarget?>(null) }
    // Previews instead of names, as for the phone screens: once the keys are changed, a name would no longer fit.
    Text(text = stringResource(R.string.watch_pages_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        text = stringResource(R.string.watch_pages_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        ReorderableRow(
            items = layout.pages,
            onClick = { viewModel.toggle(it.page) },
            onReorder = { entries -> viewModel.reorder(entries.map { it.page }) },
            modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding))
        ) { entry, index ->
            val number = index + 1
            WatchPreview(
                target = WatchEditTarget.Page(entry.page),
                layout = layout,
                icons = icons,
                onSlot = null,
                modifier = Modifier.alpha(if (entry.visible) 1f else DISABLED_ALPHA)
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.screen_thumbnail_checkbox_spacing)))
            Checkbox(checked = entry.visible, onCheckedChange = null)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewAction(R.drawable.ic_edit, stringResource(R.string.watch_page_edit, number)) {
                    viewModel.edit(WatchEditTarget.Page(entry.page))
                }
                PreviewAction(R.drawable.ic_restore, stringResource(R.string.watch_page_reset, number)) {
                    confirmReset = WatchEditTarget.Page(entry.page)
                }
            }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WatchPreview(
                target = WatchEditTarget.Tile,
                layout = layout,
                icons = icons,
                onSlot = null,
                modifier = Modifier.width(dimensionResource(R.dimen.watch_tile_thumbnail_size))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.watch_tile), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.watch_tile_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PreviewAction(R.drawable.ic_edit, stringResource(R.string.watch_edit_keys)) { viewModel.edit(WatchEditTarget.Tile) }
            PreviewAction(R.drawable.ic_restore, stringResource(R.string.watch_tile_reset)) { confirmReset = WatchEditTarget.Tile }
        }
    }
    WatchDialogs(viewModel, layout)
    confirmReset?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            title = { Text(stringResource(R.string.watch_reset_title)) },
            text = { Text(stringResource(R.string.watch_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = null
                    viewModel.reset(target)
                }) { Text(stringResource(R.string.settings_screen_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = null }) { Text(stringResource(R.string.dialog_cancel)) } }
        )
    }
}

@Composable
private fun PreviewAction(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(dimensionResource(R.dimen.screen_action_size))) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(dimensionResource(R.dimen.screen_action_icon_size))
        )
    }
}

// A watch is connected, but Elly is not on it yet.
@Composable
private fun InstallOnWatch(viewModel: WatchSettingsViewModel) {
    val prompt by viewModel.installPrompt.collectAsStateWithLifecycle()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
        ) {
            Text(text = stringResource(R.string.watch_install_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.watch_install_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = viewModel::installOnWatch) { Text(stringResource(R.string.watch_install_button)) }
            prompt?.let { opened ->
                Text(
                    text = stringResource(if (opened) R.string.watch_install_opened else R.string.watch_install_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (opened) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WatchDialogs(viewModel: WatchSettingsViewModel, layout: WatchLayout) {
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val picking by viewModel.picking.collectAsStateWithLifecycle()
    val appPicker by viewModel.appPicker.collectAsStateWithLifecycle()
    val styling by viewModel.styling.collectAsStateWithLifecycle()
    val icons by viewModel.appIcons.collectAsStateWithLifecycle()
    val target = editing ?: return
    AlertDialog(
        onDismissRequest = viewModel::closeEditor,
        title = {
            Text(
                when (target) {
                    is WatchEditTarget.Page -> stringResource(R.string.watch_page_number, layout.pages.indexOfFirst { it.page == target.page } + 1)
                    WatchEditTarget.Tile -> stringResource(R.string.watch_tile)
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                WatchPreview(target, layout, icons, onSlot = viewModel::pick)
                Text(
                    text = stringResource(R.string.watch_editor_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = viewModel::closeEditor) { Text(stringResource(R.string.dialog_ok)) } }
    )
    picking?.let { slot ->
        WatchActionPicker(
            current = layout.action(slot),
            onStyle = viewModel::styleApp,
            onPick = viewModel::assign,
            onPickApp = viewModel::openAppPicker,
            onDefault = { viewModel.assign(null) },
            onDismiss = viewModel::dismissPicker
        )
    }
    styling?.let { slot ->
        (layout.action(slot) as? WatchAction.App)?.let { app ->
            AppDisplayDialog(app, icons, onDisplay = viewModel::setDisplay, onDismiss = viewModel::closeStyle)
        }
    }
    appPicker?.let { state ->
        AppPickerDialog(
            state = state,
            onSelect = viewModel::assignApp,
            onClear = {
                viewModel.dismissAppPicker()
                viewModel.assign(null)
            },
            onDismiss = viewModel::dismissAppPicker
        )
    }
}

// The round watch page with its keys where the watch draws them; the directional pad and the digits are shown
// dimmed, they cannot be changed. Without onSlot it is a small preview without texts.
@Composable
private fun WatchPreview(
    target: WatchEditTarget,
    layout: WatchLayout,
    icons: Map<String, String>,
    onSlot: ((WatchSlot) -> Unit)?,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val thumbnail = onSlot == null
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(colorResource(R.color.watch_preview_background))
    ) {
        val d = maxWidth
        val key = fraction(R.dimen.watch_preview_key)
        val large = fraction(R.dimen.watch_preview_large_key)
        val iconSize = d * fraction(R.dimen.watch_preview_icon)
        val slot: @Composable (WatchSlot, Float, Float, Float) -> Unit = { s, x, y, size ->
            SlotFace(layout.action(s), icons, iconSize, onClick = onSlot?.let { { it(s) } }, modifier = place(d, x, y, size))
        }
        val fixed: @Composable (String, Modifier) -> Unit = { text, place -> FixedFace(text.takeUnless { thumbnail }, place) }
        val padY = fraction(R.dimen.watch_preview_dpad_y)
        val padSize = fraction(R.dimen.watch_preview_dpad_size)
        when (target) {
            is WatchEditTarget.Page -> when (target.page) {
                WatchPage.NAVIGATION -> {
                    val x = fraction(R.dimen.watch_preview_nav_key_x)
                    val y = fraction(R.dimen.watch_preview_nav_key_y)
                    fixed("", place(d, 0f, padY, padSize))
                    fixed(stringResource(R.string.key_ok), place(d, 0f, padY, fraction(R.dimen.watch_preview_dpad_center_size)))
                    slot(WatchSlot.NAV_LEFT, -x, y, key)
                    slot(WatchSlot.NAV_RIGHT, x, y, key)
                }
                WatchPage.VOLUME_CHANNEL -> {
                    val x = fraction(R.dimen.watch_preview_side_key_x)
                    val y = fraction(R.dimen.watch_preview_side_key_y)
                    slot(WatchSlot.VOL_TOP, 0f, fraction(R.dimen.watch_preview_power_key_y), key)
                    slot(WatchSlot.VOL_CENTER, 0f, 0f, large)
                    slot(WatchSlot.VOL_LEFT_TOP, -x, -y, key)
                    slot(WatchSlot.VOL_LEFT_BOTTOM, -x, y, key)
                    slot(WatchSlot.VOL_RIGHT_TOP, x, -y, key)
                    slot(WatchSlot.VOL_RIGHT_BOTTOM, x, y, key)
                }
                WatchPage.NUMBERS -> {
                    val step = fraction(R.dimen.watch_preview_digit_step)
                    val size = fraction(R.dimen.watch_preview_digit_size)
                    for (row in 0..2) for (column in 0..2) {
                        fixed((row * 3 + column + 1).toString(), place(d, (column - 1) * step, (row - 1.5f) * step, size))
                    }
                    slot(WatchSlot.NUM_LEFT, -step, 1.5f * step, size)
                    fixed("0", place(d, 0f, 1.5f * step, size))
                    slot(WatchSlot.NUM_RIGHT, step, 1.5f * step, size)
                }
                WatchPage.POINTER -> {
                    val x = fraction(R.dimen.watch_preview_nav_key_x)
                    val y = fraction(R.dimen.watch_preview_nav_key_y)
                    Icon(
                        painter = painterResource(R.drawable.ic_pointer),
                        contentDescription = stringResource(R.string.watch_pointer_area),
                        tint = colorResource(R.color.watch_preview_fixed),
                        modifier = place(d, 0f, padY, fraction(R.dimen.watch_preview_pointer_icon))
                    )
                    slot(WatchSlot.POINTER_LEFT, -x, y, key)
                    slot(WatchSlot.POINTER_RIGHT, x, y, key)
                }
                WatchPage.MEDIA -> {
                    val y = fraction(R.dimen.watch_preview_media_y)
                    val side = fraction(R.dimen.watch_preview_media_side_x)
                    val navX = fraction(R.dimen.watch_preview_media_nav_x)
                    val navY = fraction(R.dimen.watch_preview_media_nav_y)
                    slot(WatchSlot.MEDIA_TOP, 0f, fraction(R.dimen.watch_preview_media_top_y), key)
                    slot(WatchSlot.MEDIA_CENTER, 0f, y, large)
                    slot(WatchSlot.MEDIA_LEFT, -side, y, key)
                    slot(WatchSlot.MEDIA_RIGHT, side, y, key)
                    slot(WatchSlot.MEDIA_BOTTOM_LEFT, -navX, navY, key)
                    slot(WatchSlot.MEDIA_BOTTOM_RIGHT, navX, navY, key)
                }
            }
            WatchEditTarget.Tile -> {
                val x = fraction(R.dimen.watch_preview_tile_x)
                val y = fraction(R.dimen.watch_preview_tile_y)
                val size = fraction(R.dimen.watch_preview_tile_key)
                WatchSlot.TILE.forEachIndexed { index, s ->
                    slot(s, (index % 3 - 1) * x, if (index < 3) -y else y, size)
                }
            }
        }
    }
}

@Composable
private fun fraction(@DimenRes id: Int): Float = ResourcesCompat.getFloat(LocalResources.current, id)

private fun BoxWithConstraintsScope.place(d: Dp, x: Float, y: Float, size: Float): Modifier =
    Modifier
        .align(Alignment.Center)
        .absoluteOffset(d * x, d * y)
        .size(d * size)

@Composable
private fun SlotFace(action: WatchAction, icons: Map<String, String>, iconSize: Dp, onClick: (() -> Unit)?, modifier: Modifier) {
    val name = actionName(action)
    val background = when {
        action is WatchAction.Key -> keyBackground(action.key)
        else -> null
    } ?: colorResource(R.color.watch_preview_key)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick).semantics { contentDescription = name } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        ActionFace(action, icons, iconSize, showText = onClick != null)
    }
}

@Composable
private fun FixedFace(text: String?, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(dimensionResource(R.dimen.custom_editor_outline), colorResource(R.color.watch_preview_fixed), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        text?.takeIf { it.isNotEmpty() }?.let { Text(text = it, style = MaterialTheme.typography.labelMedium, color = colorResource(R.color.watch_preview_fixed)) }
    }
}

// Icon, label or app icon of a function, as the watch shows it; small previews leave the texts out.
@Composable
private fun ActionFace(action: WatchAction, icons: Map<String, String>, iconSize: Dp, showText: Boolean = true) {
    val content = colorResource(R.color.watch_preview_key_content)
    when (action) {
        is WatchAction.Key -> {
            val visual = KeyCatalog.visual(action.key)
            val label = visual.label?.let { stringResource(it) } ?: visual.text
            when {
                visual.icon != null -> Icon(
                    painter = painterResource(visual.icon),
                    contentDescription = null,
                    tint = visual.iconTint?.let { colorResource(it) } ?: content,
                    modifier = Modifier.size(iconSize)
                )
                label != null && showText -> Text(text = label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
            }
        }
        WatchAction.PlayPause -> Icon(painterResource(R.drawable.ic_play_pause), null, tint = content, modifier = Modifier.size(iconSize))
        is WatchAction.App -> {
            val icon = rememberAppIcon(icons[action.appId]).takeIf { action.display != AppDisplay.TEXT }
            val name: @Composable (Int) -> Unit = { lines ->
                if (showText) Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = lines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.watch_preview_text_padding))
                )
            }
            when {
                icon == null -> name(2)
                action.display == AppDisplay.BOTH -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(iconSize).clip(CircleShape))
                    name(1)
                }
                else -> Image(bitmap = icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun keyBackground(key: RemoteKey): Color? = when (KeyCatalog.visual(key).style) {
    KeyStyle.POWER, KeyStyle.RED -> colorResource(R.color.key_color_red)
    KeyStyle.GREEN -> colorResource(R.color.key_color_green)
    KeyStyle.YELLOW -> colorResource(R.color.key_color_yellow)
    KeyStyle.BLUE -> colorResource(R.color.key_color_blue)
    else -> null
}

@Composable
private fun actionName(action: WatchAction): String = when (action) {
    is WatchAction.Key -> KeyCatalog.name(action.key)
    WatchAction.PlayPause -> stringResource(R.string.watch_action_play_pause)
    is WatchAction.App -> action.title
}

@Composable
private fun WatchActionPicker(
    current: WatchAction,
    onStyle: () -> Unit,
    onPick: (WatchAction) -> Unit,
    onPickApp: () -> Unit,
    onDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val itemPadding = dimensionResource(R.dimen.custom_picker_item_padding)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.watch_picker_title)) },
        text = {
            LazyColumn(modifier = Modifier.height(dimensionResource(R.dimen.custom_picker_max_height))) {
                if (current is WatchAction.App) {
                    item {
                        PickerRow(
                            title = stringResource(R.string.watch_app_display_change, current.title),
                            leading = { PickerIcon(R.drawable.ic_palette) },
                            onClick = onStyle
                        )
                    }
                }
                item {
                    PickerRow(
                        title = stringResource(R.string.watch_picker_app),
                        leading = { PickerIcon(R.drawable.ic_apps) },
                        onClick = onPickApp
                    )
                }
                item {
                    PickerRow(
                        title = stringResource(R.string.watch_action_play_pause),
                        leading = { PickerIcon(R.drawable.ic_play_pause) },
                        onClick = { onPick(WatchAction.PlayPause) }
                    )
                }
                item { PickerHeader(R.string.watch_picker_keys, itemPadding) }
                items(WatchAction.ASSIGNABLE_KEYS) { key ->
                    PickerRow(
                        title = KeyCatalog.name(key),
                        leading = { KeyLeading(key) },
                        onClick = { onPick(WatchAction.Key(key)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDefault) { Text(stringResource(R.string.watch_picker_default)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

// How an app key shows its app, with the key as the watch draws it.
@Composable
private fun AppDisplayDialog(
    app: WatchAction.App,
    icons: Map<String, String>,
    onDisplay: (AppDisplay) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.watch_app_display)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(dimensionResource(R.dimen.watch_display_preview_size))
                        .clip(CircleShape)
                        .background(colorResource(R.color.watch_preview_background))
                        .padding(dimensionResource(R.dimen.watch_display_preview_padding))
                ) {
                    SlotFace(app, icons, dimensionResource(R.dimen.watch_display_preview_icon_size), onClick = {}, modifier = Modifier.fillMaxSize())
                }
                AppDisplay.entries.forEach { display ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = app.display == display, role = Role.RadioButton) { onDisplay(display) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = app.display == display, onClick = null)
                        Text(
                            text = stringResource(display.title()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = dimensionResource(R.dimen.setup_item_spacing))
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) } }
    )
}

@Composable
private fun PickerHeader(@StringRes title: Int, itemPadding: Dp) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = itemPadding, bottom = itemPadding / 2)
    )
}

private fun AppDisplay.title(): Int = when (this) {
    AppDisplay.ICON -> R.string.watch_app_display_icon
    AppDisplay.TEXT -> R.string.watch_app_display_text
    AppDisplay.BOTH -> R.string.watch_app_display_both
}

private const val DISABLED_ALPHA = 0.4f
