package de.robnice.elly.tv.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchChangedHostKeyException
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

data class SshTarget(val host: String, val port: Int, val user: String, val hostKey: String?)

sealed class SshAuth {
    class Password(val password: String) : SshAuth()
    class Key(val identity: EcdsaIdentity) : SshAuth()
    class PrivateKey(val key: String, val passphrase: String?) : SshAuth()
}

data class ShellResult(val exitStatus: Int, val output: String)

// SSH access to the root shell of a TV running the Homebrew Channel (dropbear).
// Only algorithms that Android supports without extra providers are offered; Ed25519 keys use Bouncy Castle.
class TvShell {

    // Throws InvalidKey or PassphraseNeeded if JSch cannot use the key.
    fun checkKey(key: String, passphrase: String?) {
        val pair = try {
            KeyPair.load(JSch(), key.toByteArray(Charsets.UTF_8), null)
        } catch (e: JSchException) {
            throw TvShellException.InvalidKey(e)
        }
        try {
            if (pair.isEncrypted && !pair.decrypt(passphrase?.toByteArray(Charsets.UTF_8))) throw TvShellException.PassphraseNeeded()
        } finally {
            pair.dispose()
        }
    }

    suspend fun <T> session(target: SshTarget, auth: SshAuth, block: suspend (ShellSession) -> T): T =
        withContext(Dispatchers.IO) {
            val session = open(target, auth)
            try {
                block(session)
            } finally {
                session.close()
            }
        }

    private fun open(target: SshTarget, auth: SshAuth): ShellSession {
        val jsch = JSch()
        val hostKeys = PinnedHostKey(target.hostKey)
        jsch.hostKeyRepository = hostKeys
        when (auth) {
            is SshAuth.Key -> jsch.addIdentity(auth.identity, null)
            is SshAuth.PrivateKey -> try {
                jsch.addIdentity(USER_KEY, auth.key.toByteArray(Charsets.UTF_8), null, auth.passphrase?.toByteArray(Charsets.UTF_8))
            } catch (e: JSchException) {
                throw TvShellException.InvalidKey(e)
            }
            is SshAuth.Password -> Unit
        }
        val session = jsch.getSession(target.user, target.host, target.port)
        ALGORITHMS.forEach { (key, value) -> session.setConfig(key, value) }
        when (auth) {
            is SshAuth.Password -> {
                session.setPassword(auth.password)
                session.setConfig(PREFERRED_AUTHENTICATIONS, AUTH_PASSWORD)
            }
            is SshAuth.Key, is SshAuth.PrivateKey -> session.setConfig(PREFERRED_AUTHENTICATIONS, AUTH_PUBLIC_KEY)
        }
        try {
            session.timeout = READ_TIMEOUT_MILLIS
            session.connect(CONNECT_TIMEOUT_MILLIS)
        } catch (e: JSchException) {
            session.disconnect()
            throw e.toShellException()
        }
        val hostKey = hostKeys.seen
        if (hostKey == null || (target.hostKey != null && target.hostKey != hostKey)) {
            session.disconnect()
            throw TvShellException.HostKeyChanged()
        }
        return ShellSession(session, hostKey)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 6_000
        private const val READ_TIMEOUT_MILLIS = 10_000
        private const val PREFERRED_AUTHENTICATIONS = "PreferredAuthentications"
        private const val AUTH_PASSWORD = "password,keyboard-interactive"
        private const val AUTH_PUBLIC_KEY = "publickey"
        private const val USER_KEY = "user-key"

        // Without this JSch would pick its Ed25519 classes for Java 15+, which do not run on Android.
        init {
            JSch.setConfig("ssh-ed25519", "com.jcraft.jsch.bc.SignatureEd25519")
            JSch.setConfig("keypairgen.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
            JSch.setConfig("keypairgen_fromprivate.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        }

        private val ALGORITHMS = mapOf(
            "StrictHostKeyChecking" to "yes",
            "kex" to "ecdh-sha2-nistp256,diffie-hellman-group14-sha256",
            "server_host_key" to SshKeyFormat.ALGORITHM,
            "PubkeyAcceptedAlgorithms" to "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256",
            "cipher.c2s" to "aes128-ctr,aes256-ctr",
            "cipher.s2c" to "aes128-ctr,aes256-ctr",
            "mac.c2s" to "hmac-sha2-256",
            "mac.s2c" to "hmac-sha2-256"
        )

        internal fun JSchException.toShellException(): TvShellException {
            val root = generateSequence<Throwable>(this) { it.cause }.last()
            return when {
                this is JSchChangedHostKeyException -> TvShellException.HostKeyChanged(this)
                message?.startsWith("Auth") == true -> TvShellException.AuthFailed(this)
                root is ConnectException || root is NoRouteToHostException || root is UnknownHostException ||
                    root is SocketTimeoutException || message?.contains("timeout", ignoreCase = true) == true ->
                    TvShellException.Unreachable(this)
                else -> TvShellException.Failed(this)
            }
        }
    }
}

class ShellSession internal constructor(private val session: Session, val hostKey: String) {

    // luna-send prints nothing without a terminal, so every command gets a pseudo terminal.
    suspend fun run(command: String, timeoutMillis: Long = COMMAND_TIMEOUT_MILLIS): ShellResult {
        val channel = try {
            (session.openChannel(CHANNEL_EXEC) as ChannelExec).apply {
                setCommand(command)
                setPty(true)
            }
        } catch (e: JSchException) {
            throw TvShellException.Failed(e)
        }
        try {
            val input = channel.inputStream
            channel.connect(CHANNEL_CONNECT_TIMEOUT_MILLIS)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            val deadline = System.currentTimeMillis() + timeoutMillis
            while (true) {
                while (input.available() > 0) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                if (channel.isClosed && input.available() == 0) break
                if (System.currentTimeMillis() > deadline) throw TvShellException.Timeout()
                delay(POLL_INTERVAL_MILLIS)
            }
            return ShellResult(channel.exitStatus, output.toString(Charsets.UTF_8.name()))
        } catch (e: TvShellException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: JSchException) {
            throw TvShellException.Failed(e)
        } finally {
            channel.disconnect()
        }
    }

    internal fun close() {
        session.disconnect()
    }

    private companion object {
        const val CHANNEL_EXEC = "exec"
        const val CHANNEL_CONNECT_TIMEOUT_MILLIS = 5_000
        const val COMMAND_TIMEOUT_MILLIS = 15_000L
        const val POLL_INTERVAL_MILLIS = 20L
        const val BUFFER_SIZE = 8 * 1024
    }
}
