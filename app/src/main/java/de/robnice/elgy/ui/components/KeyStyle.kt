package de.robnice.elgy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.colorResource
import de.robnice.elgy.R

enum class KeyStyle { DARK, POWER, LIGHT, RED, GREEN, YELLOW, BLUE }

data class KeyPalette(
    val top: Color,
    val bottom: Color,
    val content: Color,
    val rimLight: Color,
    val rimDark: Color,
    val highlight: Color,
    val pressedOverlay: Color
)

@Composable
fun KeyStyle.palette(): KeyPalette {
    val rimLight = colorResource(R.color.key_rim_light)
    val rimDark = colorResource(R.color.key_rim_dark)
    val highlight = colorResource(R.color.key_highlight)
    val pressed = colorResource(R.color.key_pressed_overlay)
    val content = colorResource(R.color.key_content)
    val colors = currentPaletteColors()

    fun colored(base: Color) = KeyPalette(
        top = lerp(base, Color.White, COLOR_LIGHTEN),
        bottom = lerp(base, Color.Black, COLOR_DARKEN),
        content = content,
        rimLight = rimLight,
        rimDark = rimDark,
        highlight = highlight,
        pressedOverlay = pressed
    )

    return when (this) {
        KeyStyle.DARK -> KeyPalette(colors.keyTop, colors.keyBottom, colors.content, rimLight, rimDark, highlight, pressed)
        KeyStyle.POWER -> KeyPalette(
            colorResource(R.color.key_red_top), colorResource(R.color.key_red_bottom),
            content, rimLight, rimDark, highlight, pressed
        )
        KeyStyle.LIGHT -> KeyPalette(colors.lightTop, colors.lightBottom, colors.onLight, rimLight, rimDark, highlight, pressed)
        KeyStyle.RED -> colored(colorResource(R.color.key_color_red))
        KeyStyle.GREEN -> colored(colorResource(R.color.key_color_green))
        KeyStyle.YELLOW -> colored(colorResource(R.color.key_color_yellow))
        KeyStyle.BLUE -> colored(colorResource(R.color.key_color_blue))
    }
}

private const val COLOR_LIGHTEN = 0.18f
private const val COLOR_DARKEN = 0.45f
