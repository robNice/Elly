package de.robnice.elly.tv.ssh

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

// Runs only against a real TV: set ELLY_TV_SSH_HOST (and optionally ELLY_TV_SSH_PORT).
// Uses an unknown key, so the login itself fails; algorithm negotiation and host key checks run before that.
class TvShellIntegrationTest {

    private val host: String? = System.getenv("ELLY_TV_SSH_HOST")
    private val port = System.getenv("ELLY_TV_SSH_PORT")?.toIntOrNull() ?: 22

    private fun unknownIdentity(): EcdsaIdentity {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        return EcdsaIdentity("test", keyPair.public as ECPublicKey) { data ->
            Signature.getInstance("SHA256withECDSA").run {
                initSign(keyPair.private)
                update(data)
                sign()
            }
        }
    }

    @Test
    fun negotiatesAndRejectsUnknownKey() {
        assumeTrue(host != null)
        assertThrows(TvShellException.AuthFailed::class.java) {
            runBlocking { TvShell().session(SshTarget(host!!, port, "root", null), SshAuth.Key(unknownIdentity())) { } }
        }
    }

    @Test
    fun rejectsChangedHostKey() {
        assumeTrue(host != null)
        assertThrows(TvShellException.HostKeyChanged::class.java) {
            runBlocking { TvShell().session(SshTarget(host!!, port, "root", "AAAA"), SshAuth.Key(unknownIdentity())) { } }
        }
    }
}
