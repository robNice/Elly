package de.robnice.elly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robnice.elly.AppContainer
import de.robnice.elly.data.RootAccessRepository
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.tv.ssh.RootAccessService
import de.robnice.elly.tv.ssh.TvShellException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SshError { UNREACHABLE, AUTH_FAILED, HOST_KEY_CHANGED, NO_LUNA, UNKNOWN }

enum class RootAccessCheck { IDLE, CHECKING, OK, FAILED }

data class SshSetupForm(
    val port: String = DEFAULT_PORT,
    val user: String = DEFAULT_USER,
    val password: String = "",
    val busy: Boolean = false,
    val error: SshError? = null
) {
    val portNumber: Int? get() = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val canSubmit: Boolean get() = !busy && portNumber != null && user.isNotBlank() && password.isNotEmpty()

    companion object {
        const val DEFAULT_PORT = "22"
        const val DEFAULT_USER = "root"
    }
}

data class RootAccessUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val form: SshSetupForm? = null,
    val check: RootAccessCheck = RootAccessCheck.IDLE,
    val checkError: SshError? = null,
    val confirmRemove: Boolean = false,
    val removing: Boolean = false,
    val removedOnlyLocally: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class RootAccessViewModel(
    tvRepository: TvRepository,
    settingsRepository: SettingsRepository,
    rootAccessRepository: RootAccessRepository,
    private val service: RootAccessService
) : ViewModel() {

    private val activeTv = tvRepository.activeTv
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val local = MutableStateFlow(RootAccessUiState())

    private val enabled = activeTv
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(false) else rootAccessRepository.access(id).map { it != null } }

    val state: StateFlow<RootAccessUiState> = combine(local, activeTv, settingsRepository.demoMode, enabled) { current, tv, demo, on ->
        current.copy(available = tv != null && !demo, enabled = on)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RootAccessUiState())

    init {
        viewModelScope.launch {
            activeTv.map { it?.id }.distinctUntilChanged().collect { local.value = RootAccessUiState() }
        }
    }

    fun setEnabled(checked: Boolean) {
        if (checked) {
            local.update { it.copy(form = SshSetupForm()) }
        } else {
            local.update { it.copy(confirmRemove = true) }
        }
    }

    fun updateForm(port: String? = null, user: String? = null, password: String? = null) {
        local.update { state ->
            val form = state.form ?: return@update state
            state.copy(
                form = form.copy(
                    port = port?.filter(Char::isDigit) ?: form.port,
                    user = user?.trim() ?: form.user,
                    password = password ?: form.password,
                    error = null
                )
            )
        }
    }

    fun dismissForm() {
        if (local.value.form?.busy == true) return
        local.update { it.copy(form = null) }
    }

    fun submitForm() {
        val form = local.value.form?.takeIf { it.canSubmit } ?: return
        val port = form.portNumber ?: return
        local.update { it.copy(form = form.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val tv = currentTv() ?: return@launch local.update { it.copy(form = null) }
            val error = attempt { service.setUp(tv, port, form.user, form.password) }
            local.update { state ->
                if (error == null) {
                    state.copy(form = null, check = RootAccessCheck.OK, checkError = null)
                } else {
                    state.copy(form = state.form?.copy(busy = false, error = error))
                }
            }
        }
    }

    fun check() {
        if (local.value.check == RootAccessCheck.CHECKING) return
        local.update { it.copy(check = RootAccessCheck.CHECKING, checkError = null) }
        viewModelScope.launch {
            val tv = currentTv() ?: return@launch
            val error = attempt { service.check(tv) }
            local.update {
                it.copy(check = if (error == null) RootAccessCheck.OK else RootAccessCheck.FAILED, checkError = error)
            }
        }
    }

    fun dismissRemove() {
        local.update { it.copy(confirmRemove = false) }
    }

    fun confirmRemove() {
        local.update { it.copy(confirmRemove = false, removing = true) }
        viewModelScope.launch {
            val tv = currentTv()
            val removedOnTv = tv == null || service.remove(tv)
            local.update {
                it.copy(removing = false, check = RootAccessCheck.IDLE, checkError = null, removedOnlyLocally = !removedOnTv)
            }
        }
    }

    fun dismissRemovedNotice() {
        local.update { it.copy(removedOnlyLocally = false) }
    }

    private suspend fun currentTv(): PairedTv? = activeTv.value ?: activeTv.first()

    private suspend fun attempt(action: suspend () -> Unit): SshError? = try {
        action()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: TvShellException) {
        e.toError()
    } catch (_: Exception) {
        SshError.UNKNOWN
    }

    private fun TvShellException.toError(): SshError = when (this) {
        is TvShellException.Unreachable, is TvShellException.Timeout -> SshError.UNREACHABLE
        is TvShellException.AuthFailed -> SshError.AUTH_FAILED
        is TvShellException.HostKeyChanged -> SshError.HOST_KEY_CHANGED
        is TvShellException.LunaUnavailable -> SshError.NO_LUNA
        else -> SshError.UNKNOWN
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                RootAccessViewModel(
                    container.tvRepository,
                    container.settingsRepository,
                    container.rootAccessRepository,
                    container.rootAccessService
                )
            }
        }
    }
}
