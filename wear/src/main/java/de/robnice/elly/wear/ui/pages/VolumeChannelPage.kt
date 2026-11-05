package de.robnice.elly.wear.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.components.RoundPage
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.floatResource

// By default channel on the left, volume on the right; the volume keys help on watches without a rotating crown
// or bezel.
@Composable
fun VolumeChannelPage(slotKey: SlotKey) {
    val keySize = floatResource(R.dimen.key_size)
    val sideX = floatResource(R.dimen.side_key_x)
    val sideY = floatResource(R.dimen.side_key_y)
    RoundPage {
        slotKey(WatchSlot.VOL_TOP, false, Modifier.at(0f, floatResource(R.dimen.power_key_y)).sized(keySize))
        slotKey(WatchSlot.VOL_CENTER, true, Modifier.at(0f, 0f).sized(floatResource(R.dimen.large_key_size)))
        slotKey(WatchSlot.VOL_LEFT_TOP, false, Modifier.at(sideX, -sideY).sized(keySize))
        slotKey(WatchSlot.VOL_LEFT_BOTTOM, false, Modifier.at(sideX, sideY).sized(keySize))
        slotKey(WatchSlot.VOL_RIGHT_TOP, false, Modifier.at(-sideX, -sideY).sized(keySize))
        slotKey(WatchSlot.VOL_RIGHT_BOTTOM, false, Modifier.at(-sideX, sideY).sized(keySize))
    }
}
