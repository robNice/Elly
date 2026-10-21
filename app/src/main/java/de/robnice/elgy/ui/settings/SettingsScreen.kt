package de.robnice.elgy.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elgy.BuildConfig
import de.robnice.elgy.R
import de.robnice.elgy.remote.TvApp
import de.robnice.elgy.ui.remote.messageRes
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.data.model.MotionTuning
import de.robnice.elgy.data.model.JoystickPlacement
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.TouchStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onClose: () -> Unit, onOpenSetup: () -> Unit) {
    val demoMode by viewModel.demoMode.collectAsStateWithLifecycle()
    val keyHaptics by viewModel.keyHaptics.collectAsStateWithLifecycle()
    val appSlots by viewModel.appSlots.collectAsStateWithLifecycle()
    val appSlotsAvailable by viewModel.appSlotsAvailable.collectAsStateWithLifecycle()
    val appPicker by viewModel.appPicker.collectAsStateWithLifecycle()
    val styleEditor by viewModel.styleEditor.collectAsStateWithLifecycle()
    val screenLayout by viewModel.screenLayout.collectAsStateWithLifecycle()
    val activeTv by viewModel.activeTv.collectAsStateWithLifecycle()
    val pointerSettings by viewModel.pointerSettings.collectAsStateWithLifecycle()
    var confirmDisconnect by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    if (showPrivacy) {
        PrivacyScreen(onClose = { showPrivacy = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.setup_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_section_spacing))
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) { Text(stringResource(R.string.setup_close)) }
            }
            TvSection(
                activeTv = activeTv,
                onOpenSetup = onOpenSetup,
                onDisconnect = { confirmDisconnect = true }
            )
            if (appSlotsAvailable) {
                ScreensSection(
                    layout = screenLayout,
                    onToggle = viewModel::toggleScreen,
                    onReorder = viewModel::reorderScreens
                )
            }
            PointerSection(
                settings = pointerSettings,
                motionAvailable = viewModel.motionPointerAvailable,
                onModeChange = viewModel::setPointerMode,
                onOkPositionChange = viewModel::setOkButtonPosition,
                onTouchStyleChange = viewModel::setTouchStyle,
                onJoystickPlacementChange = viewModel::setJoystickPlacement,
                onSensitivityChange = viewModel::setMotionSensitivity,
                onAccelerationChange = viewModel::setMotionAcceleration
            )
            if (appSlotsAvailable) {
                AppKeysSection(
                    slots = appSlots,
                    onToggle = viewModel::setAppSlotEnabled,
                    onChoose = viewModel::openAppPicker,
                    onStyle = viewModel::openStyleEditor
                )
            }
            SwitchSetting(
                title = stringResource(R.string.settings_key_haptics),
                description = stringResource(R.string.settings_key_haptics_description),
                checked = keyHaptics,
                onCheckedChange = viewModel::setKeyHaptics
            )
            // Demo mode stays the last entry of the settings screen.
            SwitchSetting(
                title = stringResource(R.string.settings_demo_mode),
                description = stringResource(R.string.settings_demo_mode_description),
                checked = demoMode,
                onCheckedChange = viewModel::setDemoMode
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.settings_footer_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showPrivacy = true }) {
                Text(text = stringResource(R.string.settings_privacy), style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    appPicker?.let { picker ->
        AppPickerDialog(
            state = picker,
            onSelect = viewModel::assignApp,
            onClear = viewModel::clearAppSlot,
            onDismiss = viewModel::dismissAppPicker
        )
    }

    styleEditor?.let { index ->
        appSlots.getOrNull(index)?.let { slot ->
            AppSlotStyleDialog(
                index = index,
                slot = slot,
                onSave = { showIcon, showText, customText, textColor ->
                    viewModel.saveStyle(index, showIcon, showText, customText, textColor)
                },
                onDismiss = viewModel::dismissStyleEditor
            )
        }
    }

    val tvToDisconnect = activeTv
    if (confirmDisconnect && tvToDisconnect != null) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.settings_disconnect_title)) },
            text = { Text(stringResource(R.string.settings_disconnect_message, tvToDisconnect.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    viewModel.disconnectActiveTv()
                }) { Text(stringResource(R.string.settings_disconnect)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }
}

@Composable
private fun TvSection(activeTv: PairedTv?, onOpenSetup: () -> Unit, onDisconnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        Text(text = stringResource(R.string.settings_tv), style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                if (activeTv == null) {
                    Text(text = stringResource(R.string.settings_no_tv), style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(text = activeTv.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = listOfNotNull(activeTv.modelName, activeTv.ipAddress).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onOpenSetup) {
                    Text(stringResource(if (activeTv == null) R.string.settings_connect_tv else R.string.settings_connect_other_tv))
                }
                if (activeTv != null) {
                    OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.settings_disconnect)) }
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(dimensionResource(R.dimen.setup_card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_card_padding)))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun PointerSection(
    settings: PointerSettings,
    motionAvailable: Boolean,
    onModeChange: (PointerMode) -> Unit,
    onOkPositionChange: (OkButtonPosition) -> Unit,
    onTouchStyleChange: (TouchStyle) -> Unit,
    onJoystickPlacementChange: (JoystickPlacement) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onAccelerationChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        Text(text = stringResource(R.string.settings_pointer), style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.setup_item_spacing))) {
                SettingCaption(stringResource(R.string.settings_pointer_mode))
                ChoiceRow(
                    title = stringResource(R.string.settings_pointer_mode_motion),
                    description = stringResource(
                        if (motionAvailable) R.string.settings_pointer_mode_motion_description
                        else R.string.settings_pointer_mode_motion_unavailable
                    ),
                    selected = settings.mode == PointerMode.MOTION,
                    enabled = motionAvailable,
                    onSelect = { onModeChange(PointerMode.MOTION) }
                )
                if (settings.mode == PointerMode.MOTION) {
                    Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.settings_sub_option_indent))) {
                        SliderSetting(
                            title = stringResource(R.string.settings_motion_sensitivity),
                            value = settings.motionSensitivity,
                            range = MotionTuning.SENSITIVITY_RANGE,
                            onValueChange = onSensitivityChange
                        )
                        SliderSetting(
                            title = stringResource(R.string.settings_motion_acceleration),
                            value = settings.motionAcceleration,
                            range = MotionTuning.ACCELERATION_RANGE,
                            onValueChange = onAccelerationChange
                        )
                    }
                }
                ChoiceRow(
                    title = stringResource(R.string.settings_pointer_mode_touch),
                    description = stringResource(R.string.settings_pointer_mode_touch_description),
                    selected = settings.mode == PointerMode.TOUCH,
                    onSelect = { onModeChange(PointerMode.TOUCH) }
                )
                if (settings.mode == PointerMode.TOUCH) {
                    Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.settings_sub_option_indent))) {
                        SettingCaption(stringResource(R.string.settings_touch_style))
                        ChoiceRow(
                            title = stringResource(R.string.settings_touch_style_swipe),
                            description = stringResource(R.string.settings_touch_style_swipe_description),
                            selected = settings.touchStyle == TouchStyle.SWIPE,
                            onSelect = { onTouchStyleChange(TouchStyle.SWIPE) }
                        )
                        ChoiceRow(
                            title = stringResource(R.string.settings_touch_style_joystick),
                            description = stringResource(R.string.settings_touch_style_joystick_description),
                            selected = settings.touchStyle == TouchStyle.JOYSTICK,
                            onSelect = { onTouchStyleChange(TouchStyle.JOYSTICK) }
                        )
                        if (settings.touchStyle == TouchStyle.JOYSTICK) {
                            Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.settings_sub_option_indent))) {
                                SettingCaption(stringResource(R.string.settings_joystick_placement))
                                ChoiceRow(
                                    title = stringResource(R.string.settings_joystick_placement_free),
                                    description = stringResource(R.string.settings_joystick_placement_free_description),
                                    selected = settings.joystickPlacement == JoystickPlacement.FREE,
                                    onSelect = { onJoystickPlacementChange(JoystickPlacement.FREE) }
                                )
                                ChoiceRow(
                                    title = stringResource(R.string.settings_joystick_placement_fixed),
                                    description = stringResource(R.string.settings_joystick_placement_fixed_description),
                                    selected = settings.joystickPlacement == JoystickPlacement.FIXED,
                                    onSelect = { onJoystickPlacementChange(JoystickPlacement.FIXED) }
                                )
                            }
                        }
                        SettingCaption(stringResource(R.string.settings_ok_position))
                        ChoiceRow(
                            title = stringResource(R.string.settings_ok_position_bottom),
                            selected = settings.okButtonPosition == OkButtonPosition.BOTTOM,
                            onSelect = { onOkPositionChange(OkButtonPosition.BOTTOM) }
                        )
                        ChoiceRow(
                            title = stringResource(R.string.settings_ok_position_top),
                            selected = settings.okButtonPosition == OkButtonPosition.TOP,
                            onSelect = { onOkPositionChange(OkButtonPosition.TOP) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.setup_card_padding),
            vertical = dimensionResource(R.dimen.setup_item_spacing)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_motion_value, current),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onValueChange(current) },
            valueRange = range
        )
    }
}

