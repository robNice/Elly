package de.robnice.elgy.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.robnice.elgy.R
import de.robnice.elgy.remote.RemoteCommand
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.remote.SendResult
import de.robnice.elgy.remote.TvInput
import de.robnice.elgy.ui.components.LocalKeyHapticsEnabled
import de.robnice.elgy.ui.pointer.PointerOverlay
import de.robnice.elgy.ui.remote.RemotePageContent
import de.robnice.elgy.ui.remote.RemoteViewModel
import de.robnice.elgy.ui.remote.messageRes
import de.robnice.elgy.ui.settings.SettingsButton

@Composable
fun RemotePager(
    activeTvName: String,
    viewModel: RemoteViewModel,
    onOpenSetup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layout by viewModel.screenLayout.collectAsStateWithLifecycle()
    val screens = layout.enabledScreens
    val pages = screens.map { RemotePage.valueOf(it.name) }
    val pagerState = rememberPagerState(
        initialPage = screens.indexOf(viewModel.currentScreen).coerceAtLeast(0),
        pageCount = { pages.size }
    )
    val pointerActive by viewModel.pointerActive.collectAsStateWithLifecycle()
    val pointerSettings by viewModel.pointerSettings.collectAsStateWithLifecycle()
    val keyHaptics by viewModel.keyHaptics.collectAsStateWithLifecycle()
    val inputs by viewModel.inputs.collectAsStateWithLifecycle()
    val appSlots by viewModel.appSlots.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val onKey: (RemoteKey) -> Unit = { key ->
        if (key.command == RemoteCommand.InputPicker) {
            viewModel.openInputPicker()
        } else {
            viewModel.press(key)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.currentScreen = screens.getOrNull(it) }
    }

    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            result.messageRes()?.let { snackbarHostState.showSnackbar(context.getString(it)) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenSetup, modifier = Modifier.weight(1f)) {
                    Text(text = activeTvName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = viewModel::openPointer) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pointer),
                        contentDescription = stringResource(R.string.key_pointer),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size))
                    )
                }
                SettingsButton(onClick = onOpenSettings)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                    CompositionLocalProvider(LocalKeyHapticsEnabled provides keyHaptics) {
                        RemotePageContent(
                            page = pages[index],
                            onKey = onKey,
                            appSlots = appSlots,
                            onLaunchApp = viewModel::launchApp
                        )
                    }
                }
                if (pointerActive) {
                    CompositionLocalProvider(LocalKeyHapticsEnabled provides keyHaptics) {
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
            if (pages.size > 1) PageIndicator(pagerState = pagerState)
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
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
        repeat(pagerState.pageCount) { index ->
            val color = if (index == pagerState.currentPage) {
                colorResource(R.color.indicator_active)
            } else {
                colorResource(R.color.indicator_inactive)
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.indicator_dot_size))
                    .background(color, CircleShape)
            )
        }
    }
}
