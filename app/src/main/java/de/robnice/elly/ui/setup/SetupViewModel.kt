package de.robnice.elly.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.AppContainer
import de.robnice.elly.data.AppSlotsRepository
import de.robnice.elly.data.ScreenLayoutRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.platform.DevicePlatform
import de.robnice.elly.tv.connection.TvConnectionException
import de.robnice.elly.tv.discovery.DiscoveredTv
import de.robnice.elly.tv.discovery.TvDiscovery
import de.robnice.elly.tv.pairing.PairingService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    private val repository: TvRepository,
    private val appSlotsRepository: AppSlotsRepository,
    private val screenLayoutRepository: ScreenLayoutRepository,
    private val discovery: TvDiscovery,
    private val pairingService: PairingService,
    showEmulatorHint: Boolean
) : ViewModel() {

    private val state = MutableStateFlow(SetupUiState(showEmulatorHint = showEmulatorHint))
    private var scanJob: Job? = null
    private var pairingJob: Job? = null

    val uiState: StateFlow<SetupUiState> = combine(
        state,
        repository.pairedTvs,
        repository.activeTvId
    ) { current, pairedTvs, activeTvId ->
        current.copy(pairedTvs = pairedTvs, activeTvId = activeTvId ?: pairedTvs.firstOrNull()?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), state.value)

    fun scan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            state.update { it.copy(scanState = ScanState.SCANNING, foundTvs = emptyList()) }
            val found = try {
                discovery.discover()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            state.update { it.copy(scanState = ScanState.DONE, foundTvs = found) }
        }
    }

    fun scanIfUnpaired() {
        if (state.value.scanState != ScanState.IDLE) return
        viewModelScope.launch {
            if (repository.pairedTvs.first().isEmpty()) scan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        state.update { if (it.scanState == ScanState.SCANNING) it.copy(scanState = ScanState.IDLE) else it }
    }

    fun updateManualIp(value: String) {
        state.update { it.copy(manualIp = value, manualIpInvalid = false) }
    }

    fun connectManualIp() {
        val ip = state.value.manualIp.trim()
        if (!IpAddressValidator.isValidIpv4(ip)) {
            state.update { it.copy(manualIpInvalid = true) }
            return
        }
        val known = state.value.foundTvs.firstOrNull { it.ipAddress == ip }
        pair(known ?: DiscoveredTv(ipAddress = ip, name = null, modelName = null))
    }

    fun pair(tv: DiscoveredTv) {
        if (pairingJob?.isActive == true) return
        pairingJob = viewModelScope.launch {
            state.update { it.copy(pairing = PairingState.Connecting(tv.ipAddress)) }
            val result = try {
                val existing = repository.findByIp(tv.ipAddress)
                val paired = pairingService.pair(tv, existing) {
                    state.update { it.copy(pairing = PairingState.AwaitingConfirmation(tv.ipAddress)) }
                }
                repository.saveAndActivate(paired)
                PairingState.Paired(paired)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PairingState.Failed(e.toPairingError())
            }
            state.update { it.copy(pairing = result) }
        }
    }

    fun cancelPairing() {
        pairingJob?.cancel()
        state.update { it.copy(pairing = PairingState.Idle) }
    }

    fun dismissPairingResult() {
        state.update { it.copy(pairing = PairingState.Idle) }
    }

    fun activate(id: String) {
        viewModelScope.launch { repository.setActive(id) }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            repository.remove(id)
            appSlotsRepository.removeOwner(id)
            screenLayoutRepository.removeOwner(id)
        }
    }

    private fun Exception.toPairingError(): PairingError = when (this) {
        is TvConnectionException.Unreachable, is TvConnectionException.ConnectionLost -> PairingError.UNREACHABLE
        is TvConnectionException.CertificateChanged -> PairingError.CERTIFICATE_CHANGED
        is TvConnectionException.PairingRejected -> PairingError.REJECTED
        is TvConnectionException.Timeout -> PairingError.TIMEOUT
        else -> PairingError.UNKNOWN
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SetupViewModel(
                    repository = container.tvRepository,
                    appSlotsRepository = container.appSlotsRepository,
                    screenLayoutRepository = container.screenLayoutRepository,
                    discovery = container.tvDiscovery,
                    pairingService = container.pairingService,
                    showEmulatorHint = DevicePlatform.isDebugEmulator
                )
            }
        }
    }
}
