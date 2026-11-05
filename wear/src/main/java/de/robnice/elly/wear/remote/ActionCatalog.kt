package de.robnice.elly.wear.remote

import android.content.res.Resources
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.wear.R

// How a key with a given function looks: an icon or a short text, and a colour for keys that stand out.
// The description is read out by screen readers and named on the tile.
data class ActionVisual(
    val description: String,
    @param:DrawableRes val icon: Int? = null,
    val text: String? = null,
    @param:ColorRes val container: Int? = null,
    @param:ColorRes val content: Int? = null
)

object ActionCatalog {

    fun visual(action: WatchAction, resources: Resources): ActionVisual = when (action) {
        is WatchAction.Key -> key(action.key, resources)
        WatchAction.PlayPause -> ActionVisual(resources.getString(R.string.key_play_pause), R.drawable.ic_play_pause)
        is WatchAction.App -> ActionVisual(resources.getString(R.string.key_app, action.title), text = action.title)
    }

    private fun key(key: RemoteKey, res: Resources): ActionVisual {
        fun icon(description: Int, icon: Int) = ActionVisual(res.getString(description), icon)
        fun text(description: Int, label: Int) = ActionVisual(res.getString(description), text = res.getString(label))
        fun color(description: Int, color: Int) = ActionVisual(res.getString(description), container = color)
        return when (key) {
            RemoteKey.POWER -> ActionVisual(res.getString(R.string.key_power), R.drawable.ic_power, container = R.color.power_key, content = R.color.on_power_key)
            RemoteKey.DIGIT_0, RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3, RemoteKey.DIGIT_4,
            RemoteKey.DIGIT_5, RemoteKey.DIGIT_6, RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9 -> {
                val digit = key.name.removePrefix("DIGIT_")
                ActionVisual(res.getString(R.string.key_digit, digit), text = digit)
            }
            RemoteKey.MORE_ACTIONS -> text(R.string.key_more_actions, R.string.key_label_more)
            RemoteKey.VOLUME_UP -> icon(R.string.key_volume_up, R.drawable.ic_plus)
            RemoteKey.VOLUME_DOWN -> icon(R.string.key_volume_down, R.drawable.ic_minus)
            RemoteKey.MUTE -> icon(R.string.key_mute, R.drawable.ic_volume_off)
            RemoteKey.CHANNEL_UP -> icon(R.string.key_channel_up, R.drawable.ic_chevron_up)
            RemoteKey.CHANNEL_DOWN -> icon(R.string.key_channel_down, R.drawable.ic_chevron_down)
            RemoteKey.LIVE_TV -> icon(R.string.key_live_tv, R.drawable.ic_live_tv)
            RemoteKey.HOME -> icon(R.string.key_home, R.drawable.ic_home)
            RemoteKey.SETTINGS -> icon(R.string.key_settings, R.drawable.ic_settings)
            RemoteKey.UP -> icon(R.string.key_up, R.drawable.ic_chevron_up)
            RemoteKey.DOWN -> icon(R.string.key_down, R.drawable.ic_chevron_down)
            RemoteKey.LEFT -> icon(R.string.key_left, R.drawable.ic_chevron_left)
            RemoteKey.RIGHT -> icon(R.string.key_right, R.drawable.ic_chevron_right)
            RemoteKey.OK -> text(R.string.key_ok, R.string.key_ok)
            RemoteKey.BACK -> icon(R.string.key_back, R.drawable.ic_back)
            RemoteKey.GUIDE -> text(R.string.key_guide, R.string.key_label_guide)
            RemoteKey.RED -> color(R.string.key_red, R.color.key_color_red)
            RemoteKey.GREEN -> color(R.string.key_green, R.color.key_color_green)
            RemoteKey.YELLOW -> color(R.string.key_yellow, R.color.key_color_yellow)
            RemoteKey.BLUE -> color(R.string.key_blue, R.color.key_color_blue)
            RemoteKey.PLAY -> icon(R.string.key_play, R.drawable.ic_play)
            RemoteKey.PAUSE -> icon(R.string.key_pause, R.drawable.ic_pause)
            RemoteKey.REWIND -> icon(R.string.key_rewind, R.drawable.ic_fast_rewind)
            RemoteKey.FAST_FORWARD -> icon(R.string.key_fast_forward, R.drawable.ic_fast_forward)
            RemoteKey.STOP -> icon(R.string.key_stop, R.drawable.ic_stop)
            RemoteKey.RECORD -> ActionVisual(res.getString(R.string.key_record), R.drawable.ic_record, content = R.color.key_color_red)
            RemoteKey.SCREEN_OFF -> icon(R.string.key_screen_off, R.drawable.ic_screen_off)
            RemoteKey.SCREEN_ON -> icon(R.string.key_screen_on, R.drawable.ic_screen_on)
            RemoteKey.INPUT -> icon(R.string.key_input, R.drawable.ic_input)
            // Not offered for the watch, see WatchAction.ASSIGNABLE_KEYS.
            RemoteKey.AUDIO_TRACK, RemoteKey.SUBTITLE, RemoteKey.KEYBOARD ->
                ActionVisual(key.name, text = key.name)
        }
    }
}
