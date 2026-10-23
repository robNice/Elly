package de.robnice.elly.ui.help

import java.text.Normalizer

data class HelpEntry(val chapter: Int, val topic: Int, val title: String, val body: String)

data class HelpMatch(val entry: HelpEntry, val snippet: String)

class HelpSearch(private val entries: List<HelpEntry>) {

    private val foldedEntries = entries.map { fold(it.title) to fold(it.body) }

    fun search(query: String): List<HelpMatch> {
        val terms = terms(query)
        if (terms.isEmpty()) return emptyList()
        return entries.indices
            .filter { index ->
                val (title, body) = foldedEntries[index]
                terms.all { title.contains(it) || body.contains(it) }
            }
            .sortedByDescending { index -> terms.count { foldedEntries[index].first.contains(it) } }
            .map { index -> HelpMatch(entries[index], snippet(entries[index].body, terms)) }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val SNIPPET_BEFORE = 30
        private const val SNIPPET_LENGTH = 110
        private const val ELLIPSIS = "…"

        fun terms(query: String): List<String> =
            fold(query).split(' ', '\t', '\n').map { it.trim() }.filter { it.length >= MIN_QUERY_LENGTH }.distinct()

        // Lower case without accents, keeping the length so that indices map back to the original text.
        fun fold(text: String): String = buildString(text.length) {
            text.forEach { char ->
                val base = Normalizer.normalize(char.toString(), Normalizer.Form.NFD).first()
                append(base.lowercaseChar())
            }
        }

        fun matchRanges(text: String, terms: List<String>): List<IntRange> {
            if (terms.isEmpty()) return emptyList()
            val folded = fold(text)
            val ranges = terms.flatMap { term ->
                generateSequence(folded.indexOf(term).takeIf { it >= 0 }) { start ->
                    folded.indexOf(term, start + term.length).takeIf { it >= 0 }
                }.map { it until it + term.length }.toList()
            }.sortedBy { it.first }
            return ranges.fold(mutableListOf()) { merged, range ->
                val last = merged.lastOrNull()
                if (last != null && range.first <= last.last + 1) {
                    merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
                } else {
                    merged.add(range)
                }
                merged
            }
        }

        fun snippet(body: String, terms: List<String>): String {
            val plain = body.replace("\n• ", " ").replace("• ", "").replace('\n', ' ')
            val folded = fold(plain)
            val hit = terms.map { folded.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
            var start = (hit - SNIPPET_BEFORE).coerceAtLeast(0)
            if (start > 0) start = plain.indexOf(' ', start).let { if (it in start until hit) it + 1 else start }
            var end = (start + SNIPPET_LENGTH).coerceAtMost(plain.length)
            if (end < plain.length) end = plain.lastIndexOf(' ', end).let { if (it > hit) it else end }
            val prefix = if (start > 0) ELLIPSIS else ""
            val suffix = if (end < plain.length) ELLIPSIS else ""
            return prefix + plain.substring(start, end).trim() + suffix
        }
    }
}
