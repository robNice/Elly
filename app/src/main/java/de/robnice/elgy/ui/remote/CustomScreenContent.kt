package de.robnice.elgy.ui.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import de.robnice.elgy.R
import de.robnice.elgy.data.model.CustomElement
import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.components.DirectionalPad
import de.robnice.elgy.ui.components.KeyIcon
import de.robnice.elgy.ui.components.KeyLabel
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.RemoteKeyButton
import de.robnice.elgy.ui.components.RockerKey

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
fun CustomPage(
    screen: CustomScreen,
    onKey: (RemoteKey) -> Unit,
    onLaunchApp: (String) -> Unit
) {
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
        return
    }
    CustomGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.remote_page_padding))
            .onSizeChanged { if (it.height > 0) RemotePageShape.aspectRatio = it.width.toFloat() / it.height }
    ) { metrics ->
        screen.elements.forEach { element ->
            Box(modifier = Modifier.cell(element, metrics)) {
                CustomElementView(element, onKey, onLaunchApp)
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
                        shape = RoundedCornerShape(percent = 50),
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
        val square = maxWidth / maxHeight in ROUND_KEY_RATIO
        val buttonModifier = if (square) Modifier.size(min(maxWidth, maxHeight)) else Modifier.fillMaxSize()
        RemoteKeyButton(
            contentDescription = KeyCatalog.name(key),
            onPress = { onKey(key) },
            modifier = buttonModifier,
            shape = if (square) CircleShape else RoundedCornerShape(percent = 50),
            style = visual.style,
            repeatOnHold = visual.repeatOnHold
        ) { color ->
            // Keys with both an icon and a label (GUIDE) show the icon when round and the label as a pill.
            when {
                visual.icon != null && (square || visual.label == null) ->
                    KeyIcon(icon = visual.icon, tint = visual.iconTint?.let { colorResource(it) } ?: color)
                visual.label != null -> KeyLabel(text = stringResource(visual.label), color = color, fontWeight = FontWeight.Bold)
                visual.text != null -> KeyLabel(text = visual.text, color = color, fontSize = 20)
            }
        }
    }
}

private val ROUND_KEY_RATIO = 0.7f..1.5f
