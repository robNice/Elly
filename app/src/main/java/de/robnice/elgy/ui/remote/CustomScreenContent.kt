package de.robnice.elgy.ui.remote

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import de.robnice.elgy.R
import de.robnice.elgy.data.model.CellShape
import de.robnice.elgy.data.model.CustomElement
import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.components.DirectionalPad
import de.robnice.elgy.ui.components.KeyIcon
import de.robnice.elgy.ui.components.KeyLabel
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.LocalDpadRounding
import de.robnice.elgy.ui.components.LocalKeyRounding
import de.robnice.elgy.ui.components.RemoteKeyButton
import de.robnice.elgy.ui.components.RockerKey
import de.robnice.elgy.ui.components.ShadowLayer
import de.robnice.elgy.ui.components.keyShape

class CustomGridMetrics(val cellWidth: Dp, val cellHeight: Dp)

// Shape of the grid on the remote, so the editor can show the same cell proportions.
object RemotePageShape {
    var aspectRatio by mutableStateOf<Float?>(null)
}

@Composable
fun CustomGrid(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.(CustomGridMetrics) -> Unit) {
    BoxWithConstraints(modifier = modifier) {
        content(CustomGridMetrics(maxWidth / CustomScreen.COLUMNS, maxHeight / CustomScreen.ROWS))
    }
}

fun Modifier.cell(element: CustomElement, metrics: CustomGridMetrics): Modifier = this
    .offset(metrics.cellWidth * element.x, metrics.cellHeight * element.y)
    .size(metrics.cellWidth * element.width, metrics.cellHeight * element.height)

@Composable
fun rememberCellShape(metrics: CustomGridMetrics) = CellShape(
    metrics.cellWidth / metrics.cellHeight,
    dimensionResource(R.dimen.custom_element_gap) / metrics.cellHeight,
    LocalKeyRounding.current,
    LocalDpadRounding.current
)

// Frames may overlap where the shapes do not, so smaller elements lie on top to get the taps in those corners.
val CustomElement.layer: Float
    get() = 0.5f * (1f - width * height.toFloat() / (CustomScreen.COLUMNS * CustomScreen.ROWS))

@Composable
fun CustomPage(
    screen: CustomScreen,
    onKey: (RemoteKey) -> Unit,
    onLaunchApp: (String) -> Unit,
    onDoubleTap: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Keys lie above this area and take their own taps, so only empty spots react.
        if (onDoubleTap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(onDoubleTap) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
            )
        }
        if (screen.elements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.remote_page_padding)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.custom_screen_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            CustomGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.remote_page_padding))
                    .onSizeChanged { if (it.height > 0) RemotePageShape.aspectRatio = it.width.toFloat() / it.height }
            ) { metrics ->
                ShadowLayer(screen.elements, rememberCellShape(metrics), metrics.cellHeight)
                screen.elements.forEach { element ->
                    Box(modifier = Modifier.cell(element, metrics).zIndex(element.layer)) {
                        CustomElementView(element, onKey, onLaunchApp)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomElementView(
    element: CustomElement,
    onKey: (RemoteKey) -> Unit,
    onLaunchApp: (String) -> Unit,
    editing: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.custom_element_gap)),
        contentAlignment = Alignment.Center
    ) {
        when (element.type) {
            CustomElementType.KEY -> element.key?.let { SingleKey(it, onKey) }
            CustomElementType.VOLUME -> RockerKey(
                label = null,
                labelIcon = R.drawable.ic_volume,
                topIcon = R.drawable.ic_plus,
                bottomIcon = R.drawable.ic_minus,
                topDescription = stringResource(R.string.key_volume_up),
                bottomDescription = stringResource(R.string.key_volume_down),
                onTop = { onKey(RemoteKey.VOLUME_UP) },
                onBottom = { onKey(RemoteKey.VOLUME_DOWN) },
                modifier = Modifier.fillMaxSize()
            )
            CustomElementType.CHANNEL -> RockerKey(
                label = stringResource(R.string.key_channel),
                topIcon = R.drawable.ic_chevron_up,
                bottomIcon = R.drawable.ic_chevron_down,
                topDescription = stringResource(R.string.key_channel_up),
                bottomDescription = stringResource(R.string.key_channel_down),
                onTop = { onKey(RemoteKey.CHANNEL_UP) },
                onBottom = { onKey(RemoteKey.CHANNEL_DOWN) },
                modifier = Modifier.fillMaxSize()
            )
            CustomElementType.DPAD -> DirectionalPad(onKey = onKey, modifier = Modifier.fillMaxSize(), capped = false)
            CustomElementType.APP -> {
                val slot = element.app
                val appId = slot?.appId
                if (slot != null && slot.isUsable && appId != null) {
                    AppKey(slot = slot, onPress = { onLaunchApp(appId) }, modifier = Modifier.fillMaxSize())
                } else if (editing) {
                    RemoteKeyButton(
                        contentDescription = stringResource(R.string.custom_app_hold),
                        onPress = {},
                        modifier = Modifier.fillMaxSize(),
                        shape = keyShape(LocalKeyRounding.current),
                        style = KeyStyle.LIGHT
                    ) { color -> KeyLabel(text = stringResource(R.string.custom_app_hold), color = color, fontSize = 13) }
                }
            }
        }
    }
}

@Composable
private fun SingleKey(key: RemoteKey, onKey: (RemoteKey) -> Unit) {
    val visual = KeyCatalog.visual(key)
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Cells are not square, so decide by the actual area whether the key is round or a pill.
        val square = maxWidth / maxHeight in CustomElement.ROUND_KEY_RATIO
        val buttonModifier = if (square) Modifier.size(min(maxWidth, maxHeight)) else Modifier.fillMaxSize()
        RemoteKeyButton(
            contentDescription = KeyCatalog.name(key),
            onPress = { onKey(key) },
            modifier = buttonModifier,
            shape = keyShape(LocalKeyRounding.current),
            style = visual.style,
            repeatOnHold = visual.repeatOnHold
        ) { color ->
            when {
                visual.icon != null -> KeyIcon(icon = visual.icon, tint = visual.iconTint?.let { colorResource(it) } ?: color)
                visual.label != null -> {
                    val text = stringResource(visual.label)
                    val fontSize = when {
                        !square -> PILL_LABEL_SIZE
                        text.length > SHORT_LABEL_LENGTH -> ROUND_LONG_LABEL_SIZE
                        else -> ROUND_SHORT_LABEL_SIZE
                    }
                    KeyLabel(text = text, color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
                visual.text != null -> KeyLabel(text = visual.text, color = color, fontSize = 20)
            }
        }
    }
}

private const val PILL_LABEL_SIZE = 15
private const val ROUND_SHORT_LABEL_SIZE = 18
private const val ROUND_LONG_LABEL_SIZE = 13
private const val SHORT_LABEL_LENGTH = 3
