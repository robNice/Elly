package de.robnice.elly.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgCacheTest {

    private val hour = EpgCache.HOUR_MILLIS
    private val now = 1_000 * hour
    private val vox = EpgChannel("ch_vox", "vox", "7", "VOX HD")
    private val ard = EpgChannel("ch_ard", "ard", "1", "Das Erste HD")

    private fun program(signalId: String, startHour: Long, hours: Long = 1, title: String = "P$startHour") =
        EpgProgram(signalId, "$signalId-$startHour", now + startHour * hour, now + (startHour + hours) * hour, title)

    private val base = EpgCache().withChannels("A", listOf(vox, ard), now)

    @Test
    fun mergeKeepsOlderProgramsOutsideTheWindow() {
        val cache = base
            .merge(listOf(program("vox", -10), program("vox", -2), program("vox", 1)), setOf("vox"), now - 12 * hour, now + 2 * hour)
            .merge(listOf(program("vox", -1, title = "new"), program("vox", 0)), setOf("vox"), now - hour, now + 3 * hour)

        assertEquals(listOf(-10L, -2L, -1L, 0L), cache.programs["vox"]!!.map { (it.start - now) / hour })
        assertEquals("new", cache.programs["vox"]!![2].title)
    }

    @Test
    fun mergeDropsOldProgramsOverlappedByNewOnes() {
        val cache = base
            .merge(listOf(program("vox", 0, hours = 2)), setOf("vox"), now, now + 2 * hour)
            .merge(listOf(program("vox", -1, hours = 2)), setOf("vox"), now - 2 * hour, now)

        assertEquals(listOf(-1L), cache.programs["vox"]!!.map { (it.start - now) / hour })
    }

    @Test
    fun mergeLeavesOtherChannelsAlone() {
        val cache = base
            .merge(listOf(program("ard", 0)), setOf("ard"), now, now + hour)
            .merge(emptyList(), setOf("vox"), now, now + hour)

        assertEquals(1, cache.programs["ard"]!!.size)
        assertNull(cache.programs["vox"])
    }

    @Test
    fun prunedKeepsLast24HoursAndNext14Days() {
        val cache = base.merge(
            listOf(program("vox", -30), program("vox", -25, hours = 2), program("vox", 0), program("vox", 14 * 24 + 1)),
            setOf("vox"),
            now - 40 * hour,
            now + 20 * 24 * hour
        ).pruned(now)

        assertEquals(listOf(-25L, 0L), cache.programs["vox"]!!.map { (it.start - now) / hour })
    }

    @Test
    fun prunedDropsChannelsNoLongerInAList() {
        val cache = base.merge(listOf(program("gone", 0)), setOf("gone"), now, now + hour).pruned(now)

        assertNull(cache.programs["gone"])
    }

    @Test
    fun guideListsChannelsInOrderWithTheirPrograms() {
        val guide = base.merge(listOf(program("ard", 0)), setOf("ard", "vox"), now, now + hour).guide("A")!!

        assertEquals(listOf("VOX HD", "Das Erste HD"), guide.channels.map { it.name })
        assertTrue(guide.programs["vox"]!!.isEmpty())
        assertEquals(1, guide.programs["ard"]!!.size)
        assertNull(base.guide("B"))
    }

    @Test
    fun jsonRoundTrip() {
        val withLogo = EpgChannel("ch", "sig", "3", "RTL HD", "/mnt/lg/cmn_data/channel_logo/x.png", "https://logo/x.png", "251533369-04FC")
        val cache = EpgCache().withChannels("A", listOf(withLogo), now)
            .merge(
                listOf(
                    EpgProgram("sig", "id1", now, now + hour, "Title", "Sub", "Desc", "Details", "Drama", "1_2_3", true, "https://img/1.jpg"),
                    EpgProgram("sig", "id2", now + hour, now + 2 * hour, "Plain")
                ),
                setOf("sig"),
                now,
                now + 2 * hour
            )
            .copy(nearUpdated = now, farUpdated = now - hour)

        assertEquals(cache, EpgCacheJson.decode(EpgCacheJson.encode(cache)))
    }

    @Test
    fun brokenJsonGivesEmptyCache() {
        assertEquals(EpgCache(), EpgCacheJson.decode("{broken"))
        assertEquals(EpgCache(), EpgCacheJson.decode("{\"v\":99}"))
    }
}
