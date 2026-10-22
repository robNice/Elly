package de.robnice.elgy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import android.widget.Toast
import de.robnice.elgy.data.SettingsRepository
import de.robnice.elgy.data.TvRepository
import de.robnice.elgy.data.model.PairedTv
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.remote.RemoteKey
import de.robnice.elgy.remote.SendResult
import de.robnice.elgy.ui.remote.messageRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetController(
    context: Context,
    private val scope: CoroutineScope,
    private val remoteControl: RemoteControl,
    private val tvRepository: TvRepository,
    private val settingsRepository: SettingsRepository,
    private val store: WidgetStore
) {

    private val context = context.applicationContext
    private val manager = AppWidgetManager.getInstance(this.context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var idleDisconnect: Job? = null
    private var lastUse = 0L

    @Volatile
    var appVisible = false

    suspend fun press(widgetId: Int, key: RemoteKey) {
        if (settingsRepository.keyHaptics.first()) vibrate()
        val tv = tvFor(widgetId)
        disconnectIfIdle()
        val result = remoteControl.send(key, tv)
        result.messageRes()?.takeIf { result != SendResult.DEMO }?.let(::toast)
        scheduleIdleDisconnect()
    }

    suspend fun bind(widgetId: Int, tvId: String) {
        store.setTv(widgetId, tvId)
        render(widgetId)
    }

    suspend fun remove(widgetIds: IntArray) {
        widgetIds.forEach { store.remove(it) }
    }

    fun render(widgetId: Int) {
        val kind = WidgetKind.of(manager.getAppWidgetInfo(widgetId)?.provider?.className) ?: return
        val views = RemoteViews(context.packageName, kind.layout)
        kind.keys.forEach { (viewId, key) -> views.setOnClickPendingIntent(viewId, keyIntent(kind, widgetId, key)) }
        manager.updateAppWidget(widgetId, views)
    }

    private suspend fun tvFor(widgetId: Int): PairedTv? {
        val tvs = tvRepository.pairedTvs.first()
        val id = store.tvId(widgetId)
        return tvs.firstOrNull { it.id == id } ?: tvRepository.activeTv.first()
    }

    // Widgets open a TV connection without the app; close it again once nobody uses it.
    private fun scheduleIdleDisconnect() {
        lastUse = SystemClock.elapsedRealtime()
        idleDisconnect?.cancel()
        idleDisconnect = scope.launch {
            delay(IDLE_DISCONNECT_MILLIS)
            if (!appVisible) remoteControl.disconnect()
        }
    }

    // The idle timer does not run while the system has the process frozen, so the TV may have dropped
    // a connection that still looks open here.
    private suspend fun disconnectIfIdle() {
        if (appVisible || lastUse == 0L) return
        if (SystemClock.elapsedRealtime() - lastUse >= IDLE_DISCONNECT_MILLIS) remoteControl.disconnect()
    }

    private fun keyIntent(kind: WidgetKind, widgetId: Int, key: RemoteKey): PendingIntent {
        val intent = Intent(context, WidgetKeyService::class.java)
            .setData(Uri.parse("elgy-widget://${kind.name}/$widgetId/${key.name}"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .putExtra(WidgetKeyService.EXTRA_KEY, key.name)
        return PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }

    private fun toast(message: Int) {
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    private companion object {
        const val IDLE_DISCONNECT_MILLIS = 20_000L
        const val VIBRATION_MILLIS = 20L
    }
}
