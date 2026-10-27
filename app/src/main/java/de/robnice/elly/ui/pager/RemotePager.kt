package de.robnice.elly.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.PointerMode
import de.robnice.elly.remote.ConnectionState
import de.robnice.elly.remote.RemoteCommand
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.TvInput
import de.robnice.elly.sensor.ShakeDetector
import de.robnice.elly.ui.components.LightOverlay
import de.robnice.elly.ui.epg.EpgGuides
import de.robnice.elly.ui.epg.LocalEpgGuides
import de.robnice.elly.ui.components.LocalColorPalette
import de.robnice.elly.ui.components.LocalDpadRounding
import de.robnice.elly.ui.components.LocalKeyHapticsEnabled
import de.robnice.elly.ui.components.LocalKeyRounding
import de.robnice.elly.ui.components.LocalLightEffect
import de.robnice.elly.ui.onboarding.HelpButton
import de.robnice.elly.ui.pointer.PointerOverlay
import de.robnice.elly.ui.remote.AudioTrackDialog
import de.robnice.elly.ui.remote.CustomPage
import de.robnice.elly.ui.remote.LocalAppBackground
import de.robnice.elly.ui.remote.RemoteViewModel
import de.robnice.elly.ui.remote.ScreenBackgroundImage
import de.robnice.elly.ui.remote.TextInputBar
import de.robnice.elly.ui.remote.messageRes
import de.robnice.elly.ui.settings.ScreenThumbnail
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
    val resources = LocalResources.current
    val onKey: (RemoteKey) -> Unit = { key ->
        when (key.command) {
            RemoteCommand.InputPicker -> viewModel.openInputPicker()
            RemoteCommand.AudioTrackPicker -> viewModel.openAudioTracks()
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

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.keepGuideFresh() }
    }

    val guides = remember(viewModel) {
        EpgGuides(
            guide = viewModel::guide,
            refreshing = viewModel.guideRefreshing,
            failed = viewModel.guideFailed,
            currentChannel = viewModel.currentChannel,
            switchChannel = viewModel::switchChannel,
            reservations = viewModel.guideReservations,
            canRecord = viewModel.guideCanRecord,
            perform = viewModel::performGuideAction,
            videoInfo = viewModel::guideVideoInfo,
            openAudioTracks = viewModel::openAudioTracks
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            result.messageRes()?.let { snackbarHostState.showSnackbar(resources.getString(it)) }
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
                        StatusLight(connectionState, viewModel.commandSent)
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
                        overscrollEffect = if (lightEffect) null else rememberOverscrollEffect(),
                        // A page that is only built when it slides in gets its blurred shadows a moment
                        // after it has settled, so the neighbours are kept ready.
                        beyondViewportPageCount = if (lightEffect) 1 else 0
                    ) { index ->
                        CompositionLocalProvider(
                            LocalKeyHapticsEnabled provides keyHaptics,
                            LocalKeyRounding provides keyRounding,
                            LocalDpadRounding provides dpadRounding,
                            LocalEpgGuides provides guides
                        ) {
                            // The shadow layer reaches far beyond its page and would end in a hard edge on the neighbour.
                            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
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
                if (screens.size > 1) {
                    CompositionLocalProvider(
                        LocalAppBackground provides background,
                        LocalKeyRounding provides keyRounding,
                        LocalDpadRounding provides dpadRounding
                    ) {
                        PageIndicator(
                            pagerState = pagerState,
                            previews = screens.map { screenContents[it] ?: DefaultScreens.layout(it) }
                        )
                    }
                }
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

    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    audioTracks?.let { picker ->
        AudioTrackDialog(
            picker = picker,
            onSelect = viewModel::selectAudioTrack,
            onDescriptionChange = viewModel::setAudioDescription,
            onDismiss = viewModel::dismissAudioTracks
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
private fun PageIndicator(pagerState: PagerState, previews: List<CustomScreen>) {
    val description = stringResource(
        R.string.page_indicator_description,
        pagerState.currentPage + 1,
        pagerState.pageCount
    )
    // Elements filling the whole page, like a large program guide, leave no room to swipe, so the
    // previews take swipes as well.
    val scope = rememberCoroutineScope()
    val flingVelocity = with(LocalDensity.current) { dimensionResource(R.dimen.indicator_fling_velocity).toPx() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> pagerState.dispatchRawDelta(-delta) },
                onDragStopped = { velocity ->
                    val target = when {
                        velocity < -flingVelocity -> pagerState.currentPage + 1
                        velocity > flingVelocity -> pagerState.currentPage - 1
                        else -> pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt()
                    }.coerceIn(0, pagerState.pageCount - 1)
                    scope.launch { pagerState.animateScrollToPage(target) }
                }
            )
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center
    ) {
        previews.forEachIndexed { index, preview ->
            val current = index == pagerState.currentPage
            ScreenThumbnail(
                layout = preview,
                enabled = true,
                corner = dimensionResource(R.dimen.indicator_preview_corner),
                padding = dimensionResource(R.dimen.indicator_preview_inner_padding),
                border = colorResource(if (current) R.color.indicator_active else R.color.indicator_inactive),
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.page_indicator_jump, index + 1)) {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                    .padding(
                        horizontal = dimensionResource(R.dimen.indicator_preview_padding_horizontal),
                        vertical = dimensionResource(R.dimen.indicator_preview_padding_vertical)
                    )
                    .height(dimensionResource(R.dimen.indicator_preview_height))
                    .alpha(if (current) 1f else INDICATOR_INACTIVE_ALPHA)
            )
        }
    }
}

@Composable
private fun StatusLight(state: ConnectionState, sent: Flow<Unit>) {
    val (colorRes, descriptionRes) = when (state) {
        ConnectionState.CONNECTED -> R.color.status_connected to R.string.status_connected
        ConnectionState.DISCONNECTED -> R.color.status_disconnected to R.string.status_disconnected
        ConnectionState.CHECKING -> R.color.status_checking to R.string.status_checking
        ConnectionState.DEMO -> R.color.status_demo to R.string.status_demo
    }
    val color = colorResource(colorRes)
    val description = stringResource(descriptionRes)
    // While the connection is being checked the light breathes gently, so a grey dot does not look broken.
    val pulse = if (state == ConnectionState.CHECKING) {
        val transition = rememberInfiniteTransition(label = "statusPulse")
        transition.animateFloat(
            initialValue = STATUS_PULSE_LOW,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(STATUS_PULSE_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "statusPulse"
        ).value
    } else {
        1f
    }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(sent) {
        sent.collectLatest {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(STATUS_FLASH_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.status_light_size))
            .semantics { contentDescription = description }
            .drawBehind {
                val glow = flash.value
                if (glow > 0f) drawCircle(color.copy(alpha = STATUS_FLASH_ALPHA * glow), radius = size.minDimension / 2 * (1f + STATUS_FLASH_GROWTH * glow))
                drawCircle(color.copy(alpha = STATUS_GLOW_ALPHA * pulse), radius = size.minDimension / 2 * pulse)
                drawCircle(color.copy(alpha = STATUS_CORE_LOW_ALPHA + (1f - STATUS_CORE_LOW_ALPHA) * pulse), radius = size.minDimension * STATUS_CORE_RATIO)
            }
    )
}

private const val INDICATOR_INACTIVE_ALPHA = 0.5f
private const val STATUS_GLOW_ALPHA = 0.3f
private const val STATUS_CORE_RATIO = 0.3f
private const val STATUS_PULSE_LOW = 0.45f
private const val STATUS_PULSE_MILLIS = 900
private const val STATUS_FLASH_MILLIS = 700
private const val STATUS_FLASH_ALPHA = 0.6f
private const val STATUS_FLASH_GROWTH = 1.2f
private const val STATUS_CORE_LOW_ALPHA = 0.55f
