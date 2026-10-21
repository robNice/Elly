package de.robnice.elgy.ui.setup

import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.tv.discovery.DiscoveredTv

data class SetupUiState(
    val scanState: ScanState = ScanState.IDLE,
    val foundTvs: List<DiscoveredTv> = emptyList(),
    val pairedTvs: List<PairedTv> = emptyList(),
    val activeTvId: String? = null,
    val manualIp: String = "",
    val manualIpInvalid: Boolean = false,
    val pairing: PairingState = PairingState.Idle,
    val showEmulatorHint: Boolean = false
)

enum class ScanState { IDLE, SCANNING, DONE }

sealed interface PairingState {
    data object Idle : PairingState
    data class Connecting(val ipAddress: String) : PairingState
    data class AwaitingConfirmation(val ipAddress: String) : PairingState
    data class Paired(val tv: PairedTv) : PairingState
    data class Failed(val error: PairingError) : PairingState
}

enum class PairingError { UNREACHABLE, CERTIFICATE_CHANGED, REJECTED, TIMEOUT, UNKNOWN }
