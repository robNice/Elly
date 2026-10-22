package de.robnice.elgy.ui.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.robnice.elgy.R
import de.robnice.elgy.data.model.AppSlot
import de.robnice.elgy.ui.components.KeyStyle
import de.robnice.elgy.ui.components.LocalKeyRounding
import de.robnice.elgy.ui.components.RemoteKeyButton
import de.robnice.elgy.ui.components.keyShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val APP_LABEL_MIN_SP = 8f
private const val APP_LABEL_MAX_SP = 14f
private const val APP_LABEL_STEP_SP = 0.5f

@Composable
fun AppKey(slot: AppSlot, onPress: () -> Unit, modifier: Modifier = Modifier) {
    val icon = if (slot.showIcon) rememberAppIcon(slot.iconPath) else null
    val showText = slot.showText || icon == null
    val label = slot.label ?: slot.appId.orEmpty()
    val iconWithText = icon != null && showText

    RemoteKeyButton(
        contentDescription = label,
        onPress = onPress,
        modifier = modifier.height(dimensionResource(R.dimen.key_app_height)),
        shape = keyShape(LocalKeyRounding.current),
        style = KeyStyle.LIGHT
    ) {
        Row(
            modifier = Modifier
                .then(if (iconWithText) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = dimensionResource(R.dimen.key_app_label_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.app_key_content_spacing)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(if (showText) R.dimen.app_key_icon_size_small else R.dimen.app_key_icon_size))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.app_key_icon_corner)))
                )
            }
            if (showText) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(color = appLabelColor(slot), fontWeight = FontWeight.ExtraBold),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = APP_LABEL_MIN_SP.sp,
                        maxFontSize = APP_LABEL_MAX_SP.sp,
                        stepSize = APP_LABEL_STEP_SP.sp
                    ),
                    modifier = Modifier.weight(1f, fill = iconWithText)
                )
            }
        }
    }
}

@Composable
fun appLabelColor(slot: AppSlot): Color {
    slot.textColor?.let { return Color(it) }
    return when (slot.appId) {
        "netflix" -> colorResource(R.color.brand_netflix)
        "amazon" -> colorResource(R.color.brand_prime_video)
        else -> colorResource(R.color.key_content_on_light)
    }
}

@Composable
private fun rememberAppIcon(path: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = path?.let {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(File(it).absolutePath)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    return bitmap
}
