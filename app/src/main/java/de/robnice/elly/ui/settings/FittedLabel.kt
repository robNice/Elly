package de.robnice.elly.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// One line under a preview tile; the text shrinks instead of breaking inside a word.
@Composable
fun FittedLabel(text: String, color: Color) {
    val style = MaterialTheme.typography.bodySmall
    BasicText(
        text = text,
        maxLines = 1,
        softWrap = false,
        style = style.copy(color = color, textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(
            minFontSize = MIN_FONT_SP.sp,
            maxFontSize = style.fontSize,
            stepSize = STEP_SP.sp
        )
    )
}

private const val MIN_FONT_SP = 8
private const val STEP_SP = 0.5f
