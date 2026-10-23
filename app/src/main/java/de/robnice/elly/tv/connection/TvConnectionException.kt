package de.robnice.elly.tv.connection

sealed class TvConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Unreachable(cause: Throwable? = null) : TvConnectionException("TV not reachable", cause)
    class CertificateChanged : TvConnectionException("TV certificate changed")
    class PairingRejected(reason: String?) : TvConnectionException("Pairing rejected: $reason")
    class Timeout : TvConnectionException("TV did not respond in time")
    class ConnectionLost : TvConnectionException("Connection to TV lost")
    class RequestFailed(reason: String?) : TvConnectionException("Request failed: $reason")
}
