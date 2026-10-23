package de.robnice.elly.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.robnice.elly.R
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.remote.RemoteCommand
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.TvInput
import de.robnice.elly.sensor.ShakeDetector
import de.robnice.elly.ui.components.LightOverlay
import de.robnice.elly.ui.components.LocalColorPalette
import de.robnice.elly.ui.components.LocalDpadRounding
import de.robnice.elly.ui.components.LocalKeyHapticsEnabled
import de.robnice.elly.ui.components.LocalKeyRounding
import de.robnice.elly.ui.components.LocalLightEffect
import de.robnice.elly.ui.onboarding.HelpButton
import de.robnice.elly.ui.pointer.PointerOverlay
import de.robnice.elly.ui.remote.CustomPage
import de.robnice.elly.ui.remote.RemoteViewModel
import de.robnice.elly.ui.remote.ScreenBackgroundImage
import de.robnice.elly.ui.remote.TextInputBar
import de.robnice.elly.ui.remote.messageRes
import de.robnice.elly.ui.settings.SettingsButton

@Composable
fun RemotePager(
    activeTvName: String,
    viewModel: RemoteViewModel,
    onOpenSetup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layout by viewModel.screenLayout.collectAsStateWithLifecycle()
    val screens = layout.enabledScreens
    val pagerState = rememberPagerState(
        initialPage = screens.indexOf(viewModel.currentScreen).coerceAtLeast(0),
        pageCount = { screens.size }
    )
    val pointerActive by viewModel.pointerActive.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val pointerSettings by viewModel.pointerSettings.collectAsStateWithLifecycle()
    val keyHaptics by viewModel.keyHaptics.collectAsStateWithLifecycle()
    val keyRounding by viewModel.keyRounding.collectAsStateWithLifecycle()
    val dpadRounding by viewModel.dpadRounding.collectAsStateWithLifecycle()
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val background by viewModel.background.collectAsStateWithLifecycle()
    val lightEffect by viewModel.lightEffect.collectAsStateWithLifecycle()
    val inputs by viewModel.inputs.collectAsStateWithLifecycle()
    val textInputOpen by viewModel.textInputOpen.collectAsStateWithLifecycle()
    val screenContents by viewModel.screens.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val onKey: (RemoteKey) -> Unit = { key ->
        when (key.command) {
            RemoteCommand.InputPicker -> viewModel.openInputPicker()
            RemoteCommand.TextInput -> viewModel.openKeyboard()
            else -> viewModel.press(key)
        }
    }

    // Shaking opens the motion pointer; the sensor only runs while the remote is visible. The
    // detector is kept across opening and closing, so its pause after a shake still applies.
    val shakeDetector = remember(context) { ShakeDetector(context) { viewModel.openPointer() } }
    shakeDetector.sensitivity = pointerSettings.shakeSensitivity
    if (pointerSettings.mode == PointerMode.MOTION && pointerSettings.shakeToOpen && !pointerActive) {
        val detector = shakeDetector
        LifecycleStartEffect(detector) {
            detector.start()
            onStopOrDispose { detector.stop() }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.currentScreen = screens.getOrNull(it) }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.monitorConnection() }
    }

    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            result.messageRes()?.let { snackbarHostState.showSnackbar(context.getString(it)) }
        }
    }

    CompositionLocalProvider(LocalColorPalette provides colorPalette, LocalLightEffect provides lightEffect) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScreenBackgroundImage(background.type, background.image)
            LightOverlay()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.top_bar_background))
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onOpenSetup, modifier = Modifier.weight(1f)) {
                        StatusLight(connectionState)
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.status_light_spacing)))
                        Text(text = activeTvName, color = colorResource(R.color.top_bar_content))
                    }
                    IconButton(onClick = viewModel::openPointer) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pointer),
                            contentDescription = stringResource(R.string.key_pointer),
                            tint = if (pointerActive) colorResource(R.color.top_bar_icon_active)
                            else colorResource(R.color.top_bar_content),
                            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size))
                        )
                    }
                    SettingsButton(onClick = onOpenSettings, tint = colorResource(R.color.top_bar_content))
                    HelpButton(onClick = onOpenHelp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // The stretch at the ends draws the page into its own layer, where the soft-light
                    // shadows would have nothing to blend with and turn black.
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        overscrollEffect = if (lightEffect) null else rememberOverscrollEffect()
                    ) { index ->
                        CompositionLocalProvider(
                            LocalKeyHapticsEnabled provides keyHaptics,
                            LocalKeyRounding provides keyRounding,
                            LocalDpadRounding provides dpadRounding
                        ) {
                            CustomPage(
                                screen = screenContents[screens[index]] ?: DefaultScreens.layout(screens[index]),
                                onKey = onKey,
                                onLaunchApp = viewModel::launchApp,
                                onDoubleTap = viewModel::openPointer.takeIf {
                                    pointerSettings.mode == PointerMode.TOUCH && pointerSettings.doubleTapToOpen && !pointerActive
                                }
                            )
                        }
                    }
                    if (pointerActive) {
                        CompositionLocalProvider(LocalKeyHapticsEnabled provides keyHaptics, LocalKeyRounding provides keyRounding) {
                            PointerOverlay(
                            settings = pointerSettings,
                            onMove = viewModel::movePointer,
                            onScroll = viewModel::scrollPointer,
                            onOk = viewModel::clickPointer,
                                onClose = viewModel::closePointer
                            )
                        }
                    }
                }
                if (screens.size > 1) PageIndicator(pagerState = pagerState)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
            )
            if (textInputOpen) {
                TextInputBar(
                    initialText = viewModel.initialText,
                    onTextChange = viewModel::changeText,
                    onEnter = viewModel::sendEnter,
                    onClose = viewModel::closeKeyboard,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .navigationBarsPadding()
                )
            }
        }
    }

    inputs?.let { list ->
        InputPickerDialog(
            inputs = list,
            onSelect = viewModel::selectInput,
            onDismiss = viewModel::dismissInputPicker
        )
    }
}

