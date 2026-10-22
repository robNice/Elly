package de.robnice.elgy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import de.robnice.elgy.R

@Composable
fun RockerKey(
    label: String?,
    @DrawableRes topIcon: Int,
    @DrawableRes bottomIcon: Int,
    topDescription: String,
    bottomDescription: String,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    modifier: Modifier = Modifier.size(dimensionResource(R.dimen.rocker_width), dimensionResource(R.dimen.rocker_height)),
    @DrawableRes labelIcon: Int? = null
) {
    val rounding = LocalKeyRounding.current
    val shape = keyShape(rounding)
    val palette = KeyStyle.DARK.palette()
    val topInteraction = remember { MutableInteractionSource() }
    val bottomInteraction = remember { MutableInteractionSource() }
    val topPressed by topInteraction.collectIsPressedAsState()
    val bottomPressed by bottomInteraction.collectIsPressedAsState()

    Column(
        modifier = modifier.keyBody(shape, palette, pressed = false),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RockerHalf(
            icon = topIcon,
            description = topDescription,
            interactionSource = topInteraction,
            pressed = topPressed,
            pressedColor = palette.pressedOverlay,
            contentColor = palette.content,
            shape = rockerHalfShape(rounding, top = true),
            onPress = onTop
        )
        if (labelIcon != null) KeyIcon(icon = labelIcon, tint = palette.content)
        if (label != null) KeyLabel(text = label, color = palette.content, fontSize = 13, fontWeight = FontWeight.Bold)
        RockerHalf(
            icon = bottomIcon,
            description = bottomDescription,
            interactionSource = bottomInteraction,
            pressed = bottomPressed,
            pressedColor = palette.pressedOverlay,
            contentColor = palette.content,
            shape = rockerHalfShape(rounding, top = false),
            onPress = onBottom
        )
    }
}

@Composable
private fun ColumnScope.RockerHalf(
    @DrawableRes icon: Int,
    description: String,
    interactionSource: MutableInteractionSource,
    pressed: Boolean,
    pressedColor: Color,
    contentColor: Color,
    shape: Shape,
    onPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(if (pressed) pressedColor else Color.Transparent, shape)
            .keyPress(interactionSource, description, repeatOnHold = true, onPress = onPress),
        contentAlignment = Alignment.Center
    ) {
        KeyIcon(icon = icon, tint = contentColor)
    }
}
