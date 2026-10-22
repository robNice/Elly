package de.robnice.elgy.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import de.robnice.elgy.ElgyApplication
import de.robnice.elgy.R
import de.robnice.elgy.remote.RemoteKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Samsung (and Android's background network chain) cut a background app off the network a few
// seconds after it leaves the foreground, also while a widget broadcast runs. A foreground
// service started from the widget click keeps network access until the key is sent.
class WidgetKeyService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val key = intent?.getStringExtra(EXTRA_KEY)?.let { runCatching { RemoteKey.valueOf(it) }.getOrNull() }
        if (key == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val container = (application as ElgyApplication).container
        container.applicationScope.launch {
            try {
                withTimeoutOrNull(PRESS_TIMEOUT_MILLIS) { container.widgetController.press(widgetId, key) }
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.widget_service_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.widget_service_sending))
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val EXTRA_KEY = "key"
        private const val CHANNEL_ID = "widget_keys"
        private const val NOTIFICATION_ID = 1
        private const val PRESS_TIMEOUT_MILLIS = 20_000L
    }
}
