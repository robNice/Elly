package de.robnice.elly.data

import de.robnice.elly.data.model.AppSlot
import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.DefaultScreens
import de.robnice.elly.data.model.RemoteScreen
import de.robnice.elly.remote.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CustomScreenTest {

    private val key = CustomElementType.KEY

    @Test
    fun add_placesAtFirstFreeSpotWithDefaultSize() {
        val screen = CustomScreen.EMPTY
            .add(key, key = RemoteKey.POWER)!!
            .add(key, key = RemoteKey.HOME)!!

        val second = screen.elements[1]
        assertEquals(key.defaultWidth, second.x)
        assertEquals(0, second.y)
        assertEquals(key.defaultWidth, second.width)
        assertEquals(key.defaultHeight, second.height)
    }

    @Test
    fun add_shrinksWhenDefaultSizeDoesNotFit() {
        val free = CustomElementType.VOLUME.minWidth
        val blocker = CustomElement(1, CustomElementType.DPAD, 0, 0, CustomScreen.COLUMNS - free, CustomScreen.ROWS)
        val screen = CustomScreen(listOf(blocker))

        val added = screen.add(CustomElementType.VOLUME)

        assertNotNull(added)
        val rocker = added!!.elements.last()
        assertEquals(CustomScreen.COLUMNS - free, rocker.x)
        assertEquals(free, rocker.width)
        assertEquals(CustomElementType.VOLUME.defaultHeight, rocker.height)
        assertNull(added.add(CustomElementType.DPAD))
    }

    @Test
    fun add_returnsNullWhenFull() {
        val full = CustomScreen(
            listOf(CustomElement(1, CustomElementType.DPAD, 0, 0, CustomScreen.COLUMNS, CustomScreen.ROWS))
        )

        assertNull(full.add(key, key = RemoteKey.OK))
    }

    @Test
    fun move_ignoresOverlapsAndPositionsOutsideTheGrid() {
        val screen = CustomScreen.EMPTY
            .add(key, key = RemoteKey.POWER)!!
            .add(key, key = RemoteKey.HOME)!!

        assertSame(screen, screen.move(2, 1, 0))
        assertSame(screen, screen.move(2, CustomScreen.COLUMNS - 1, 0))
        assertEquals(10, screen.move(2, 8, 10).elements[1].y)
    }

    @Test
    fun resize_respectsMinimumSize() {
        val screen = CustomScreen.EMPTY.add(CustomElementType.DPAD)!!
        val dpad = CustomElementType.DPAD

        assertSame(screen, screen.resize(1, dpad.minWidth - 1, dpad.minHeight))
        assertEquals(dpad.minWidth, screen.resize(1, dpad.minWidth, dpad.minHeight).elements.single().width)
    }

    @Test
    fun resize_allowsSmallerAppKeysOnlyWhenIconOnly() {
        val withText = CustomScreen.EMPTY.add(CustomElementType.APP)!!
        val iconOnly = withText.updateApp(1) { AppSlot(enabled = true, appId = "netflix", title = "Netflix", iconPath = "/icon") }

        assertSame(withText, withText.resize(1, 2, 2))
        assertEquals(2, iconOnly.resize(1, 2, 2).elements.single().width)
    }

    @Test
    fun normalize_dropsOverlappingAndInvalidElements() {
        val screen = CustomScreen.normalize(
            listOf(
                CustomElement(1, key, 0, 0, 4, 3, key = RemoteKey.OK),
                CustomElement(2, key, 2, 1, 4, 3, key = RemoteKey.BACK),
                CustomElement(3, key, 10, 0, 4, 3),
                CustomElement(4, CustomElementType.APP, 0, 10, 8, 2)
            )
        )

        assertEquals(listOf(1L, 4L), screen.elements.map { it.id })
    }

    @Test
    fun json_roundTrip() {
        val screen = CustomScreen.EMPTY
            .add(key, key = RemoteKey.MUTE)!!
            .add(CustomElementType.APP)!!
            .add(CustomElementType.DPAD)!!
            .let { added ->
                added.updateApp(added.elements[1].id) {
                    AppSlot(enabled = true, appId = "netflix", title = "Netflix", showText = true, textColor = 0xFFFF0000)
                }
            }

        val stored = mapOf("tv" to mapOf(RemoteScreen.MEDIA to screen))

        assertEquals(stored, CustomScreenJson.decodeAll(CustomScreenJson.encodeAll(stored)))
    }

    @Test
    fun json_scalesLegacyEightColumnLayouts() {
        val legacy = """{"tv": [{"id": 1, "type": "KEY", "x": 1, "y": 2, "w": 1, "h": 1, "key": "OK"}]}"""

        val element = CustomScreenJson.decodeAll(legacy).getValue("tv").getValue(RemoteScreen.CUSTOM).elements.single()

        assertEquals(3, element.x)
        assertEquals(8, element.y)
        assertEquals(3, element.width)
        assertEquals(4, element.height)
    }

    @Test
    fun json_scalesFirstFineGridToCurrentRows() {
        val stored = """{"tv": {"columns": 24, "elements": [{"id": 1, "type": "KEY", "x": 4, "y": 9, "w": 4, "h": 3, "key": "OK"}]}}"""

        val element = CustomScreenJson.decodeAll(stored).getValue("tv").getValue(RemoteScreen.CUSTOM).elements.single()

        assertEquals(4, element.x)
        assertEquals(12, element.y)
        assertEquals(4, element.height)
    }

    @Test
    fun defaultScreens_fitTheGridWithoutOverlaps() {
        DefaultScreens.all().forEach { (screen, layout) ->
            assertEquals(screen.name, layout, CustomScreen.normalize(layout.elements))
        }
    }

    @Test
    fun json_invalidInput_isEmpty() {
        assertEquals(emptyMap<String, CustomScreen>(), CustomScreenJson.decodeAll("not json"))
    }
}
