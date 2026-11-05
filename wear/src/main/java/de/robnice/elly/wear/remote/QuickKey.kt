package de.robnice.elly.wear.remote

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.wear.R

// Keys offered as complications; the tile takes its keys from the layout set on the phone.
// shortLabel fits the few characters a complication shows next to the icon.
enum class QuickKey(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    @StringRes val shortLabel: Int,
    val action: WatchAction
) {
    POWER(R.drawable.ic_power, R.string.key_power, R.string.short_power, WatchAction.Key(RemoteKey.POWER)),
    MUTE(R.drawable.ic_volume_off, R.string.key_mute, R.string.short_mute, WatchAction.Key(RemoteKey.MUTE)),
    PLAY_PAUSE(R.drawable.ic_play_pause, R.string.key_play_pause, R.string.short_play_pause, WatchAction.PlayPause),
    VOLUME_DOWN(R.drawable.ic_minus, R.string.key_volume_down, R.string.short_volume_down, WatchAction.Key(RemoteKey.VOLUME_DOWN)),
    HOME(R.drawable.ic_home, R.string.key_home, R.string.key_home, WatchAction.Key(RemoteKey.HOME)),
    VOLUME_UP(R.drawable.ic_plus, R.string.key_volume_up, R.string.short_volume_up, WatchAction.Key(RemoteKey.VOLUME_UP));

    val id: String get() = name

    companion object {
        fun fromId(id: String?): QuickKey? = entries.firstOrNull { it.id == id }
    }
}
