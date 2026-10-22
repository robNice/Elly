package de.robnice.elgy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import de.robnice.elgy.ElgyApplication
import de.robnice.elgy.R
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.ui.MainActivity
import de.robnice.elgy.ui.enableDarkEdgeToEdge
import de.robnice.elgy.ui.theme.ElgyTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private val container
        get() = (application as ElgyApplication).container

    private var selectedTvId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED, resultIntent(widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableDarkEdgeToEdge()
        setContent {
            ElgyTheme {
                val tvs by container.tvRepository.pairedTvs.collectAsStateWithLifecycle(initialValue = null)
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TvChoice(
                        tvs = tvs,
                        selectedTvId = selectedTvId,
                        onSelect = { tv -> select(widgetId, tv) },
                        onOpenApp = {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun select(widgetId: Int, tv: PairedTv) {
        if (selectedTvId != null) return
        selectedTvId = tv.id
        lifecycleScope.launch {
            container.widgetController.bind(widgetId, tv.id)
            setResult(RESULT_OK, resultIntent(widgetId))
            finish()
        }
    }

    private fun resultIntent(widgetId: Int) = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}

@Composable
private fun TvChoice(tvs: List<PairedTv>?, selectedTvId: String?, onSelect: (PairedTv) -> Unit, onOpenApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(dimensionResource(R.dimen.setup_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
    ) {
        Text(text = stringResource(R.string.widget_config_title), style = MaterialTheme.typography.headlineSmall)
        when {
            tvs == null -> Unit
            tvs.isEmpty() -> {
                Text(
                    text = stringResource(R.string.widget_config_no_tvs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onOpenApp) { Text(stringResource(R.string.widget_config_open_app)) }
            }
            else -> tvs.forEach { tv ->
                val selected = tv.id == selectedTvId
                Card(
                    onClick = { onSelect(tv) },
                    enabled = selectedTvId == null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (selected) {
                        CardDefaults.cardColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tv.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (selected) stringResource(R.string.widget_config_saving) else tv.ipAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensionResource(R.dimen.setup_progress_size)),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