@Composable
private fun SettingCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.setup_card_padding),
            vertical = dimensionResource(R.dimen.setup_item_spacing)
        )
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = dimensionResource(R.dimen.setup_item_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_item_spacing)))
        Column(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.setup_item_spacing))) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppKeysSection(
    slots: List<AppSlot>,
    onToggle: (Int, Boolean) -> Unit,
    onChoose: (Int) -> Unit,
    onStyle: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        Text(text = stringResource(R.string.settings_app_keys), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.settings_app_keys_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.setup_item_spacing))) {
                slots.forEachIndexed { index, slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.setup_card_padding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_app_slot, index + 1),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = slot.label ?: stringResource(R.string.settings_app_slot_empty),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (!slot.appId.isNullOrBlank()) {
                            TextButton(onClick = { onStyle(index) }) { Text(stringResource(R.string.settings_app_style)) }
                        }
                        TextButton(onClick = { onChoose(index) }) { Text(stringResource(R.string.settings_app_choose)) }
                        Switch(
                            checked = slot.enabled,
                            onCheckedChange = { onToggle(index, it) },
                            enabled = !slot.appId.isNullOrBlank()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    state: AppPickerState,
    onSelect: (TvApp) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_picker_title, state.slotIndex + 1)) },
        text = {
            when {
                state.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(dimensionResource(R.dimen.setup_progress_size)))
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_card_padding)))
                    Text(stringResource(R.string.app_picker_loading))
                }
                state.error != null -> Text(stringResource(state.error.messageRes() ?: R.string.result_failed))
                state.apps.isEmpty() -> Text(stringResource(R.string.app_picker_empty))
                else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    state.apps.forEachIndexed { index, app ->
                        if (index > 0) HorizontalDivider()
                        TextButton(onClick = { onSelect(app) }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = app.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClear) { Text(stringResource(R.string.app_picker_clear)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}
