package de.robnice.elly.data

import de.robnice.elly.data.model.CustomElement
import de.robnice.elly.data.model.CustomElementType
import de.robnice.elly.data.model.CustomScreen
import de.robnice.elly.data.model.EpgConfig
import de.robnice.elly.data.model.RemoteScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgElementTest {

    private val config = EpgConfig("A", "TV")

    @Test
    fun addPlacesFullWidthHalfHeightElement() {
        val screen = CustomScreen.EMPTY.add(CustomElementType.EPG, epg = config)!!
        val element = screen.elements.single()

        assertEquals(CustomScreen.COLUMNS, element.width)
        assertEquals(CustomScreen.ROWS / 2, element.height)
        assertEquals(config, element.epg)
    }

    @Test
    fun secondElementFitsBelowTheFirst() {
        val screen = CustomScreen.EMPTY
            .add(CustomElementType.EPG, epg = config)!!
            .add(CustomElementType.EPG, epg = EpgConfig("B", "Kids"))

        assertNotNull(screen)
        assertEquals(24, screen!!.elements[1].y)
        assertNull(screen.add(CustomElementType.EPG, epg = config))
    }

    @Test
    fun jsonKeepsFavoriteList() {
        val screen = CustomScreen.EMPTY.add(CustomElementType.EPG, epg = config)!!
        val encoded = CustomScreenJson.encodeAll(mapOf("tv" to mapOf(RemoteScreen.CUSTOM to screen)))

        val decoded = CustomScreenJson.decodeAll(encoded)["tv"]!![RemoteScreen.CUSTOM]!!

        assertEquals(screen, decoded)
    }

    @Test
    fun normalizeDropsGuideWithoutList() {
        val element = CustomElement(1, CustomElementType.EPG, 0, 0, 24, 24)

        assertTrue(CustomScreen.normalize(listOf(element)).elements.isEmpty())
    }

    @Test
    fun updateEpgAndWithoutEpg() {
        val screen = CustomScreen.EMPTY.add(CustomElementType.EPG, epg = config)!!
        val id = screen.elements.single().id
        val changed = screen.updateEpg(id, EpgConfig("C", "Radio"))

        assertEquals("C", changed.elements.single().epg?.listId)
        assertTrue(changed.withoutEpg().elements.isEmpty())
    }
}
