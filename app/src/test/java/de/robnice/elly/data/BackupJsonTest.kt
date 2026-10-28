package de.robnice.elly.data

import de.robnice.elly.data.model.AppSlot
import de.robnice.elly.data.model.ColorPalette
import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.EpgConfig
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.data.model.ScreenBackground
import de.robnice.elly.data.model.ScreenLayout
import de.robnice.elly.remote.RemoteKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    @Test
    fun screensRoundTripWithoutIconPaths() {
        val screen = CustomScreen.normalize(
            listOf(
                CustomElement(1, CustomElementType.KEY, 0, 0, 4, 4, key = RemoteKey.HOME),
                CustomElement(2, CustomElementType.APP, 4, 0, 8, 3, app = AppSlot(true, "netflix", "Netflix", "/data/icons/netflix.img")),
                CustomElement(3, CustomElementType.EPG, 0, 4, 24, 24, epg = EpgConfig("list-7", "Favoriten 1"))
            )
        )
        val backup = Backup.Screens(ScreenLayout.DEFAULT.toggle(RemoteScreen.CUSTOM), mapOf(RemoteScreen.CUSTOM to screen))

        val raw = BackupJson.encode(BackupContent(backup, null))
        val content = BackupJson.decode(raw)!!
        val restored = content.screens!!

        assertNull(content.appearance)

        assertFalse(raw.contains("/data/icons"))
        assertEquals(backup.layout, restored.layout)
        val elements = restored.screens.getValue(RemoteScreen.CUSTOM).elements
        assertNull(elements.single { it.id == 2L }.app?.iconPath)
        assertEquals("netflix", elements.single { it.id == 2L }.app?.appId)
        assertEquals(EpgConfig("list-7", "Favoriten 1"), elements.single { it.id == 3L }.epg)
        assertEquals(RemoteKey.HOME, elements.single { it.id == 1L }.key)
    }

    @Test
    fun appearanceRoundTrip() {
        val image = byteArrayOf(1, 2, 3, 4)
        val backup = Backup.Appearance(ColorPalette.entries.last(), ScreenBackground.CUSTOM, image, 0.3f, 0.7f, true)

        val restored = BackupJson.decode(BackupJson.encode(BackupContent(null, backup)))!!.appearance!!

        assertEquals(backup.palette, restored.palette)
        assertEquals(ScreenBackground.CUSTOM, restored.background)
        assertArrayEquals(image, restored.image)
        assertEquals(0.3f, restored.keyRounding, 0.0001f)
        assertEquals(0.7f, restored.dpadRounding, 0.0001f)
        assertTrue(restored.lightEffect)
    }

    @Test
    fun screensAndAppearanceInOneFile() {
        val screens = Backup.Screens(ScreenLayout.DEFAULT.toggle(RemoteScreen.CUSTOM), emptyMap())
        val appearance = Backup.Appearance(ColorPalette.OCEAN, ScreenBackground.HILLS, null, 0.2f, 1f, true)

        val content = BackupJson.decode(BackupJson.encode(BackupContent(screens, appearance)))!!

        assertEquals(screens.layout, content.screens!!.layout)
        assertEquals(ColorPalette.OCEAN, content.appearance!!.palette)
        assertEquals(ScreenBackground.HILLS, content.appearance!!.background)
    }

    @Test
    fun readsTheOlderSingleFiles() {
        val appearance = BackupJson.decode(
            """{"format":"elly-appearance","version":1,"palette":"RUBY","background":"SKY","keyRounding":0.4,"dpadRounding":0.6,"lightEffect":true}"""
        )!!
        assertNull(appearance.screens)
        assertEquals(ColorPalette.RUBY, appearance.appearance!!.palette)
        assertEquals(ScreenBackground.SKY, appearance.appearance!!.background)
        assertTrue(appearance.appearance!!.lightEffect)

        val screens = BackupJson.decode("""{"format":"elly-screens","version":1}""")!!
        assertNull(screens.appearance)
        assertEquals(ScreenLayout.DEFAULT, screens.screens!!.layout)
    }

    @Test
    fun recognisesTheKindOfFile() {
        assertNull(BackupJson.decode("""{"format":"elly-backup","version":1}"""))
        assertNull(BackupJson.decode("""{"format":"something-else"}"""))
        assertNull(BackupJson.decode("not json"))
    }
}
