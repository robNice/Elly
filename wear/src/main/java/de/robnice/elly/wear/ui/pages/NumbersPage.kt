package de.robnice.elly.wear.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ScreenScaffold
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.TextKey

// Each digit goes to the TV right away; ✓ confirms the number so the TV switches without waiting for its timeout.
// A 3 x 4 block of 48 dp keys is taller than a small watch, so the block scrolls; on this page the crown
// scrolls it and the volume stays on the ring gesture.
@Composable
fun NumbersPage(onKey: (RemoteKey) -> Unit, slotKey: SlotKey) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val keySize = Modifier.size(dimensionResource(R.dimen.digit_key_size))
    val spacing = Arrangement.Absolute.spacedBy(dimensionResource(R.dimen.digit_key_spacing))
    val digit: (Char) -> Unit = { onKey(RemoteKey.valueOf("DIGIT_$it")) }
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            listOf("123", "456", "789").forEach { row ->
                item {
                    Row(horizontalArrangement = spacing) {
                        row.forEach { d -> TextKey(d.toString(), { digit(d) }, keySize) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = spacing) {
                    slotKey(WatchSlot.NUM_LEFT, false, keySize)
                    TextKey("0", { digit('0') }, keySize)
                    slotKey(WatchSlot.NUM_RIGHT, false, keySize)
                }
            }
        }
    }
}
