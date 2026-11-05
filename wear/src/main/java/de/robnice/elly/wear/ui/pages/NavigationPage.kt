package de.robnice.elly.wear.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.MaterialTheme
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.components.DirectionalPad
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.RoundPage
import de.robnice.elly.wear.ui.components.StatusLight
import de.robnice.elly.wear.ui.components.directionTaps
import de.robnice.elly.wear.ui.components.floatResource

@Composable
fun NavigationPage(tvName: String, connection: ConnectionState, sentCount: Int, onKey: (RemoteKey) -> Unit, slotKey: SlotKey) {
    val keySize = floatResource(R.dimen.key_size)
    val keyX = floatResource(R.dimen.nav_key_x)
    val keyY = floatResource(R.dimen.nav_key_y)
    val padY = floatResource(R.dimen.dpad_y)
    val centerSize = floatResource(R.dimen.dpad_center_size)
    val density = LocalDensity.current
    RoundPage {
        Box(
            Modifier
                .fillMaxSize()
                .directionTaps(
                    centerY = { with(density) { (diameter * padY).toPx() } },
                    centerRadius = { with(density) { (diameter * centerSize).toPx() / 2 } },
                    onKey = onKey
                )
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .at(0f, floatResource(R.dimen.tv_name_y))
                .wide(floatResource(R.dimen.tv_name_width))
        ) {
            StatusLight(connection, sentCount)
            Spacer(Modifier.width(dimensionResource(R.dimen.status_light_spacing)))
            val style = MaterialTheme.typography.labelMedium
            BasicText(
                text = tvName,
                style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = with(density) { dimensionResource(R.dimen.tv_name_min_size).toSp() },
                    maxFontSize = style.fontSize
                ),
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        DirectionalPad(
            centerSize = diameter * centerSize,
            onKey = onKey,
            modifier = Modifier
                .at(0f, padY)
                .sized(floatResource(R.dimen.dpad_size))
        )
        slotKey(WatchSlot.NAV_LEFT, false, Modifier.at(-keyX, keyY).sized(keySize))
        slotKey(WatchSlot.NAV_RIGHT, false, Modifier.at(keyX, keyY).sized(keySize))
    }
}
