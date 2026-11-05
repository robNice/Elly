package de.robnice.elly.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import kotlinx.coroutines.delay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchPage
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.wear.R
import de.robnice.elly.wear.sync.PhoneApp
import de.robnice.elly.wear.ui.components.ActionKey
import de.robnice.elly.wear.ui.components.ColoredKey
import de.robnice.elly.wear.ui.components.EllyPeek
import de.robnice.elly.wear.ui.components.SlotKey
import de.robnice.elly.wear.ui.components.LightRing
import de.robnice.elly.wear.ui.components.PageDots
import de.robnice.elly.wear.ui.components.RoundPage
import de.robnice.elly.wear.ui.components.color
import de.robnice.elly.wear.ui.components.crownVolume
import de.robnice.elly.wear.ui.components.floatResource
import de.robnice.elly.wear.ui.components.rememberLightRingState
import de.robnice.elly.wear.ui.components.ringVolume
import de.robnice.elly.wear.ui.pages.MediaPage
import de.robnice.elly.wear.ui.pages.NavigationPage
import de.robnice.elly.wear.ui.pages.NumbersPage
import de.robnice.elly.wear.ui.pages.PointerPage
import de.robnice.elly.wear.ui.pages.SettingsPage
import de.robnice.elly.wear.ui.pages.VolumeChannelPage

@Composable
fun WearRemoteApp(viewModel: WearRemoteViewModel, onDismissed: () -> Unit) {
    val tvState by viewModel.tvState.collectAsStateWithLifecycle()
    MaterialTheme {
        // No time at the top: it would sit on the TV name of small watches, and the remote is opened only briefly.
        AppScaffold(timeText = {}) {
            SwipeToDismissBox(onDismissed = onDismissed) { isBackground ->
                if (!isBackground) {
                    when (val state = tvState) {
                        WearRemoteViewModel.TvState.Loading -> Unit
                        WearRemoteViewModel.TvState.None -> NoTvScreen(viewModel)
                        WearRemoteViewModel.TvState.Demo -> RemotePages(stringResource(R.string.demo_tv_name), viewModel)
                        is WearRemoteViewModel.TvState.Ready -> RemotePages(state.name, viewModel)
                    }
                }
            }
            PhoneOpenedDialog(viewModel)
        }
    }
}

@Composable
private fun RemotePages(tvName: String, viewModel: WearRemoteViewModel) {
    val onKey: (RemoteKey) -> Unit = { viewModel.send(it) }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val tvs by viewModel.pairedTvs.collectAsStateWithLifecycle()
    val activeTvId by viewModel.activeTvId.collectAsStateWithLifecycle()
    val phonePrompt by viewModel.phonePrompt.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val appIcons by viewModel.appIcons.collectAsStateWithLifecycle()
    val slotKey: SlotKey = { slot, large, modifier ->
        val action = layout.action(slot)
        // OK in the corner of the keypad confirms the number, so it looks like that.
        if (slot == WatchSlot.NUM_RIGHT && action == WatchAction.Key(RemoteKey.OK)) {
            ColoredKey(
                icon = R.drawable.ic_check,
                label = R.string.digits_send,
                container = R.color.send_key,
                content = R.color.on_send_key,
                onClick = { viewModel.run(action) },
                modifier = modifier
            )
        } else {
            ActionKey(
                action = action,
                onClick = { viewModel.run(action) },
                modifier = modifier,
                large = large,
                appIcon = (action as? WatchAction.App)?.let { appIcons[it.appId] }
            )
        }
    }
    val pages = remember(layout) { layout.visiblePages.map(Page::of) + Page.SETTINGS }
    val currentPages by rememberUpdatedState(pages)
    val lightEffect by rememberUpdatedState(preferences.lightEffect)
    val ring = rememberLightRingState()
    var sentCount by remember { mutableIntStateOf(0) }
    val okColor by rememberUpdatedState(connection.color())
    val failColor = colorResource(R.color.status_disconnected)
    LaunchedEffect(Unit) {
        viewModel.sendResults.collect { ok ->
            if (!lightEffect) return@collect
            if (ok) sentCount++
            ring.pulse(if (ok) okColor else failColor)
        }
    }
    // A short wait shows no running arc, only one that keeps the user waiting.
    LaunchedEffect(busy, preferences.lightEffect) {
        if (busy) delay(BUSY_DELAY_MILLIS)
        ring.busy = busy && preferences.lightEffect
    }
    val volumeStep: (Boolean) -> Unit = { louder ->
        viewModel.send(if (louder) RemoteKey.VOLUME_UP else RemoteKey.VOLUME_DOWN, announce = false)
    }
    val pagerState = rememberPagerState(pageCount = { currentPages.size })
    val crownFocus = remember { FocusRequester() }
    // Each page takes the focus when it is shown, so it is taken back for the crown.
    // Pages with a list keep the crown for scrolling.
    LaunchedEffect(pagerState.settledPage) {
        viewModel.pointerShown(pages.getOrNull(pagerState.settledPage) == Page.POINTER)
        if (pages.getOrNull(pagerState.settledPage)?.scrolls != true) crownFocus.requestFocus()
    }
    HorizontalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = { PageDots(pagerState) },
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    viewModel.stopVolume()
                }
            }
            .then(
                // On the pointer page a finger rests on the display while the arm moves; the ring stays out of the way.
                if (preferences.ringGesture && pages.getOrNull(pagerState.currentPage) != Page.POINTER) {
                    Modifier.ringVolume(
                        innerRadius = floatResource(R.dimen.volume_ring_inner_radius),
                        onTrack = { angle ->
                            ring.track(angle)
                            if (angle == null) viewModel.volumeMotionEnded()
                        },
                        onSpeed = viewModel::volumeMotion
                    ) { louder ->
                        ring.step(louder)
                        volumeStep(louder)
                    }
                } else {
                    Modifier
                }
            )
            .crownVolume(crownFocus, viewModel::volumeMotion) { louder ->
                ring.crownStep(louder)
                volumeStep(louder)
            }
    ) {
        HorizontalPager(state = pagerState, rotaryScrollableBehavior = null) { page ->
            when (pages.getOrNull(page) ?: Page.SETTINGS) {
                Page.NAVIGATION -> NavigationPage(tvName, connection, sentCount, onKey, slotKey)
                Page.VOLUME_CHANNEL -> VolumeChannelPage(slotKey)
                Page.NUMBERS -> NumbersPage(onKey, slotKey)
                Page.MEDIA -> MediaPage(slotKey)
                Page.POINTER -> PointerPage(
                    active = pagerState.settledPage == page,
                    onHold = viewModel::holdPointer,
                    onClick = viewModel::clickPointer,
                    onScroll = viewModel::scrollPointer,
                    slotKey = slotKey
                )
                Page.SETTINGS -> SettingsPage(
                    preferences = preferences,
                    tvs = tvs,
                    activeTvId = activeTvId,
                    onHaptics = viewModel::setHaptics,
                    onRingGesture = viewModel::setRingGesture,
                    onLightEffect = viewModel::setLightEffect,
                    onPointerSpeed = viewModel::setPointerSpeed,
                    onPointerInverted = viewModel::setPointerInverted,
                    onSelectTv = viewModel::selectTv,
                    phoneUnreachable = phonePrompt == WearRemoteViewModel.PhonePrompt.UNREACHABLE,
                    onOpenApp = viewModel::openAppOnPhone,
                    onOpenPrivacy = viewModel::openPrivacyOnPhone
                )
            }
        }
    }
    if (preferences.lightEffect) LightRing(ring, connection.color())
    EllyPeek()
    message?.messageRes()?.let { text ->
        ResultMessage(stringResource(text))
        LaunchedEffect(message) {
            delay(MESSAGE_MILLIS)
            viewModel.messageShown()
        }
    }
}

