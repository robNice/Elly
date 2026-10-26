package de.robnice.elly.ui.settings

import android.content.ContentResolver
import android.net.Uri
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
import de.robnice.elly.tv.ssh.SshAuth
import de.robnice.elly.tv.ssh.TvShellException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

enum class SshError { UNREACHABLE, AUTH_FAILED, HOST_KEY_CHANGED, NO_LUNA, KEY_INVALID, KEY_PASSPHRASE, KEY_FILE, UNKNOWN }

enum class SshMethod { PASSWORD, KEY }

enum class RootAccessCheck { IDLE, CHECKING, OK, FAILED }

data class SshSetupForm(
    val port: String = DEFAULT_PORT,
    val user: String = DEFAULT_USER,
    val method: SshMethod = SshMethod.PASSWORD,
    val password: String = "",
    val key: String = "",
    val passphrase: String = "",
    val busy: Boolean = false,
    val error: SshError? = null
) {
    val portNumber: Int? get() = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val canSubmit: Boolean
        get() = !busy && portNumber != null && user.isNotBlank() &&
            if (method == SshMethod.PASSWORD) password.isNotEmpty() else key.isNotBlank()

    // OpenSSH only reads a key whose last line ends with a line break.
    val auth: SshAuth
        get() = if (method == SshMethod.PASSWORD) {
            SshAuth.Password(password)
        } else {
            SshAuth.PrivateKey(key.trim() + "\n", passphrase.takeIf { it.isNotEmpty() })
        }

    companion object {
        const val DEFAULT_PORT = "22"
        const val DEFAULT_USER = "root"
    }
}

data class RootAccessUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val oldKey: Boolean = false,
    val form: SshSetupForm? = null,
    val check: RootAccessCheck = RootAccessCheck.IDLE,
    val checkError: SshError? = null,
    val confirmRemove: Boolean = false,
    val removing: Boolean = false,
    val confirmOldKeyRemoval: Boolean = false,
    val oldKeyRemoved: Boolean = false
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

    private val access = activeTv
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else rootAccessRepository.access(id) }

    val state: StateFlow<RootAccessUiState> = combine(local, activeTv, settingsRepository.demoMode, access) { current, tv, demo, root ->
        current.copy(available = tv != null && !demo, enabled = root != null, oldKey = root?.oldElly == true)
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

    fun updateForm(
        port: String? = null,
        user: String? = null,
        method: SshMethod? = null,
        password: String? = null,
        key: String? = null,
        passphrase: String? = null
    ) {
        local.update { state ->
            val form = state.form ?: return@update state
            state.copy(
                form = form.copy(
                    port = port?.filter(Char::isDigit) ?: form.port,
                    user = user?.trim() ?: form.user,
                    method = method ?: form.method,
                    password = password ?: form.password,
                    key = key ?: form.key,
                    passphrase = passphrase ?: form.passphrase,
                    error = null
                )
            )
        }
    }

    fun importKey(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { it.readKeyFile() } }.getOrNull()
            }
            if (text == null) {
                local.update { state -> state.copy(form = state.form?.copy(error = SshError.KEY_FILE)) }
            } else {
                updateForm(key = text)
            }
        }
    }

    // Key files are small text files; anything larger is not a private key.
    private fun InputStream.readKeyFile(): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(MAX_KEY_FILE_BYTES)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_KEY_FILE_BYTES) return null
        }
        return out.toString(Charsets.UTF_8.name())
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
            val error = attempt { service.setUp(tv, port, form.user, form.auth) }
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
            currentTv()?.let { service.forget(it.id) }
            local.update { it.copy(removing = false, check = RootAccessCheck.IDLE, checkError = null) }
        }
    }

    fun askOldKeyRemoval() {
        local.update { it.copy(confirmOldKeyRemoval = true) }
    }

    fun dismissOldKeyRemoval() {
        local.update { it.copy(confirmOldKeyRemoval = false) }
    }

    fun confirmOldKeyRemoval() {
        local.update { it.copy(confirmOldKeyRemoval = false, removing = true) }
        viewModelScope.launch {
            val tv = currentTv() ?: return@launch local.update { it.copy(removing = false) }
            val error = attempt { service.removeOldKey(tv) }
            local.update {
                if (error == null) {
                    it.copy(removing = false, check = RootAccessCheck.IDLE, checkError = null, oldKeyRemoved = true)
                } else {
                    it.copy(removing = false, check = RootAccessCheck.FAILED, checkError = error)
                }
            }
        }
    }

    fun dismissOldKeyRemoved() {
        local.update { it.copy(oldKeyRemoved = false) }
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
        is TvShellException.InvalidKey -> SshError.KEY_INVALID
        is TvShellException.PassphraseNeeded -> SshError.KEY_PASSPHRASE
        else -> SshError.UNKNOWN
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val MAX_KEY_FILE_BYTES = 16 * 1024

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
