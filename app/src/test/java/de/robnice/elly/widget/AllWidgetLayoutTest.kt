package de.robnice.elly.widget

import de.robnice.elly.remote.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllWidgetLayoutTest {

    @Test
    fun keys_areExactlyTheKeysOfTheSingleWidgets() {
        val single = WidgetKind.entries.filter { it != WidgetKind.ALL }.flatMap { it.keys.values }.toSet()
        assertEquals(single, AllWidgetLayout.keys.toSet())
        assertEquals(AllWidgetLayout.keys.size, AllWidgetLayout.keys.toSet().size)
    }

    @Test
    fun rows_allKeysGiveOneRowPerGroup() {
        assertEquals(AllWidgetLayout.groups, AllWidgetLayout.rows(AllWidgetLayout.keys.toSet()))
    }

    @Test
    fun rows_smallGroupsShareARow() {
        val rows = AllWidgetLayout.rows(setOf(RemoteKey.POWER, RemoteKey.MUTE, RemoteKey.PLAY, RemoteKey.PAUSE))
        assertEquals(listOf(listOf(RemoteKey.POWER, RemoteKey.MUTE, RemoteKey.PLAY, RemoteKey.PAUSE)), rows)
    }

    @Test
    fun rows_neverExceedFiveKeysAndKeepGroupsTogether() {
        val rows = AllWidgetLayout.rows(setOf(RemoteKey.POWER, RemoteKey.MUTE) + WidgetKind.VOLUME.keys.values + WidgetKind.CHANNEL.keys.values)
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.size <= 5 })
        assertEquals(listOf(RemoteKey.POWER, RemoteKey.MUTE), rows.first())
    }

    @Test
    fun rows_splitGroupsEvenlyWhenTheyDoNotFit() {
        val rows = AllWidgetLayout.rows(WidgetKind.MEDIA.keys.values.toSet(), columns = 4)
        assertEquals(listOf(3, 2), rows.map { it.size })
    }

    @Test
    fun rows_respectTheColumnCount() {
        (AllWidgetLayout.MIN_COLUMNS..AllWidgetLayout.MAX_COLUMNS).forEach { columns ->
            val rows = AllWidgetLayout.rows(AllWidgetLayout.keys.toSet(), columns)
            assertTrue(rows.all { it.size <= columns })
            assertEquals(AllWidgetLayout.keys, rows.flatten())
        }
    }

    @Test
    fun rows_wideRowsJoinGroups() {
        val rows = AllWidgetLayout.rows(AllWidgetLayout.keys.toSet(), columns = 8)
        assertEquals(listOf(8, 5), rows.map { it.size })
    }

    @Test
    fun rows_emptySelectionGivesNoRows() {
        assertTrue(AllWidgetLayout.rows(emptySet()).isEmpty())
    }
}
