package de.robnice.elly.tv.ssh

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class TvShellKeyTest {

    private val shell = TvShell()

    private fun privateKey(type: Int, size: Int = 0, passphrase: String? = null): String {
        val pair = KeyPair.genKeyPair(JSch(), type, size)
        val out = ByteArrayOutputStream()
        if (passphrase == null) pair.writeOpenSSHv1PrivateKey(out, null) else pair.writeOpenSSHv1PrivateKey(out, passphrase.toByteArray())
        pair.dispose()
        return out.toString(Charsets.UTF_8.name())
    }

    @Test
    fun readsEd25519EcdsaAndRsaKeys() {
        shell.checkKey(privateKey(KeyPair.ED25519), null)
        shell.checkKey(privateKey(KeyPair.ECDSA, 256), null)
        shell.checkKey(privateKey(KeyPair.RSA, 2048), null)
    }

    @Test
    fun protectedKeyNeedsTheRightPassphrase() {
        val key = privateKey(KeyPair.ED25519, passphrase = "secret")

        shell.checkKey(key, "secret")
        assertThrows(TvShellException.PassphraseNeeded::class.java) { shell.checkKey(key, null) }
        assertThrows(TvShellException.PassphraseNeeded::class.java) { shell.checkKey(key, "wrong") }
    }

    @Test
    fun otherTextIsNoKey() {
        assertThrows(TvShellException.InvalidKey::class.java) { shell.checkKey("not a key", null) }
    }
}
