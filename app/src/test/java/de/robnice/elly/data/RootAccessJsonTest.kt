package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootAccessJsonTest {

    private val encrypt: (String) -> String = { "sealed:" + it.reversed() }
    private val decrypt: (String) -> String? = { it.removePrefix("sealed:").reversed() }

    @Test
    fun roundTrip() {
        val access = RootAccess(22, "root", "AAAAE2VjZHNh", "alpine")
        val raw = RootAccessJson.encode(access, encrypt)

        assertFalse(raw.contains("alpine"))
        assertEquals(access, RootAccessJson.decode(raw, decrypt))
    }

    @Test
    fun keyAndPassphraseAreStoredEncrypted() {
        val access = RootAccess(22, "root", "x", privateKey = "-----BEGIN KEY-----", passphrase = "secret")
        val raw = RootAccessJson.encode(access, encrypt)

        assertFalse(raw.contains("BEGIN") || raw.contains("secret"))
        assertEquals(access, RootAccessJson.decode(raw, decrypt))
        assertFalse(access.oldElly)
    }

    @Test
    fun accessOfOlderVersionsHasNoPassword() {
        val raw = "{\"port\":22,\"user\":\"root\",\"hostKey\":\"x\"}"

        assertEquals(RootAccess(22, "root", "x"), RootAccessJson.decode(raw, decrypt))
        assertTrue(RootAccess(22, "root", "x").oldElly)
    }

    @Test
    fun passwordThatCannotBeDecryptedIsDropped() {
        val raw = RootAccessJson.encode(RootAccess(22, "root", "x", "alpine"), encrypt)

        assertEquals(RootAccess(22, "root", "x"), RootAccessJson.decode(raw) { null })
    }

    @Test
    fun invalidDataIsIgnored() {
        assertNull(RootAccessJson.decode(null, decrypt))
        assertNull(RootAccessJson.decode("not json", decrypt))
        assertNull(RootAccessJson.decode("{\"port\":0,\"user\":\"root\",\"hostKey\":\"x\"}", decrypt))
        assertNull(RootAccessJson.decode("{\"port\":22,\"user\":\"\",\"hostKey\":\"x\"}", decrypt))
        assertNull(RootAccessJson.decode("{\"port\":22,\"user\":\"root\"}", decrypt))
    }
}