@Composable
private fun InputPickerDialog(inputs: List<TvInput>, onSelect: (TvInput) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.input_picker_title)) },
        text = {
            if (inputs.isEmpty()) {
                Text(stringResource(R.string.input_picker_empty))
            } else {
                Column {
                    inputs.forEach { input ->
                        TextButton(onClick = { onSelect(input) }, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = input.label, style = MaterialTheme.typography.bodyLarge)
                                if (!input.connected) {
                                    Text(
                                        text = stringResource(R.string.input_picker_not_connected),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }
    )
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
    val description = stringResource(
        R.string.page_indicator_description,
        pagerState.currentPage + 1,
        pagerState.pageCount
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.indicator_padding))
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.indicator_dot_spacing),
            Alignment.CenterHorizontally
        )
    ) {
        val halo = colorResource(R.color.indicator_halo)
        repeat(pagerState.pageCount) { index ->
            val color = if (index == pagerState.currentPage) {
                colorResource(R.color.indicator_active)
            } else {
                colorResource(R.color.indicator_inactive)
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.indicator_dot_size))
                    .drawBehind {
                        val radius = size.minDimension / 2 * INDICATOR_HALO_RATIO
                        drawCircle(Brush.radialGradient(listOf(halo, Color.Transparent), center, radius), radius)
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun StatusLight(state: ConnectionState) {
    val (colorRes, descriptionRes) = when (state) {
        ConnectionState.CONNECTED -> R.color.status_connected to R.string.status_connected
        ConnectionState.DISCONNECTED -> R.color.status_disconnected to R.string.status_disconnected
        ConnectionState.CHECKING -> R.color.status_checking to R.string.status_checking
        ConnectionState.DEMO -> R.color.status_demo to R.string.status_demo
    }
    val color = colorResource(colorRes)
    val description = stringResource(descriptionRes)
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.status_light_size))
            .semantics { contentDescription = description }
            .drawBehind {
                drawCircle(color.copy(alpha = STATUS_GLOW_ALPHA))
                drawCircle(color, radius = size.minDimension * STATUS_CORE_RATIO)
            }
    )
}

private const val INDICATOR_HALO_RATIO = 2.2f
private const val STATUS_GLOW_ALPHA = 0.3f
private const val STATUS_CORE_RATIO = 0.3f
