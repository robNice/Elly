package de.robnice.elgy.ui.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elgy.BuildConfig
import de.robnice.elgy.R
import de.robnice.elgy.data.model.ColorPalette
import de.robnice.elgy.data.model.KeyRounding
import de.robnice.elgy.remote.TvApp
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.LocalColorPalette
import de.robnice.elgy.ui.components.LocalDpadRounding
import de.robnice.elgy.ui.components.LocalKeyRounding
import de.robnice.elgy.ui.components.LocalLightEffect
import de.robnice.elgy.ui.components.colors
import de.robnice.elgy.ui.components.keyBody
import de.robnice.elgy.ui.components.keyShape
import de.robnice.elgy.ui.components.palette
import de.robnice.elgy.ui.remote.LocalAppBackground
import de.robnice.elgy.ui.remote.messageRes
import de.robnice.elgy.data.model.CustomScreen
import de.robnice.elgy.data.model.MotionTuning
import de.robnice.elgy.data.model.JoystickPlacement
import de.robnice.elgy.data.model.OkButtonPosition
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.data.model.PointerMode
import de.robnice.elgy.data.model.PointerSettings
import de.robnice.elgy.data.model.RemoteScreen
import de.robnice.elgy.data.model.TouchStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private enum class SettingsTab(@StringRes val title: Int) {
    TV(R.string.settings_tab_tv),
    SCREENS(R.string.settings_tab_screens),
    CONTROL(R.string.settings_tab_control)
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onClose: () -> Unit, onOpenSetup: () -> Unit) {
    val demoMode by viewModel.demoMode.collectAsStateWithLifecycle()
    val keyHaptics by viewModel.keyHaptics.collectAsStateWithLifecycle()
    val keyRounding by viewModel.keyRounding.collectAsStateWithLifecycle()
    val dpadRounding by viewModel.dpadRounding.collectAsStateWithLifecycle()
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val background by viewModel.background.collectAsStateWithLifecycle()
    val lightEffect by viewModel.lightEffect.collectAsStateWithLifecycle()
    val screensAvailable by viewModel.screensAvailable.collectAsStateWithLifecycle()
    val screenLayout by viewModel.screenLayout.collectAsStateWithLifecycle()
    val activeTv by viewModel.activeTv.collectAsStateWithLifecycle()
    val pointerSettings by viewModel.pointerSettings.collectAsStateWithLifecycle()
    var confirmDisconnect by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = viewModel.lastTab) { SettingsTab.entries.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.lastTab = it }
    }
    var editing by rememberSaveable { mutableStateOf<RemoteScreen?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf<RemoteScreen?>(null) }
    val screens by viewModel.screens.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()

    BackHandler(onBack = onClose)

    if (showPrivacy) {
        PrivacyScreen(onClose = { showPrivacy = false })
        return
    }

    editing?.let { target ->
        CompositionLocalProvider(
            LocalKeyRounding provides keyRounding,
            LocalDpadRounding provides dpadRounding,
            LocalColorPalette provides colorPalette,
            LocalAppBackground provides background,
            LocalLightEffect provides lightEffect
        ) {
            CustomScreenEditor(
                screen = screens[target] ?: CustomScreen.EMPTY,
                onAdd = { type, key -> viewModel.addElement(target, type, key) },
                onAddGroup = { viewModel.addGroup(target, it) },
                pickerTab = viewModel.lastPickerTab,
                onPickerTab = { viewModel.lastPickerTab = it },
                onMove = { id, x, y, cells -> viewModel.moveElement(target, id, x, y, cells) },
                onResize = { id, width, height, cells -> viewModel.resizeElement(target, id, width, height, cells) },
                onRemove = { id -> viewModel.removeElement(target, id) },
                onMoveAll = { ids, dx, dy, cells -> viewModel.moveElements(target, ids, dx, dy, cells) },
                onRemoveAll = { ids -> viewModel.removeElements(target, ids) },
                onConfigureApp = { id -> viewModel.openAppConfig(target, id) },
                onReset = { viewModel.resetScreen(target) },
                canUndo = canUndo,
                onUndo = { viewModel.undo(target) },
                onClose = {
                    viewModel.clearHistory()
                    editing = null
                }
            )
            AppDialogs(viewModel)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.setup_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.setup_close)) }
        }
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            SettingsTab.entries.forEach { entry ->
                Tab(
                    selected = pagerState.currentPage == entry.ordinal,
                    onClick = { scope.launch { pagerState.animateScrollToPage(entry.ordinal) } },
                    text = { Text(stringResource(entry.title)) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.setup_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_section_spacing))
            ) {
                when (SettingsTab.entries[page]) {
                    SettingsTab.TV -> {
                        TvSection(
                            activeTv = activeTv,
                            onOpenSetup = onOpenSetup,
                            onDisconnect = { confirmDisconnect = true }
                        )
                        SwitchSetting(
                            title = stringResource(R.string.settings_demo_mode),
                            description = stringResource(R.string.settings_demo_mode_description),
                            checked = demoMode,
                            onCheckedChange = viewModel::setDemoMode
                        )
                    }
                    SettingsTab.SCREENS -> {
                        if (screensAvailable) {
                            CompositionLocalProvider(
            LocalKeyRounding provides keyRounding,
            LocalDpadRounding provides dpadRounding,
            LocalColorPalette provides colorPalette,
            LocalAppBackground provides background,
            LocalLightEffect provides lightEffect
        ) {
                                ScreensSection(
                                    layout = screenLayout,
                                    screens = screens,
                                    onToggle = viewModel::toggleScreen,
                                    onReorder = viewModel::reorderScreens,
                                    onEdit = {
                                        viewModel.clearHistory()
                                        editing = it
                                    },
                                    onReset = { confirmReset = it }
                                )
                            }
                        }
                        PaletteSection(selected = colorPalette, onSelect = viewModel::setColorPalette)
                        CompositionLocalProvider(LocalColorPalette provides colorPalette) {
                            BackgroundSection(
                                current = background,
                                onSelect = viewModel::setBackground,
                                onImage = viewModel::setBackgroundImage
                            )
                        }
                        RoundingSection(
                            keyRounding = keyRounding,
                            dpadRounding = dpadRounding,
                            onKeyRoundingChange = viewModel::setKeyRounding,
                            onDpadRoundingChange = viewModel::setDpadRounding
                        )
                        if (viewModel.lightEffectAvailable) {
                            SwitchSetting(
                                title = stringResource(R.string.settings_light_effect),
                                description = stringResource(R.string.settings_light_effect_description),
                                checked = lightEffect,
                                onCheckedChange = viewModel::setLightEffect
                            )
                        }
                    }
                    SettingsTab.CONTROL -> {
                        PointerSection(
                            settings = pointerSettings,
                            motionAvailable = viewModel.motionPointerAvailable,
                            onModeChange = viewModel::setPointerMode,
                            onOkPositionChange = viewModel::setOkButtonPosition,
                            onTouchStyleChange = viewModel::setTouchStyle,
                            onJoystickPlacementChange = viewModel::setJoystickPlacement,
                            onSensitivityChange = viewModel::setMotionSensitivity,
                            onAccelerationChange = viewModel::setMotionAcceleration,
                            onSwipeSpeedChange = viewModel::setSwipeSpeed,
                            onShakeChange = viewModel::setShakeToOpen,
                            onShakeSensitivityChange = viewModel::setShakeSensitivity,
                            onDoubleTapChange = viewModel::setDoubleTapToOpen
                        )
                        SwitchSetting(
                            title = stringResource(R.string.settings_key_haptics),
                            description = stringResource(R.string.settings_key_haptics_description),
                            checked = keyHaptics,
                            onCheckedChange = viewModel::setKeyHaptics
                        )
                    }
                }
            }
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

    confirmReset?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            title = { Text(stringResource(R.string.settings_screen_reset_title)) },
            text = { Text(stringResource(R.string.settings_screen_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = null
                    viewModel.resetScreen(target)
                }) { Text(stringResource(R.string.settings_screen_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = null }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
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
private fun PaletteSection(selected: ColorPalette, onSelect: (ColorPalette) -> Unit) {
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(text = stringResource(R.string.settings_palette), style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                ColorPalette.entries.chunked(PALETTE_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEach { palette ->
                            PaletteOption(
                                palette = palette,
                                selected = palette == selected,
                                onClick = { onSelect(palette) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// A bar split into the key color and the light color of the palette.
@Composable
private fun PaletteOption(palette: ColorPalette, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.settings_palette_corner))
    val colors = palette.colors()
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                dimensionResource(if (selected) R.dimen.custom_editor_selected_outline else R.dimen.custom_editor_outline),
                if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.setup_item_spacing)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.settings_palette_label_spacing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.settings_palette_bar_height))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.settings_palette_bar_corner)))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(colors.keyTop, colors.keyBottom)))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(colors.lightTop, colors.lightBottom)))
            )
        }
        FittedLabel(text = stringResource(palette.title()), color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun ColorPalette.title(): Int = when (this) {
    ColorPalette.GRAPHITE -> R.string.palette_graphite
    ColorPalette.SILVER -> R.string.palette_silver
    ColorPalette.OCEAN -> R.string.palette_ocean
    ColorPalette.FOREST -> R.string.palette_forest
    ColorPalette.COPPER -> R.string.palette_copper
    ColorPalette.VIOLET -> R.string.palette_violet
    ColorPalette.PETROL -> R.string.palette_petrol
    ColorPalette.BORDEAUX -> R.string.palette_bordeaux
    ColorPalette.SAND -> R.string.palette_sand
    ColorPalette.CORAL -> R.string.palette_coral
    ColorPalette.MIDNIGHT -> R.string.palette_midnight
    ColorPalette.SAGE -> R.string.palette_sage
    ColorPalette.RUBY -> R.string.palette_ruby
    ColorPalette.LAGOON -> R.string.palette_lagoon
    ColorPalette.AMBER -> R.string.palette_amber
}

private const val PALETTE_COLUMNS = 3

@Composable
private fun RoundingSection(
    keyRounding: Float,
    dpadRounding: Float,
    onKeyRoundingChange: (Float) -> Unit,
    onDpadRoundingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))) {
        Text(text = stringResource(R.string.settings_rounding), style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.setup_item_spacing))) {
                RoundingSetting(stringResource(R.string.settings_rounding_keys), keyRounding, onKeyRoundingChange)
                RoundingSetting(stringResource(R.string.settings_rounding_dpad), dpadRounding, onDpadRoundingChange)
            }
        }
    }
}

