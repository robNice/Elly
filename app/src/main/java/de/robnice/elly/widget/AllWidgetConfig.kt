package de.robnice.elly.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.robnice.elly.R
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.ui.remote.KeyCatalog
import kotlin.math.roundToInt

private val groupTitles = listOf(
    R.string.widget_config_group_power,
    R.string.widget_config_group_volume,
    R.string.widget_config_group_media
)

@Composable
fun AllWidgetConfig(
    tvs: List<PairedTv>?,
    options: AllWidgetOptions,
    onChange: (AllWidgetOptions) -> Unit,
    saving: Boolean,
    onDone: () -> Unit,
    onOpenApp: () -> Unit
) {
    val enabled = !saving
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.setup_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
    ) {
        Text(text = stringResource(R.string.widget_config_title), style = MaterialTheme.typography.headlineSmall)
        when {
            tvs == null -> return@Column
            tvs.isEmpty() -> {
                Text(
                    text = stringResource(R.string.widget_config_no_tvs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onOpenApp) { Text(stringResource(R.string.widget_config_open_app)) }
                return@Column
            }
            else -> tvs.forEach { tv -> TvOption(tv, tv.id == options.tvId, enabled) { onChange(options.copy(tvId = tv.id)) } }
        }

        Heading(stringResource(R.string.widget_config_keys))
        AllWidgetLayout.groups.zip(groupTitles).forEach { (group, title) ->
            SectionTitle(stringResource(title))
            Card(modifier = Modifier.fillMaxWidth()) {
                group.forEach { key ->
                    KeyToggle(key, key in options.keys, enabled) {
                        onChange(options.copy(keys = if (key in options.keys) options.keys - key else options.keys + key))
                    }
                }
            }
        }

        Heading(stringResource(R.string.widget_config_look))
        SliderOption(
            title = stringResource(R.string.widget_config_columns),
            value = options.columns,
            valueText = options.columns.toString(),
            range = AllWidgetLayout.MIN_COLUMNS..AllWidgetLayout.MAX_COLUMNS,
            step = 1,
            enabled = enabled
        ) { onChange(options.copy(columns = it)) }
        SwitchOption(stringResource(R.string.widget_config_show_name), options.showName, enabled) {
            onChange(options.copy(showName = it))
        }
        SwitchOption(stringResource(R.string.widget_config_row_background), options.rowBackground, enabled) {
            onChange(options.copy(rowBackground = it))
        }
        SectionTitle(stringResource(R.string.widget_config_panel_color))
        Card(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.setup_item_spacing))
            ) {
                WidgetPanelColor.entries.forEach { color ->
                    ColorSwatch(color, color == options.panelColor, enabled) { onChange(options.copy(panelColor = color)) }
                }
            }
        }
        SliderOption(
            title = stringResource(R.string.widget_config_panel_opacity),
            value = options.panelOpacity,
            valueText = stringResource(R.string.widget_config_percent, options.panelOpacity),
            range = 0..FULL_PERCENT,
            step = OPACITY_STEP,
            enabled = enabled
        ) { onChange(options.copy(panelOpacity = it)) }

        Button(onClick = onDone, enabled = enabled && options.tvId != null && options.keys.isNotEmpty()) {
            Text(stringResource(if (saving) R.string.widget_config_saving else R.string.widget_config_done))
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.setup_section_spacing))
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.setup_item_spacing))
    )
}

@Composable
private fun TvOption(tv: PairedTv, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onSelect)
                .padding(dimensionResource(R.dimen.setup_card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.setup_item_spacing)))
            Column {
                Text(text = tv.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = tv.ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeyToggle(key: RemoteKey, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val visual = KeyCatalog.visual(key)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = dimensionResource(R.dimen.setup_card_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visual.icon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.onboarding_item_icon_size))
            )
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.onboarding_item_icon_spacing)))
        Text(text = KeyCatalog.name(key), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SwitchOption(title: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onChange(!checked) }
                .padding(dimensionResource(R.dimen.setup_card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
private fun SliderOption(
    title: String,
    value: Int,
    valueText: String,
    range: IntRange,
    step: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.setup_card_padding))) {
            Row(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.setup_card_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(text = valueText, style = MaterialTheme.typography.titleMedium)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(((it / step).roundToInt() * step).coerceIn(range)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / step - 1,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: WidgetPanelColor, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    val description = stringResource(color.title)
    val border = dimensionResource(if (selected) R.dimen.widget_config_swatch_border_selected else R.dimen.widget_config_swatch_border)
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.widget_config_swatch_size))
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onSelect)
            .border(border, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
            .padding(dimensionResource(R.dimen.widget_config_swatch_border_selected))
            .background(colorResource(color.color ?: R.color.widget_background), CircleShape)
    )
}

private const val OPACITY_STEP = 5
private const val FULL_PERCENT = 100
