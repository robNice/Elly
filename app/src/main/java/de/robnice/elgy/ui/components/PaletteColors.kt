package de.robnice.elgy.ui.components

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import de.robnice.elgy.R
import de.robnice.elgy.data.model.ColorPalette

val LocalColorPalette = compositionLocalOf { ColorPalette.GRAPHITE }

data class PaletteColors(
    val keyTop: Color,
    val keyBottom: Color,
    val ringTop: Color,
    val ringBottom: Color,
    val content: Color,
    val lightTop: Color,
    val lightBottom: Color,
    val onLight: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color
)

private class PaletteResources(
    @ColorRes val keyTop: Int,
    @ColorRes val keyBottom: Int,
    @ColorRes val ringTop: Int,
    @ColorRes val ringBottom: Int,
    @ColorRes val content: Int,
    @ColorRes val lightTop: Int,
    @ColorRes val lightBottom: Int,
    @ColorRes val onLight: Int,
    @ColorRes val backgroundTop: Int,
    @ColorRes val backgroundBottom: Int
)

private fun ColorPalette.resources(): PaletteResources = when (this) {
    ColorPalette.GRAPHITE -> PaletteResources(
        R.color.key_body_top, R.color.key_body_bottom, R.color.dpad_ring_top, R.color.dpad_ring_bottom,
        R.color.key_content, R.color.key_light_top, R.color.key_light_bottom, R.color.key_content_on_light,
        R.color.palette_graphite_background_top, R.color.palette_graphite_background_bottom
    )
    ColorPalette.SILVER -> PaletteResources(
        R.color.palette_silver_key_top, R.color.palette_silver_key_bottom,
        R.color.palette_silver_ring_top, R.color.palette_silver_ring_bottom, R.color.palette_silver_content,
        R.color.palette_silver_light_top, R.color.palette_silver_light_bottom, R.color.palette_silver_on_light,
        R.color.palette_silver_background_top, R.color.palette_silver_background_bottom
    )
    ColorPalette.OCEAN -> PaletteResources(
        R.color.palette_ocean_key_top, R.color.palette_ocean_key_bottom,
        R.color.palette_ocean_ring_top, R.color.palette_ocean_ring_bottom, R.color.palette_ocean_content,
        R.color.palette_ocean_light_top, R.color.palette_ocean_light_bottom, R.color.palette_ocean_on_light,
        R.color.palette_ocean_background_top, R.color.palette_ocean_background_bottom
    )
    ColorPalette.FOREST -> PaletteResources(
        R.color.palette_forest_key_top, R.color.palette_forest_key_bottom,
        R.color.palette_forest_ring_top, R.color.palette_forest_ring_bottom, R.color.palette_forest_content,
        R.color.palette_forest_light_top, R.color.palette_forest_light_bottom, R.color.palette_forest_on_light,
        R.color.palette_forest_background_top, R.color.palette_forest_background_bottom
    )
    ColorPalette.COPPER -> PaletteResources(
        R.color.palette_copper_key_top, R.color.palette_copper_key_bottom,
        R.color.palette_copper_ring_top, R.color.palette_copper_ring_bottom, R.color.palette_copper_content,
        R.color.palette_copper_light_top, R.color.palette_copper_light_bottom, R.color.palette_copper_on_light,
        R.color.palette_copper_background_top, R.color.palette_copper_background_bottom
    )
    ColorPalette.VIOLET -> PaletteResources(
        R.color.palette_violet_key_top, R.color.palette_violet_key_bottom,
        R.color.palette_violet_ring_top, R.color.palette_violet_ring_bottom, R.color.palette_violet_content,
        R.color.palette_violet_light_top, R.color.palette_violet_light_bottom, R.color.palette_violet_on_light,
        R.color.palette_violet_background_top, R.color.palette_violet_background_bottom
    )
    ColorPalette.PETROL -> PaletteResources(
        R.color.palette_petrol_key_top, R.color.palette_petrol_key_bottom,
        R.color.palette_petrol_ring_top, R.color.palette_petrol_ring_bottom, R.color.palette_petrol_content,
        R.color.palette_petrol_light_top, R.color.palette_petrol_light_bottom, R.color.palette_petrol_on_light,
        R.color.palette_petrol_background_top, R.color.palette_petrol_background_bottom
    )
    ColorPalette.BORDEAUX -> PaletteResources(
        R.color.palette_bordeaux_key_top, R.color.palette_bordeaux_key_bottom,
        R.color.palette_bordeaux_ring_top, R.color.palette_bordeaux_ring_bottom, R.color.palette_bordeaux_content,
        R.color.palette_bordeaux_light_top, R.color.palette_bordeaux_light_bottom, R.color.palette_bordeaux_on_light,
        R.color.palette_bordeaux_background_top, R.color.palette_bordeaux_background_bottom
    )
    ColorPalette.SAND -> PaletteResources(
        R.color.palette_sand_key_top, R.color.palette_sand_key_bottom,
        R.color.palette_sand_ring_top, R.color.palette_sand_ring_bottom, R.color.palette_sand_content,
        R.color.palette_sand_light_top, R.color.palette_sand_light_bottom, R.color.palette_sand_on_light,
        R.color.palette_sand_background_top, R.color.palette_sand_background_bottom
    )
    ColorPalette.CORAL -> PaletteResources(
        R.color.palette_coral_key_top, R.color.palette_coral_key_bottom,
        R.color.palette_coral_ring_top, R.color.palette_coral_ring_bottom, R.color.palette_coral_content,
        R.color.palette_coral_light_top, R.color.palette_coral_light_bottom, R.color.palette_coral_on_light,
        R.color.palette_coral_background_top, R.color.palette_coral_background_bottom
    )
    ColorPalette.MIDNIGHT -> PaletteResources(
        R.color.palette_midnight_key_top, R.color.palette_midnight_key_bottom,
        R.color.palette_midnight_ring_top, R.color.palette_midnight_ring_bottom, R.color.palette_midnight_content,
        R.color.palette_midnight_light_top, R.color.palette_midnight_light_bottom, R.color.palette_midnight_on_light,
        R.color.palette_midnight_background_top, R.color.palette_midnight_background_bottom
    )
    ColorPalette.SAGE -> PaletteResources(
        R.color.palette_sage_key_top, R.color.palette_sage_key_bottom,
        R.color.palette_sage_ring_top, R.color.palette_sage_ring_bottom, R.color.palette_sage_content,
        R.color.palette_sage_light_top, R.color.palette_sage_light_bottom, R.color.palette_sage_on_light,
        R.color.palette_sage_background_top, R.color.palette_sage_background_bottom
    )
    ColorPalette.RUBY -> PaletteResources(
        R.color.palette_ruby_key_top, R.color.palette_ruby_key_bottom,
        R.color.palette_ruby_ring_top, R.color.palette_ruby_ring_bottom, R.color.palette_ruby_content,
        R.color.palette_ruby_light_top, R.color.palette_ruby_light_bottom, R.color.palette_ruby_on_light,
        R.color.palette_ruby_background_top, R.color.palette_ruby_background_bottom
    )
    ColorPalette.LAGOON -> PaletteResources(
        R.color.palette_lagoon_key_top, R.color.palette_lagoon_key_bottom,
        R.color.palette_lagoon_ring_top, R.color.palette_lagoon_ring_bottom, R.color.palette_lagoon_content,
        R.color.palette_lagoon_light_top, R.color.palette_lagoon_light_bottom, R.color.palette_lagoon_on_light,
        R.color.palette_lagoon_background_top, R.color.palette_lagoon_background_bottom
    )
    ColorPalette.AMBER -> PaletteResources(
        R.color.palette_amber_key_top, R.color.palette_amber_key_bottom,
        R.color.palette_amber_ring_top, R.color.palette_amber_ring_bottom, R.color.palette_amber_content,
        R.color.palette_amber_light_top, R.color.palette_amber_light_bottom, R.color.palette_amber_on_light,
        R.color.palette_amber_background_top, R.color.palette_amber_background_bottom
    )
}

@Composable
fun ColorPalette.colors(): PaletteColors {
    val r = resources()
    return PaletteColors(
        keyTop = colorResource(r.keyTop),
        keyBottom = colorResource(r.keyBottom),
        ringTop = colorResource(r.ringTop),
        ringBottom = colorResource(r.ringBottom),
        content = colorResource(r.content),
        lightTop = colorResource(r.lightTop),
        lightBottom = colorResource(r.lightBottom),
        onLight = colorResource(r.onLight),
        backgroundTop = colorResource(r.backgroundTop),
        backgroundBottom = colorResource(r.backgroundBottom)
    )
}

@Composable
fun currentPaletteColors(): PaletteColors = LocalColorPalette.current.colors()
