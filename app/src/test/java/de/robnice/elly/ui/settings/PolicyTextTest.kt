package de.robnice.elly.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyTextTest {

    @Test
    fun parse_recognizesHeadingsBulletsAndParagraphs() {
        val blocks = PolicyText.parse("## 1. General\nSome text.\n\n- First\n- Second\n")

        assertEquals(
            listOf(
                PolicyText.Block.Heading("1. General"),
                PolicyText.Block.Paragraph("Some text."),
                PolicyText.Block.Bullet("First"),
                PolicyText.Block.Bullet("Second")
            ),
            blocks
        )
    }

    @Test
    fun parse_ignoresBlankLines() {
        assertEquals(emptyList<PolicyText.Block>(), PolicyText.parse("\n  \n\n"))
    }
}
