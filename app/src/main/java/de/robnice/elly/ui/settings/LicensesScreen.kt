package de.robnice.elly.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import de.robnice.elly.R

@Composable
fun LicensesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(-1) }

    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.setup_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.privacy_block_spacing))
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.licenses_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.setup_close)) }
        }
        Text(
            text = stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OpenSourceComponents.all.forEachIndexed { index, component ->
            Column(modifier = Modifier.padding(top = dimensionResource(R.dimen.privacy_heading_spacing))) {
                Text(text = stringResource(component.name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.license_line, stringResource(component.copyright), stringResource(component.license)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { expanded = if (expanded == index) -1 else index }) {
                    Text(stringResource(if (expanded == index) R.string.licenses_hide else R.string.licenses_show))
                }
                if (expanded == index) {
                    val text = remember(component.text) {
                        context.resources.openRawResource(component.text).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
