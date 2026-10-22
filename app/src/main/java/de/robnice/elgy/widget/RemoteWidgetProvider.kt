package de.robnice.elgy.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import de.robnice.elgy.ElgyApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

abstract class RemoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync(context) { controller -> appWidgetIds.forEach { controller.render(it) } }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync(context) { it.remove(appWidgetIds) }
    }

    // Broadcasts must finish within about 10 s.
    private fun runAsync(context: Context, block: suspend (WidgetController) -> Unit) {
        val container = (context.applicationContext as ElgyApplication).container
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                withTimeoutOrNull(BROADCAST_TIMEOUT_MILLIS) { block(container.widgetController) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val BROADCAST_TIMEOUT_MILLIS = 9_000L
    }
}

class VolumeWidget : RemoteWidgetProvider()

class ChannelWidget : RemoteWidgetProvider()

class MediaWidget : RemoteWidgetProvider()

class PowerWidget : RemoteWidgetProvider()

class MuteWidget : RemoteWidgetProvider()

class ScreenWidget : RemoteWidgetProvider()