@Composable
private fun RoundingSetting(title: String, rounding: Float, onRoundingChange: (Float) -> Unit) {
    var current by remember(rounding) { mutableFloatStateOf(rounding) }
    Row(
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.setup_card_padding),
            vertical = dimensionResource(R.dimen.setup_item_spacing)
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.settings_rounding_value, (current * 100).roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = current,
                onValueChange = { current = it },
                onValueChangeFinished = { onRoundingChange(current) },
                valueRange = KeyRounding.RANGE
            )
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_card_padding)))
        RoundingPreview(current)
    }
}

// Shows the top left corner of a large key, so the radius is easy to judge.
@Composable
private fun RoundingPreview(rounding: Float) {
    val inset = dimensionResource(R.dimen.settings_rounding_preview_inset)
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.settings_rounding_preview_size))
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(inset, inset)
                .requiredSize(dimensionResource(R.dimen.settings_rounding_preview_key))
                .keyBody(keyShape(rounding), KeyStyle.DARK.palette(), pressed = false)
        )
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
    onAccelerationChange: (Float) -> Unit,
    onSwipeSpeedChange: (Float) -> Unit,
    onShakeChange: (Boolean) -> Unit,
    onShakeSensitivityChange: (Float) -> Unit,
    onDoubleTapChange: (Boolean) -> Unit
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
                        SubSwitch(
                            title = stringResource(R.string.settings_shake_to_open),
                            description = stringResource(R.string.settings_shake_to_open_description),
                            checked = settings.shakeToOpen,
                            onCheckedChange = onShakeChange
                        )
                        if (settings.shakeToOpen) {
                            Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.settings_sub_option_indent))) {
                                SliderSetting(
                                    title = stringResource(R.string.settings_shake_sensitivity),
                                    value = settings.shakeSensitivity,
                                    range = MotionTuning.SHAKE_SENSITIVITY_RANGE,
                                    onValueChange = onShakeSensitivityChange
                                )
                            }
                        }
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
                        SubSwitch(
                            title = stringResource(R.string.settings_double_tap_to_open),
                            description = stringResource(R.string.settings_double_tap_to_open_description),
                            checked = settings.doubleTapToOpen,
                            onCheckedChange = onDoubleTapChange
                        )
                        SettingCaption(stringResource(R.string.settings_touch_style))
                        ChoiceRow(
                            title = stringResource(R.string.settings_touch_style_swipe),
                            description = stringResource(R.string.settings_touch_style_swipe_description),
                            selected = settings.touchStyle == TouchStyle.SWIPE,
                            onSelect = { onTouchStyleChange(TouchStyle.SWIPE) }
                        )
                        if (settings.touchStyle == TouchStyle.SWIPE) {
                            Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.settings_sub_option_indent))) {
                                SliderSetting(
                                    title = stringResource(R.string.settings_swipe_speed),
                                    value = settings.swipeSpeed,
                                    range = MotionTuning.SWIPE_SPEED_RANGE,
                                    onValueChange = onSwipeSpeedChange
                                )
                            }
                        }
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

