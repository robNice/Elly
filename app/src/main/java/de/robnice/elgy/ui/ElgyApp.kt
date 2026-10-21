package de.robnice.elgy.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.robnice.elgy.AppContainer
import de.robnice.elgy.R
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.ui.pager.RemotePager
import de.robnice.elgy.ui.remote.RemoteViewModel
import de.robnice.elgy.ui.settings.SettingsScreen
import de.robnice.elgy.ui.settings.SettingsViewModel
import de.robnice.elgy.ui.setup.SetupScreen
import de.robnice.elgy.ui.setup.SetupViewModel
import kotlinx.coroutines.flow.combine

private sealed interface RootState {
    data object Loading : RootState
    data object NoTv : RootState
    data object Demo : RootState
    data class Ready(val tv: PairedTv) : RootState
}

private enum class Overlay { SETUP, SETTINGS }

@Composable
fun ElgyApp(container: AppContainer) {
    val rootFlow = remember(container) {
        combine(container.tvRepository.activeTv, container.settingsRepository.demoMode) { tv, demoMode ->
            when {
                demoMode -> RootState.Demo
                tv == null -> RootState.NoTv
                else -> RootState.Ready(tv)
            }
        }
    }
    val root by rootFlow.collectAsStateWithLifecycle(initialValue = RootState.Loading)
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val current = root
        when {
            current == RootState.Loading -> Unit
            overlay == Overlay.SETTINGS -> SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModel.factory(container)),
                onClose = { overlay = null },
                onOpenSetup = { overlay = Overlay.SETUP }
            )
            overlay == Overlay.SETUP || current == RootState.NoTv -> SetupScreen(
                viewModel = viewModel(factory = SetupViewModel.factory(container)),
                onClose = if (current == RootState.NoTv) null else ({ overlay = null }),
                onOpenSettings = { overlay = Overlay.SETTINGS },
                onPaired = { overlay = null }
            )
            else -> RemotePager(
                activeTvName = when (current) {
                    is RootState.Ready -> current.tv.name
                    else -> stringResource(R.string.demo_tv_name)
                },
                viewModel = viewModel(factory = RemoteViewModel.factory(container)),
                onOpenSetup = { overlay = Overlay.SETUP },
                onOpenSettings = { overlay = Overlay.SETTINGS }
            )
        }
    }
}
