package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun accessOfOlderVersionsHasNoPassword() {
        val raw = "{\"port\":22,\"user\":\"root\",\"hostKey\":\"x\"}"

        assertEquals(RootAccess(22, "root", "x"), RootAccessJson.decode(raw, decrypt))
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
