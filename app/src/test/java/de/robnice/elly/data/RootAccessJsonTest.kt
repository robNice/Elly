package de.robnice.elly.data

import de.robnice.elly.data.model.RootAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RootAccessJsonTest {

    @Test
    fun roundTrip() {
        val access = RootAccess(22, "root", "AAAAE2VjZHNh")

        assertEquals(access, RootAccessJson.decode(RootAccessJson.encode(access)))
    }

    @Test
    fun invalidDataIsIgnored() {
        assertNull(RootAccessJson.decode(null))
        assertNull(RootAccessJson.decode("not json"))
        assertNull(RootAccessJson.decode("{\"port\":0,\"user\":\"root\",\"hostKey\":\"x\"}"))
        assertNull(RootAccessJson.decode("{\"port\":22,\"user\":\"\",\"hostKey\":\"x\"}"))
        assertNull(RootAccessJson.decode("{\"port\":22,\"user\":\"root\"}"))
    }
}
