package de.robnice.elly.data

import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchPage
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.remote.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLayoutJsonTest {

    @Test
    fun encodeAndDecode_roundTrip() {
        val layout = WatchLayout()
            .reordered(listOf(WatchPage.MEDIA, WatchPage.NAVIGATION))
            .withVisible(WatchPage.NUMBERS, false)
            .with(WatchSlot.NAV_LEFT, WatchAction.App("netflix", "Netflix", AppDisplay.BOTH))
            .with(WatchSlot.TILE_3, WatchAction.Key(RemoteKey.GUIDE))
            .with(WatchSlot.MEDIA_LEFT, WatchAction.PlayPause)

        assertEquals(layout, WatchLayoutJson.decode(WatchLayoutJson.encode(layout)))
    }

    @Test
    fun with_defaultActionIsNotStored() {
        val layout = WatchLayout().with(WatchSlot.NAV_LEFT, WatchAction.Key(RemoteKey.BACK))

        assertTrue(layout.assigned.isEmpty())
    }

    @Test
    fun withVisible_keepsTheLastVisiblePage() {
        var layout = WatchLayout()
        WatchPage.entries.forEach { layout = layout.withVisible(it, false) }

        assertEquals(listOf(WatchPage.entries.last()), layout.visiblePages)
    }

    @Test
    fun decode_appendsMissingPagesAndSkipsUnknownNames() {
        val layout = WatchLayoutJson.decode(
            """{"pages":[{"page":"MEDIA","visible":true},{"page":"GONE","visible":true}],
               "actions":{"NAV_LEFT":{"key":"NO_SUCH_KEY"},"NO_SLOT":{"key":"HOME"},"NAV_RIGHT":{"key":"GUIDE"}}}"""
        )

        assertEquals(WatchPage.MEDIA, layout.pages.first().page)
        assertEquals(WatchPage.entries.size, layout.pages.size)
        assertEquals(WatchAction.Key(RemoteKey.BACK), layout.action(WatchSlot.NAV_LEFT))
        assertEquals(WatchAction.Key(RemoteKey.GUIDE), layout.action(WatchSlot.NAV_RIGHT))
        assertEquals(WatchLayout(), WatchLayoutJson.decode("no json"))
    }
}
