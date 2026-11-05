package de.robnice.elly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.zIndex
import de.robnice.elly.R
import java.util.Collections

private const val DRAG_SCALE = 1.06f

// Previews side by side: a tap calls onClick, touching and holding one and dragging it changes the order.
@Composable
fun <T : Any> ReorderableRow(
    items: List<T>,
    onClick: (T) -> Unit,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(item: T, index: Int) -> Unit
) {
    val entries = remember(items) { items.toMutableStateList() }
    var dragging by remember { mutableStateOf<T?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemStride by remember { mutableFloatStateOf(0f) }
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        entries.forEach { item ->
            key(item) {
                val isDragged = dragging == item
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
                        .pointerInput(item, entries) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragging = item
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    dragging = null
                                    dragOffset = 0f
                                    currentOnReorder(entries.toList())
                                },
                                onDragCancel = {
                                    dragging = null
                                    dragOffset = 0f
                                    currentOnReorder(entries.toList())
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                    val index = entries.indexOf(item)
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
                        .clickable { onClick(item) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content(item, entries.indexOf(item))
                }
            }
        }
    }
}
