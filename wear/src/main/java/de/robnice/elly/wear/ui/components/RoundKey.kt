package de.robnice.elly.wear.ui.components

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import de.robnice.elly.wear.R

@Composable
fun TextKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ColoredKey(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    @ColorRes container: Int,
    @ColorRes content: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = colorResource(container),
            contentColor = colorResource(content)
        )
    ) {
        Icon(painterResource(icon), stringResource(label), Modifier.size(dimensionResource(R.dimen.key_icon_size)))
    }
}

