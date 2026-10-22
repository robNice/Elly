package de.robnice.elgy.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import de.robnice.elgy.R
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.ScreenLayout
import java.util.Collections

private const val DRAG_SCALE = 1.06f

@Composable
fun ScreensSection(
    layout: ScreenLayout,
    screens: Map<RemoteScreen, CustomScreen>,
    onToggle: (RemoteScreen) -> Unit,
    onReorder: (List<RemoteScreen>) -> Unit,
    onEdit: (RemoteScreen) -> Unit,
    onReset: (RemoteScreen) -> Unit
) {
    val entries = remember(layout) { layout.entries.toMutableStateList() }
    var dragging by remember { mutableStateOf<RemoteScreen?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemStride by remember { mutableFloatStateOf(0f) }
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(text = stringResource(R.string.settings_screens), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.settings_screens_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                entries.forEach { entry ->
                    key(entry.screen) {
                        val isDragged = dragging == entry.screen
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .onSizeChanged { itemStride = it.width + spacingPx }
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationX = if (isDragged) dragOffset else 0f
                                    scaleX = if (isDragged) DRAG_SCALE else 1f
                                    scaleY = if (isDragged) DRAG_SCALE else 1f
                                }
                                .pointerInput(entry.screen, entries) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragging = entry.screen
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            dragging = null
                                            dragOffset = 0f
                                            currentOnReorder(entries.map { it.screen })
                                        },
                                        onDragCancel = {
                                            dragging = null
                                            dragOffset = 0f
                                            currentOnReorder(entries.map { it.screen })
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.x
                                            val index = entries.indexOfFirst { it.screen == entry.screen }
                                            if (dragOffset > itemStride / 2f && index < entries.lastIndex) {
                                                Collections.swap(entries, index, index + 1)
                                                dragOffset -= itemStride
                                            } else if (dragOffset < -itemStride / 2f && index > 0) {
                                                Collections.swap(entries, index, index - 1)
                                                dragOffset += itemStride
                                            }
                                        }
                                    )
                                }
                                .clickable { onToggle(entry.screen) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ScreenThumbnail(
                                layout = screens[entry.screen] ?: CustomScreen.EMPTY,
                                enabled = entry.enabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.screen_thumbnail_checkbox_spacing)))
                            Checkbox(checked = entry.enabled, onCheckedChange = null)
                            val number = entries.indexOf(entry) + 1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ScreenAction(R.drawable.ic_edit, stringResource(R.string.settings_screen_edit, number)) {
                                    onEdit(entry.screen)
                                }
                                ScreenAction(R.drawable.ic_restore, stringResource(R.string.settings_screen_reset, number)) {
                                    onReset(entry.screen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenAction(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(dimensionResource(R.dimen.screen_action_size))) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(dimensionResource(R.dimen.screen_action_icon_size))
        )
    }
}
