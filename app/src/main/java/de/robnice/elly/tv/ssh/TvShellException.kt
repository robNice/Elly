package de.robnice.elly.tv.ssh

sealed class TvShellException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unreachable(cause: Throwable? = null) : TvShellException("SSH server not reachable", cause)
    class AuthFailed(cause: Throwable? = null) : TvShellException("SSH login rejected", cause)
    class HostKeyChanged(cause: Throwable? = null) : TvShellException("SSH host key changed", cause)
    class Timeout : TvShellException("SSH command did not finish in time")
    class CommandFailed(val exitStatus: Int, output: String) : TvShellException("SSH command failed ($exitStatus): $output")
    class LunaUnavailable(reason: String?) : TvShellException("luna-send not usable: $reason")
    class InvalidKey(cause: Throwable? = null) : TvShellException("Private key not usable", cause)
    class PassphraseNeeded : TvShellException("Private key needs the correct passphrase")
    class Failed(cause: Throwable) : TvShellException("SSH failed: ${cause.message}", cause)
}
