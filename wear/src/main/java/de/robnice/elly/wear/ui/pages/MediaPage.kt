package de.robnice.elly.wear.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.components.RoundPage
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.floatResource

@Composable
fun MediaPage(slotKey: SlotKey) {
    val keySize = floatResource(R.dimen.key_size)
    val mediaY = floatResource(R.dimen.media_y)
    val sideX = floatResource(R.dimen.media_side_key_x)
    val navX = floatResource(R.dimen.media_nav_key_x)
    val navY = floatResource(R.dimen.media_nav_key_y)
    RoundPage {
        slotKey(WatchSlot.MEDIA_TOP, false, Modifier.at(0f, floatResource(R.dimen.media_top_key_y)).sized(keySize))
        slotKey(WatchSlot.MEDIA_CENTER, true, Modifier.at(0f, mediaY).sized(floatResource(R.dimen.large_key_size)))
        slotKey(WatchSlot.MEDIA_LEFT, false, Modifier.at(-sideX, mediaY).sized(keySize))
        slotKey(WatchSlot.MEDIA_RIGHT, false, Modifier.at(sideX, mediaY).sized(keySize))
        slotKey(WatchSlot.MEDIA_BOTTOM_LEFT, false, Modifier.at(-navX, navY).sized(keySize))
        slotKey(WatchSlot.MEDIA_BOTTOM_RIGHT, false, Modifier.at(navX, navY).sized(keySize))
    }
}