@Composable
private fun ResultMessage(text: String) {
    RoundPage {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .at(0f, 0f)
                .wide(floatResource(R.dimen.message_width))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(
                    horizontal = dimensionResource(R.dimen.message_padding_horizontal),
                    vertical = dimensionResource(R.dimen.message_padding_vertical)
                )
        )
    }
}

private fun SendResult.messageRes(): Int? = when (this) {
    SendResult.UNREACHABLE -> R.string.result_unreachable
    SendResult.PAIRING_REQUIRED -> R.string.result_pairing_required
    SendResult.FAILED -> R.string.result_failed
    SendResult.WAKE_SENT -> R.string.result_wake_sent
    SendResult.SENT, SendResult.DEMO, SendResult.NO_TV, SendResult.UNSUPPORTED -> null
}

// A list, so the install key never ends up cut off at the bottom of small watches or with large fonts.
@Composable
private fun NoTvScreen(viewModel: WearRemoteViewModel) {
    val status by viewModel.phoneAppStatus.collectAsStateWithLifecycle()
    val prompt by viewModel.phonePrompt.collectAsStateWithLifecycle()
    val text = when (status) {
        PhoneApp.Status.CHECKING -> null
        PhoneApp.Status.INSTALLED -> R.string.no_tv
        PhoneApp.Status.MISSING -> R.string.no_tv_install_phone
        PhoneApp.Status.NOT_ANDROID -> R.string.no_tv_needs_android
    }
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.no_tv_spacing), Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Image(
                    painter = painterResource(R.drawable.elly),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.no_tv_image_size))
                )
            }
            text?.let {
                item {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (status == PhoneApp.Status.MISSING) {
                item {
                    Button(
                        onClick = viewModel::installOnPhone,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(stringResource(R.string.install_on_phone), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    )
                }
            }
            if (prompt == WearRemoteViewModel.PhonePrompt.UNREACHABLE) {
                item {
                    Text(
                        text = stringResource(R.string.phone_unreachable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneOpenedDialog(viewModel: WearRemoteViewModel) {
    val prompt by viewModel.phonePrompt.collectAsStateWithLifecycle()
    val dialogText = OpenOnPhoneDialogDefaults.text
    val dialogStyle = OpenOnPhoneDialogDefaults.curvedTextStyle
    OpenOnPhoneDialog(
        visible = prompt == WearRemoteViewModel.PhonePrompt.OPENED,
        onDismissRequest = viewModel::phonePromptShown,
        curvedText = { openOnPhoneDialogCurvedText(text = dialogText, style = dialogStyle) }
    )
}

// The phone decides which remote pages are shown and in which order; the settings always come last.
private enum class Page(val scrolls: Boolean = false) {
    NAVIGATION,
    VOLUME_CHANNEL,
    NUMBERS(scrolls = true),
    MEDIA,

    // Takes the crown for scrolling on the TV.
    POINTER(scrolls = true),
    SETTINGS(scrolls = true);

    companion object {
        fun of(page: WatchPage): Page = when (page) {
            WatchPage.NAVIGATION -> NAVIGATION
            WatchPage.VOLUME_CHANNEL -> VOLUME_CHANNEL
            WatchPage.NUMBERS -> NUMBERS
            WatchPage.MEDIA -> MEDIA
            WatchPage.POINTER -> POINTER
        }
    }
}

private const val MESSAGE_MILLIS = 3_000L
private const val BUSY_DELAY_MILLIS = 300L
