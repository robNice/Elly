package de.robnice.elly.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.robnice.elly.AppContainer
import de.robnice.elly.R
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.ui.help.HelpScreen
import de.robnice.elly.ui.onboarding.OnboardingScreen
import de.robnice.elly.ui.pager.RemotePager
import de.robnice.elly.ui.remote.RemoteViewModel
import de.robnice.elly.ui.settings.PrivacyScreen
import de.robnice.elly.ui.settings.SettingsScreen
import de.robnice.elly.ui.settings.SettingsViewModel
import de.robnice.elly.ui.setup.SetupViewModel
import de.robnice.elly.ui.theme.EllyWarmTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private sealed interface RootState {
    data object Loading : RootState
    data object Onboarding : RootState
    data object NoTv : RootState
    data object Demo : RootState
    data class Ready(val tv: PairedTv) : RootState
}

private enum class Overlay { SETTINGS, SETTINGS_TV }

@Composable
fun EllyApp(container: AppContainer) {
    val rootFlow = remember(container) {
        combine(
            container.tvRepository.activeTv,
            container.settingsRepository.demoMode,
            container.settingsRepository.onboardingDone
        ) { tv, demoMode, onboardingDone ->
            when {
                !onboardingDone -> RootState.Onboarding
                demoMode -> RootState.Demo
                tv == null -> RootState.NoTv
                else -> RootState.Ready(tv)
            }
        }
    }
    val root by rootFlow.collectAsStateWithLifecycle(initialValue = RootState.Loading)
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    var tourOpen by rememberSaveable { mutableStateOf(false) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Without a TV the settings open on the TV tab; as an overlay they stay open after pairing
    // so the success dialog can still be shown.
    LaunchedEffect(root) {
        if (root == RootState.NoTv) overlay = Overlay.SETTINGS_TV
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val current = root
        when {
            current == RootState.Loading -> Unit
            current == RootState.Onboarding -> OnboardingScreen(
                onFinish = { scope.launch { container.settingsRepository.setOnboardingDone() } }
            )
            tourOpen -> OnboardingScreen(onFinish = { tourOpen = false })
            privacyOpen -> EllyWarmTheme { PrivacyScreen(onClose = { privacyOpen = false }) }
            helpOpen -> EllyWarmTheme {
                HelpScreen(onClose = { helpOpen = false }, onStartTour = { tourOpen = true }, onOpenPrivacy = { privacyOpen = true })
            }
            overlay != null || current == RootState.NoTv -> EllyWarmTheme {
                SettingsScreen(
                    viewModel = viewModel(factory = SettingsViewModel.factory(container)),
                    setupViewModel = viewModel(factory = SetupViewModel.factory(container)),
                    onClose = if (current == RootState.NoTv) null else ({ overlay = null }),
                    onOpenHelp = { helpOpen = true },
                    onPaired = { overlay = null },
                    openTvTab = overlay == Overlay.SETTINGS_TV
                )
            }
            else -> RemotePager(
                activeTvName = when (current) {
                    is RootState.Ready -> current.tv.name
                    else -> stringResource(R.string.demo_tv_name)
                },
                viewModel = viewModel(factory = RemoteViewModel.factory(container)),
                onOpenSetup = { overlay = Overlay.SETTINGS_TV },
                onOpenSettings = { overlay = Overlay.SETTINGS },
                onOpenHelp = { helpOpen = true }
            )
        }
    }
}
