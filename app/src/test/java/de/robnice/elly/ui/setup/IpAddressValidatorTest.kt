package de.robnice.elly.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IpAddressValidatorTest {

    @Test
    fun acceptsValidAddresses() {
        assertTrue(IpAddressValidator.isValidIpv4("192.168.0.10"))
        assertTrue(IpAddressValidator.isValidIpv4(" 10.0.0.1 "))
    }

    @Test
    fun rejectsInvalidAddresses() {
        listOf("", "192.168.0", "192.168.0.256", "192.168..1", "a.b.c.d", "1.2.3.4.5", "0001.2.3.4")
            .forEach { assertFalse(it, IpAddressValidator.isValidIpv4(it)) }
    }
}
