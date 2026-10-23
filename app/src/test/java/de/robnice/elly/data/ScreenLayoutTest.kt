package de.robnice.elly.data

import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenEntry
import de.robnice.elly.data.model.ScreenLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLayoutTest {

    @Test
    fun toggle_disablesAndEnablesScreens() {
        val layout = ScreenLayout.DEFAULT.toggle(RemoteScreen.NUMBERS)

        assertEquals(listOf(RemoteScreen.NAVIGATION, RemoteScreen.MEDIA), layout.enabledScreens)
        assertEquals(ScreenLayout.DEFAULT, layout.toggle(RemoteScreen.NUMBERS))
    }

    @Test
    fun toggle_keepsLastEnabledScreen() {
        val single = ScreenLayout.DEFAULT.toggle(RemoteScreen.NUMBERS).toggle(RemoteScreen.MEDIA)

        assertEquals(listOf(RemoteScreen.NAVIGATION), single.toggle(RemoteScreen.NAVIGATION).enabledScreens)
    }

    @Test
    fun reorder_changesOrderAndKeepsState() {
        val layout = ScreenLayout.DEFAULT.toggle(RemoteScreen.NUMBERS)
            .reorder(listOf(RemoteScreen.CUSTOM, RemoteScreen.MEDIA, RemoteScreen.NAVIGATION, RemoteScreen.NUMBERS))

        assertEquals(listOf(RemoteScreen.MEDIA, RemoteScreen.NAVIGATION), layout.enabledScreens)
        assertEquals(RemoteScreen.NUMBERS, layout.entries.last().screen)
    }

    @Test
    fun normalize_addsMissingScreensAndEnsuresOneEnabled() {
        val layout = ScreenLayout.normalize(listOf(ScreenEntry(RemoteScreen.MEDIA, enabled = false)))

        assertEquals(RemoteScreen.entries.size, layout.entries.size)
        assertTrue(layout.enabledScreens.isNotEmpty())
        assertEquals(RemoteScreen.MEDIA, layout.entries.first().screen)
    }

    @Test
    fun json_roundTripPerOwner() {
        val layouts = mapOf(
            "tv-1" to ScreenLayout.DEFAULT.reorder(listOf(RemoteScreen.MEDIA, RemoteScreen.NUMBERS, RemoteScreen.NAVIGATION)),
            "demo" to ScreenLayout.DEFAULT.toggle(RemoteScreen.NAVIGATION)
        )

        assertEquals(layouts, ScreenLayoutJson.decodeAll(ScreenLayoutJson.encodeAll(layouts)))
    }

    @Test
    fun json_ignoresUnknownScreens() {
        val decoded = ScreenLayoutJson.decodeAll("""{"tv-1": [{"screen": "UNKNOWN"}, {"screen": "MEDIA", "enabled": true}]}""")

        assertEquals(RemoteScreen.MEDIA, decoded.getValue("tv-1").entries.first().screen)
    }
}
