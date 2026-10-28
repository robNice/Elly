package de.robnice.elly.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import de.robnice.elly.R
import de.robnice.elly.data.model.AppBackground
import de.robnice.elly.data.model.ScreenBackground
import de.robnice.elly.ui.remote.ScreenBackgroundImage
import de.robnice.elly.ui.remote.ScreenBackgrounds

@Composable
fun BackgroundSection(current: AppBackground, onSelect: (ScreenBackground) -> Unit, onImage: (Uri) -> Unit, modifier: Modifier) {
    val spacing = dimensionResource(R.dimen.setup_item_spacing)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onImage)
    }
    var open by remember { mutableStateOf(false) }
    // Only the background in use is shown; tapping it opens all of them.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(text = stringResource(R.string.settings_background), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        BackgroundOption(
            background = current.type,
            customImage = current.image,
            selected = true,
            onClick = { open = true },
            modifier = Modifier.width(dimensionResource(R.dimen.settings_current_background_width))
        )
    }
    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.setup_card_padding)),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    ScreenBackground.entries.chunked(BACKGROUND_COLUMNS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            row.forEach { background ->
                                BackgroundOption(
                                    background = background,
                                    customImage = current.image,
                                    selected = background == current.type,
                                    onClick = {
                                        open = false
                                        if (background == ScreenBackground.CUSTOM) {
                                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        } else {
                                            onSelect(background)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(BACKGROUND_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundOption(
    background: ScreenBackground,
    customImage: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.custom_editor_corner))
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BACKGROUND_PREVIEW_ASPECT)
                .clip(shape)
                .background(colorResource(R.color.remote_background))
                .border(
                    dimensionResource(if (selected) R.dimen.custom_editor_selected_outline else R.dimen.custom_editor_outline),
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    shape
                ),
            contentAlignment = Alignment.Center
        ) {
            ScreenBackgroundImage(background, customImage, Modifier.fillMaxSize(), thumbnail = true)
            if (background == ScreenBackground.CUSTOM && customImage == null) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        FittedLabel(text = stringResource(ScreenBackgrounds.title(background)), color = MaterialTheme.colorScheme.onSurface)
    }
}

private const val BACKGROUND_COLUMNS = 4
private const val BACKGROUND_PREVIEW_ASPECT = 0.46f
