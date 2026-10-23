package de.robnice.elly.ui.settings

// Minimal Markdown subset used by the bundled privacy policy: "## " headings, "- " bullets and paragraphs.
object PolicyText {

    sealed interface Block {
        data class Heading(val text: String) : Block
        data class Bullet(val text: String) : Block
        data class Paragraph(val text: String) : Block
    }

    fun parse(text: String): List<Block> = text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            when {
                line.startsWith(HEADING) -> Block.Heading(line.removePrefix(HEADING).trim())
                line.startsWith(BULLET) -> Block.Bullet(line.removePrefix(BULLET).trim())
                else -> Block.Paragraph(line)
            }
        }

    private const val HEADING = "## "
    private const val BULLET = "- "
}
