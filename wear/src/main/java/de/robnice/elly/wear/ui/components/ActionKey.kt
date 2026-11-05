package de.robnice.elly.wear.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.wear.R
import de.robnice.elly.wear.remote.ActionCatalog

// A key showing whatever function the phone gave it: an app with its icon or name, a key with its icon or label.
@Composable
fun ActionKey(
    action: WatchAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    appIcon: ImageBitmap? = null
) {
    val resources = LocalResources.current
    val visual = remember(action, resources) { ActionCatalog.visual(action, resources) }
    val iconSize = dimensionResource(if (large) R.dimen.mute_icon_size else R.dimen.key_icon_size)
    val described = modifier.semantics { contentDescription = visual.description }
    val app = action as? WatchAction.App
    val icon = appIcon?.takeIf { app?.display != AppDisplay.TEXT }
    val content: @Composable BoxScope.() -> Unit = {
        when {
            icon != null && app?.display == AppDisplay.BOTH -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                )
                AppName(app.title, maxLines = 1)
            }
            // The app icon fills the whole key, like a launcher icon.
            icon != null -> Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            visual.icon != null -> Icon(
                painter = painterResource(visual.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = visual.content?.let { colorResource(it) } ?: LocalContentColor.current
            )
            app != null -> AppName(app.title, maxLines = 2)
            visual.text != null -> Text(
                text = visual.text,
                style = if (action is WatchAction.App) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = if (action is WatchAction.App) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.dpad_icon_inset))
            )
        }
    }
    val container = visual.container
    if (container != null) {
        FilledIconButton(
            onClick = onClick,
            modifier = described,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colorResource(container),
                contentColor = visual.content?.let { colorResource(it) } ?: MaterialTheme.colorScheme.onPrimary
            ),
            content = content
        )
    } else {
        FilledTonalIconButton(onClick = onClick, modifier = described, content = content)
    }
}

// App names shrink until they fit instead of breaking inside a word; a single word stays on one line.
@Composable
private fun AppName(name: String, maxLines: Int) {
    val style = MaterialTheme.typography.labelSmall
    val minSize = with(LocalDensity.current) { dimensionResource(R.dimen.app_name_min_size).toSp() }
    val color = LocalContentColor.current
    BasicText(
        text = name,
        style = style.copy(color = color, textAlign = TextAlign.Center),
        maxLines = minOf(maxLines, name.split(' ').count { it.isNotBlank() }.coerceAtLeast(1)),
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = style.fontSize)
    )
}

// Draws the key of a slot with the function the phone gave it; large keys sit in the middle of a page.
typealias SlotKey = @Composable (slot: WatchSlot, large: Boolean, modifier: Modifier) -> Unit
