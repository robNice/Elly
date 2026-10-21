package de.robnice.elgy.ui.remote

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import de.robnice.elgy.R
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.components.KeyIcon
import de.robnice.elgy.ui.components.KeyLabel
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.RemoteKeyButton

@Composable
fun IconKey(
    key: RemoteKey,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onKey: (RemoteKey) -> Unit,
    style: KeyStyle = KeyStyle.DARK,
    small: Boolean = false,
    repeatOnHold: Boolean = false
) {
    val size = dimensionResource(if (small) R.dimen.key_size_small else R.dimen.key_size)
    RemoteKeyButton(
        contentDescription = stringResource(description),
        onPress = { onKey(key) },
        modifier = Modifier.size(size),
        style = style,
        repeatOnHold = repeatOnHold
    ) { color -> KeyIcon(icon = icon, tint = color) }
}

@Composable
fun TextKey(key: RemoteKey, @StringRes label: Int, onKey: (RemoteKey) -> Unit) {
    RemoteKeyButton(
        contentDescription = stringResource(label),
        onPress = { onKey(key) },
        modifier = Modifier.size(dimensionResource(R.dimen.key_size))
    ) { color -> KeyLabel(text = stringResource(label), color = color, fontSize = 18, fontWeight = FontWeight.Bold) }
}

@Composable
fun TransportKey(
    key: RemoteKey,
    @DrawableRes icon: Int,
    @StringRes description: Int,
    onKey: (RemoteKey) -> Unit,
    iconTint: Color? = null
) {
    RemoteKeyButton(
        contentDescription = stringResource(description),
        onPress = { onKey(key) },
        modifier = Modifier.size(dimensionResource(R.dimen.key_size_transport))
    ) { color -> KeyIcon(icon = icon, tint = iconTint ?: color) }
}

@Composable
fun WideKey(
    key: RemoteKey,
    label: String,
    description: String,
    onKey: (RemoteKey) -> Unit,
    style: KeyStyle = KeyStyle.DARK,
    labelColor: Color? = null,
    fontSize: Int = 20
) {
    RemoteKeyButton(
        contentDescription = description,
        onPress = { onKey(key) },
        modifier = Modifier.size(dimensionResource(R.dimen.key_wide_width), dimensionResource(R.dimen.key_wide_height)),
        shape = RoundedCornerShape(percent = 50),
        style = style
    ) { color -> KeyLabel(text = label, color = labelColor ?: color, fontSize = fontSize) }
}

@Composable
fun ColorKey(key: RemoteKey, style: KeyStyle, @StringRes description: Int, onKey: (RemoteKey) -> Unit) {
    RemoteKeyButton(
        contentDescription = stringResource(description),
        onPress = { onKey(key) },
        modifier = Modifier.size(dimensionResource(R.dimen.key_color_width), dimensionResource(R.dimen.key_color_height)),
        shape = RoundedCornerShape(percent = 50),
        style = style
    ) { }
}
