package de.robnice.elly.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import de.robnice.elly.R
import de.robnice.elly.ui.elly.EllySeason
import java.time.LocalDate

@Composable
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Image(
            painter = painterResource(EllySeason.of(LocalDate.now()).image),
            contentDescription = stringResource(R.string.onboarding_help),
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_elly_size))
        )
    }
}
