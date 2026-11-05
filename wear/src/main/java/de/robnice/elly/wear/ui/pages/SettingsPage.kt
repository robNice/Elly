package de.robnice.elly.wear.ui.pages

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.wear.BuildConfig
import de.robnice.elly.wear.R
import de.robnice.elly.wear.ui.WearRemoteViewModel

// Only the settings that belong to the watch; TVs, keys and root access are set up on the phone. The list
// keys of Wear Material 3 bring their own 48 dp touch targets and scale with the font size.
@Composable
fun SettingsPage(
    preferences: WearRemoteViewModel.Preferences,
    tvs: List<PairedTv>,
    activeTvId: String?,
    onHaptics: (Boolean) -> Unit,
    onRingGesture: (Boolean) -> Unit,
    onLightEffect: (Boolean) -> Unit,
    onPointerSpeed: (Float) -> Unit,
    onPointerInverted: (Boolean) -> Unit,
    onSelectTv: (String) -> Unit,
    phoneUnreachable: Boolean,
    onOpenApp: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.elly),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.settings_elly_size))
                )
            }
            item { ListHeader { Text(stringResource(R.string.settings_title)) } }
            item {
                Setting(preferences.haptics, onHaptics, stringResource(R.string.settings_haptics))
            }
            item {
                Setting(preferences.ringGesture, onRingGesture, stringResource(R.string.settings_ring_gesture))
            }
            item {
                Setting(preferences.lightEffect, onLightEffect, stringResource(R.string.settings_light_effect))
            }
            item { ListHeader { Text(stringResource(R.string.settings_pointer)) } }
            item {
                Text(
                    text = stringResource(R.string.settings_pointer_speed),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Slider(
                    value = preferences.pointerSpeed,
                    onValueChange = onPointerSpeed,
                    steps = POINTER_SPEED_STEPS,
                    valueRange = POINTER_SPEED_RANGE,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Setting(preferences.pointerInverted, onPointerInverted, stringResource(R.string.settings_pointer_inverted))
            }
            if (tvs.size > 1) {
                item { ListHeader { Text(stringResource(R.string.settings_tv)) } }
                tvs.forEach { tv ->
                    item {
                        RadioButton(
                            selected = tv.id == activeTvId,
                            onSelect = { onSelectTv(tv.id) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tv.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(dimensionResource(R.dimen.settings_section_spacing))) }
            item { PhoneButton(R.string.settings_open_app, onOpenApp) }
            item { PhoneButton(R.string.settings_open_privacy, onOpenPrivacy) }
            if (phoneUnreachable) {
                item {
                    Text(
                        text = stringResource(R.string.phone_unreachable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PhoneButton(@StringRes label: Int, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = { WrappingLabel(stringResource(label)) }
    )
}

@Composable
private fun Setting(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { WrappingLabel(label) }
    )
}

// Long single words (Finnish, Russian) would otherwise break without a hyphen in the narrow buttons.
@Composable
private fun WrappingLabel(text: String) {
    Text(text, maxLines = 3, overflow = TextOverflow.Ellipsis, style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto))
}

// 0.5 to 2 times the phone's default speed, in steps of a quarter.
private val POINTER_SPEED_RANGE = 0.5f..2f
private const val POINTER_SPEED_STEPS = 5
