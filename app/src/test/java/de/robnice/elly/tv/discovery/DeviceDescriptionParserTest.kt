package de.robnice.elly.tv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceDescriptionParserTest {

    @Test
    fun parse_readsFriendlyAndModelName() {
        val xml = """
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <friendlyName>[LG] webOS TV OLED55C1</friendlyName>
                <modelName>OLED55C1 </modelName>
              </device>
            </root>
        """.trimIndent()

        val description = DeviceDescriptionParser.parse(xml)

        assertEquals("[LG] webOS TV OLED55C1", description.friendlyName)
        assertEquals("OLED55C1", description.modelName)
    }

    @Test
    fun parse_unescapesEntitiesAndSupportsPrefixes() {
        val xml = "<d:friendlyName>Living room &amp; kitchen</d:friendlyName>"

        assertEquals("Living room & kitchen", DeviceDescriptionParser.parse(xml).friendlyName)
    }

    @Test
    fun parse_returnsNullForMissingOrBlankElements() {
        val description = DeviceDescriptionParser.parse("<friendlyName> </friendlyName>")

        assertNull(description.friendlyName)
        assertNull(description.modelName)
    }
}
