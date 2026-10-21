package de.robnice.elgy.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.min
import de.robnice.elgy.R
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.components.DirectionalPad
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.RockerKey
import de.robnice.elgy.ui.pager.RemotePage

@Composable
fun RemotePageContent(
    page: RemotePage,
    onKey: (RemoteKey) -> Unit,
    appSlots: List<AppSlot>,
    onLaunchApp: (String) -> Unit
) {
    when (page) {
        RemotePage.NAVIGATION -> NavigationPage(onKey)
        RemotePage.NUMBERS -> NumbersPage(onKey)
        RemotePage.MEDIA -> MediaPage(onKey, appSlots, onLaunchApp)
    }
}

@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.remote_page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) { content() }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun NavigationPage(onKey: (RemoteKey) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.remote_page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.remote_row_spacing))
    ) {
        KeyRow {
            IconKey(RemoteKey.POWER, R.drawable.ic_power, R.string.key_power, onKey, style = KeyStyle.POWER)
            IconKey(RemoteKey.HOME, R.drawable.ic_home, R.string.key_home, onKey)
            IconKey(RemoteKey.SETTINGS, R.drawable.ic_settings, R.string.key_settings, onKey)
            IconKey(RemoteKey.LIVE_TV, R.drawable.ic_live_tv, R.string.key_live_tv, onKey)
        }
        KeyRow {
            RockerKey(
                label = null,
                labelIcon = R.drawable.ic_volume,
                topIcon = R.drawable.ic_plus,
                bottomIcon = R.drawable.ic_minus,
                topDescription = stringResource(R.string.key_volume_up),
                bottomDescription = stringResource(R.string.key_volume_down),
                onTop = { onKey(RemoteKey.VOLUME_UP) },
                onBottom = { onKey(RemoteKey.VOLUME_DOWN) }
            )
            IconKey(RemoteKey.MUTE, R.drawable.ic_volume_off, R.string.key_mute, onKey, small = true)
            RockerKey(
                label = stringResource(R.string.key_channel),
                topIcon = R.drawable.ic_chevron_up,
                bottomIcon = R.drawable.ic_chevron_down,
                topDescription = stringResource(R.string.key_channel_up),
                bottomDescription = stringResource(R.string.key_channel_down),
                onTop = { onKey(RemoteKey.CHANNEL_UP) },
                onBottom = { onKey(RemoteKey.CHANNEL_DOWN) }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            DirectionalPad(onKey = onKey, modifier = Modifier.fillMaxSize())
        }
        KeyRow {
            IconKey(RemoteKey.BACK, R.drawable.ic_back, R.string.key_back, onKey)
            IconKey(RemoteKey.GUIDE, R.drawable.ic_guide, R.string.key_guide, onKey)
        }
    }
}

@Composable
private fun NumbersPage(onKey: (RemoteKey) -> Unit) {
    val digits = listOf(
        listOf(RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3),
        listOf(RemoteKey.DIGIT_4, RemoteKey.DIGIT_5, RemoteKey.DIGIT_6),
        listOf(RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9)
    )
    PageColumn {
        KeyRow {
            IconKey(RemoteKey.INPUT, R.drawable.ic_input, R.string.key_input, onKey)
            IconKey(RemoteKey.LIVE_TV, R.drawable.ic_live_tv, R.string.key_live_tv, onKey)
        }
        digits.forEach { row ->
            KeyRow {
                row.forEach { key ->
                    val digit = key.name.removePrefix("DIGIT_")
                    WideKey(key, digit, digit, onKey)
                }
            }
        }
        KeyRow {
            WideKey(RemoteKey.GUIDE, stringResource(R.string.key_label_guide), stringResource(R.string.key_guide), onKey, fontSize = 14)
            WideKey(RemoteKey.DIGIT_0, "0", "0", onKey)
            WideKey(RemoteKey.MORE_ACTIONS, stringResource(R.string.key_label_more), stringResource(R.string.key_more_actions), onKey)
        }
        KeyRow {
            IconKey(RemoteKey.BACK, R.drawable.ic_back, R.string.key_back, onKey)
            TextKey(RemoteKey.OK, R.string.key_ok, onKey)
        }
    }
}

@Composable
private fun MediaPage(onKey: (RemoteKey) -> Unit, appSlots: List<AppSlot>, onLaunchApp: (String) -> Unit) {
    val rowSpacing = dimensionResource(R.dimen.remote_row_spacing)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.remote_page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        val transportSpacing = dimensionResource(R.dimen.transport_key_spacing)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(transportSpacing)) {
                TransportKey(
                    RemoteKey.RECORD,
                    R.drawable.ic_record,
                    R.string.key_record,
                    onKey,
                    iconTint = colorResource(R.color.key_color_red)
                )
                TransportKey(RemoteKey.LIVE_STOP, R.drawable.ic_live_stop, R.string.key_live_stop, onKey)
                TransportKey(RemoteKey.LIVE_PLAY, R.drawable.ic_live_play, R.string.key_live_play, onKey)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(transportSpacing)) {
                TransportKey(RemoteKey.REWIND, R.drawable.ic_fast_rewind, R.string.key_rewind, onKey)
                TransportKey(RemoteKey.PAUSE, R.drawable.ic_pause, R.string.key_pause, onKey)
                TransportKey(RemoteKey.PLAY, R.drawable.ic_play, R.string.key_play, onKey)
                TransportKey(RemoteKey.FAST_FORWARD, R.drawable.ic_fast_forward, R.string.key_fast_forward, onKey)
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val backSize = dimensionResource(R.dimen.key_size_small)
            val padSize = min(min(maxWidth, maxHeight - backSize - rowSpacing), dimensionResource(R.dimen.dpad_max_size))
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                DirectionalPad(onKey = onKey, modifier = Modifier.size(padSize))
                IconKey(RemoteKey.BACK, R.drawable.ic_back, R.string.key_back, onKey, small = true)
            }
        }
        KeyRow {
            ColorKey(RemoteKey.RED, KeyStyle.RED, R.string.key_red, onKey)
            ColorKey(RemoteKey.GREEN, KeyStyle.GREEN, R.string.key_green, onKey)
            ColorKey(RemoteKey.YELLOW, KeyStyle.YELLOW, R.string.key_yellow, onKey)
            ColorKey(RemoteKey.BLUE, KeyStyle.BLUE, R.string.key_blue, onKey)
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.app_keys_top_spacing)))
        appSlots.filter { it.isUsable }.chunked(APPS_PER_ROW).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                row.forEach { slot ->
                    val appId = slot.appId ?: return@forEach
                    AppKey(slot = slot, onPress = { onLaunchApp(appId) }, modifier = Modifier.weight(1f))
                }
                repeat(APPS_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private const val APPS_PER_ROW = 3
