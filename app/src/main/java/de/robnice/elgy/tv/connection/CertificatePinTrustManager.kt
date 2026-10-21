package de.robnice.elgy.tv.connection

import android.annotation.SuppressLint
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

// webOS uses a self-signed certificate, so the TV is trusted on first use and pinned afterwards.
@SuppressLint("CustomX509TrustManager")
class CertificatePinTrustManager(
    private val expectedPin: String?,
    private val trustAll: Boolean
) : X509TrustManager {

    @Volatile
    var observedPin: String? = null
        private set

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val certificate = chain?.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        val pin = pinOf(certificate)
        observedPin = pin
        if (trustAll || expectedPin == null) return
        if (pin != expectedPin) throw CertificatePinMismatchException()
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun pinOf(certificate: X509Certificate): String =
            MessageDigest.getInstance("SHA-256")
                .digest(certificate.encoded)
                .joinToString(":") { "%02X".format(it) }
    }
}

class CertificatePinMismatchException : CertificateException("TV certificate does not match the stored pin")
