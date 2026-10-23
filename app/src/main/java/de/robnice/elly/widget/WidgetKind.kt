package de.robnice.elly.widget

import androidx.annotation.LayoutRes
import de.robnice.elly.R
import de.robnice.elly.remote.RemoteKey

enum class WidgetKind(
    @param:LayoutRes val layout: Int,
    val provider: Class<out RemoteWidgetProvider>,
    val keys: Map<Int, RemoteKey>
) {
    VOLUME(
        R.layout.widget_volume,
        VolumeWidget::class.java,
        mapOf(
            R.id.widget_volume_up to RemoteKey.VOLUME_UP,
            R.id.widget_volume_down to RemoteKey.VOLUME_DOWN
        )
    ),
    CHANNEL(
        R.layout.widget_channel,
        ChannelWidget::class.java,
        mapOf(
            R.id.widget_channel_up to RemoteKey.CHANNEL_UP,
            R.id.widget_channel_down to RemoteKey.CHANNEL_DOWN
        )
    ),
    MEDIA(
        R.layout.widget_media,
        MediaWidget::class.java,
        mapOf(
            R.id.widget_rewind to RemoteKey.REWIND,
            R.id.widget_play to RemoteKey.PLAY,
            R.id.widget_pause to RemoteKey.PAUSE,
            R.id.widget_fast_forward to RemoteKey.FAST_FORWARD,
            R.id.widget_stop to RemoteKey.STOP
        )
    ),
    POWER(
        R.layout.widget_power,
        PowerWidget::class.java,
        mapOf(R.id.widget_power to RemoteKey.POWER)
    ),
    MUTE(
        R.layout.widget_mute,
        MuteWidget::class.java,
        mapOf(R.id.widget_mute to RemoteKey.MUTE)
    ),
    SCREEN(
        R.layout.widget_screen,
        ScreenWidget::class.java,
        mapOf(
            R.id.widget_screen_off to RemoteKey.SCREEN_OFF,
            R.id.widget_screen_on to RemoteKey.SCREEN_ON
        )
    ),

    // Keys are laid out at runtime from AllWidgetLayout and the keys chosen for the widget.
    ALL(R.layout.widget_all, AllWidget::class.java, emptyMap());

    companion object {
        fun of(provider: String?): WidgetKind? = entries.firstOrNull { it.provider.name == provider }
    }
}
