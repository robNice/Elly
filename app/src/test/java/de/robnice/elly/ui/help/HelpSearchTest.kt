package de.robnice.elly.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpSearchTest {

    private val entries = listOf(
        HelpEntry(0, 0, "Fernseher suchen", "Tippe auf „Suchen“, um Fernseher im Netzwerk zu finden."),
        HelpEntry(0, 1, "Koppeln", "Bestätige die Anfrage am Fernseher."),
        HelpEntry(1, 0, "Zeiger", "• Bewegung: Der Zeiger folgt dem Handy.\n• Touchpad: Wischen bewegt den Zeiger.")
    )
    private val search = HelpSearch(entries)

    @Test
    fun search_findsTermsInTitleAndBody() {
        assertEquals(listOf(0, 1), search.search("fernseher").map { it.entry.topic }.sorted())
    }

    @Test
    fun search_requiresAllTerms() {
        assertEquals(listOf(entries[2]), search.search("zeiger wischen").map { it.entry })
    }

    @Test
    fun search_ranksTitleMatchesFirst() {
        assertEquals(entries[0], search.search("fernseher").first().entry)
    }

    @Test
    fun search_ignoresCaseAndAccents() {
        assertEquals(listOf(entries[1]), search.search("BESTATIGE").map { it.entry })
    }

    @Test
    fun search_ignoresTooShortQueries() {
        assertTrue(search.search("z").isEmpty())
        assertTrue(search.search("   ").isEmpty())
    }

    @Test
    fun fold_keepsLength() {
        val text = "Größe ändern"
        assertEquals(text.length, HelpSearch.fold(text).length)
        assertEquals("große andern", HelpSearch.fold(text))
    }

    @Test
    fun matchRanges_findsAllOccurrencesAndMerges() {
        assertEquals(listOf(4..8, 17..21), HelpSearch.matchRanges("Der Zeiger folgt Zeiger", listOf("zeige")))
        assertEquals(listOf(0..5), HelpSearch.matchRanges("Zeiger", listOf("zei", "eiger")))
    }

    @Test
    fun snippet_removesBulletsAndMarksCuts() {
        val body = "• Bewegung: Der Zeiger folgt dem Handy.\n• Touchpad: Wischen bewegt den Zeiger."
        assertEquals("Bewegung: Der Zeiger folgt dem Handy. Touchpad: Wischen bewegt den Zeiger.", HelpSearch.snippet(body, listOf("zeiger")))
        val long = "a ".repeat(60) + "Treffer " + "b ".repeat(60)
        val snippet = HelpSearch.snippet(long, listOf("treffer"))
        assertTrue(snippet.startsWith("…") && snippet.endsWith("…") && snippet.contains("Treffer"))
    }
}
