package de.robnice.elgy.ui.remote

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.robnice.elgy.R
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.ui.components.KeyStyle

data class KeyVisual(
    @param:StringRes val description: Int? = null,
    @param:DrawableRes val icon: Int? = null,
    @param:StringRes val label: Int? = null,
    val text: String? = null,
    val style: KeyStyle = KeyStyle.DARK,
    @param:ColorRes val iconTint: Int? = null,
    val repeatOnHold: Boolean = false
)

object KeyCatalog {

    fun visual(key: RemoteKey): KeyVisual = when (key) {
        RemoteKey.POWER -> KeyVisual(R.string.key_power, R.drawable.ic_power, style = KeyStyle.POWER)
        RemoteKey.DIGIT_0, RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3, RemoteKey.DIGIT_4,
        RemoteKey.DIGIT_5, RemoteKey.DIGIT_6, RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9 ->
            KeyVisual(text = key.name.removePrefix("DIGIT_"))
        RemoteKey.MORE_ACTIONS -> KeyVisual(R.string.key_more_actions, label = R.string.key_label_more)
        RemoteKey.VOLUME_UP -> KeyVisual(R.string.key_volume_up, R.drawable.ic_plus, repeatOnHold = true)
        RemoteKey.VOLUME_DOWN -> KeyVisual(R.string.key_volume_down, R.drawable.ic_minus, repeatOnHold = true)
        RemoteKey.MUTE -> KeyVisual(R.string.key_mute, R.drawable.ic_volume_off)
        RemoteKey.CHANNEL_UP -> KeyVisual(R.string.key_channel_up, R.drawable.ic_chevron_up, repeatOnHold = true)
        RemoteKey.CHANNEL_DOWN -> KeyVisual(R.string.key_channel_down, R.drawable.ic_chevron_down, repeatOnHold = true)
        RemoteKey.LIVE_TV -> KeyVisual(R.string.key_live_tv, R.drawable.ic_live_tv)
        RemoteKey.HOME -> KeyVisual(R.string.key_home, R.drawable.ic_home)
        RemoteKey.SETTINGS -> KeyVisual(R.string.key_settings, R.drawable.ic_settings)
        RemoteKey.UP -> KeyVisual(R.string.key_up, R.drawable.ic_chevron_up, repeatOnHold = true)
        RemoteKey.DOWN -> KeyVisual(R.string.key_down, R.drawable.ic_chevron_down, repeatOnHold = true)
        RemoteKey.LEFT -> KeyVisual(R.string.key_left, R.drawable.ic_chevron_left, repeatOnHold = true)
        RemoteKey.RIGHT -> KeyVisual(R.string.key_right, R.drawable.ic_chevron_right, repeatOnHold = true)
        RemoteKey.OK -> KeyVisual(R.string.key_ok, label = R.string.key_ok)
        RemoteKey.BACK -> KeyVisual(R.string.key_back, R.drawable.ic_back)
        RemoteKey.GUIDE -> KeyVisual(R.string.key_guide, label = R.string.key_label_guide)
        RemoteKey.INPUT -> KeyVisual(R.string.key_input, R.drawable.ic_input)
        RemoteKey.KEYBOARD -> KeyVisual(R.string.key_keyboard, R.drawable.ic_keyboard)
        RemoteKey.RED -> KeyVisual(R.string.key_red, style = KeyStyle.RED)
        RemoteKey.GREEN -> KeyVisual(R.string.key_green, style = KeyStyle.GREEN)
        RemoteKey.YELLOW -> KeyVisual(R.string.key_yellow, style = KeyStyle.YELLOW)
        RemoteKey.BLUE -> KeyVisual(R.string.key_blue, style = KeyStyle.BLUE)
        RemoteKey.PLAY -> KeyVisual(R.string.key_play, R.drawable.ic_play)
        RemoteKey.PAUSE -> KeyVisual(R.string.key_pause, R.drawable.ic_pause)
        RemoteKey.REWIND -> KeyVisual(R.string.key_rewind, R.drawable.ic_fast_rewind)
        RemoteKey.FAST_FORWARD -> KeyVisual(R.string.key_fast_forward, R.drawable.ic_fast_forward)
        RemoteKey.RECORD -> KeyVisual(R.string.key_record, R.drawable.ic_record, iconTint = R.color.key_color_red)
        RemoteKey.STOP -> KeyVisual(R.string.key_stop, R.drawable.ic_stop)
        RemoteKey.SCREEN_OFF -> KeyVisual(R.string.key_screen_off, R.drawable.ic_screen_off)
        RemoteKey.SCREEN_ON -> KeyVisual(R.string.key_screen_on, R.drawable.ic_screen_on)
    }

    @Composable
    fun name(key: RemoteKey): String {
        val visual = visual(key)
        return visual.description?.let { stringResource(it) } ?: visual.text.orEmpty()
    }
}