// A switch inside a card, aligned with the other sub-options.
@Composable
private fun SubSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(
                horizontal = dimensionResource(R.dimen.setup_card_padding),
                vertical = dimensionResource(R.dimen.setup_item_spacing)
            ),
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
private fun AppDialogs(viewModel: SettingsViewModel) {
    val appDraft by viewModel.appDraft.collectAsStateWithLifecycle()
    val appPicker by viewModel.appPicker.collectAsStateWithLifecycle()
    val styleEditor by viewModel.styleEditor.collectAsStateWithLifecycle()
    val draft = appDraft ?: return

    CustomAppDialog(
        app = draft.app,
        onChooseApp = viewModel::openAppPicker,
        onStyle = viewModel::openStyleEditor,
        onConfirm = viewModel::confirmApp,
        onDismiss = viewModel::dismissApp
    )

    appPicker?.let { picker ->
        AppPickerDialog(
            state = picker,
            onSelect = viewModel::assignApp,
            onClear = viewModel::clearApp,
            onDismiss = viewModel::dismissAppPicker
        )
    }

    val slot = draft.app
    if (styleEditor && slot != null) {
        AppSlotStyleDialog(
            title = stringResource(R.string.custom_app_style_title),
            slot = slot,
            onSave = viewModel::saveStyle,
            onDismiss = viewModel::dismissStyleEditor,
            confirmLabel = R.string.dialog_ok
        )
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
        title = { Text(stringResource(R.string.custom_app_picker_title)) },
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
