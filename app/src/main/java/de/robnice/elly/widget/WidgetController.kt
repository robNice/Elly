package de.robnice.elly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import de.robnice.elly.R
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.data.model.PairedTv
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.remote.RemoteKey
import de.robnice.elly.remote.SendResult
import de.robnice.elly.ui.remote.KeyCatalog
import de.robnice.elly.ui.remote.messageRes
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

    suspend fun options(widgetId: Int): AllWidgetOptions = store.options(widgetId, tvRepository.activeTv.first()?.id)

    suspend fun configure(widgetId: Int, options: AllWidgetOptions) {
        store.setOptions(widgetId, options)
        render(widgetId)
    }

    fun kind(widgetId: Int): WidgetKind? = WidgetKind.of(manager.getAppWidgetInfo(widgetId)?.provider?.className)

    suspend fun remove(widgetIds: IntArray) {
        widgetIds.forEach { store.remove(it) }
    }

    suspend fun render(widgetId: Int) {
        val kind = kind(widgetId) ?: return
        val views = RemoteViews(context.packageName, kind.layout)
        if (kind == WidgetKind.ALL) {
            val options = options(widgetId)
            val name = tvFor(widgetId)?.name?.takeIf { options.showName }
            views.setViewVisibility(R.id.widget_all_title, if (name != null) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_all_title, name.orEmpty())
            val panelColor = options.panelColor.color
            views.setImageViewResource(R.id.widget_all_panel, if (panelColor == null) R.drawable.widget_panel_system else R.drawable.widget_panel_plain)
            views.setInt(R.id.widget_all_panel, "setColorFilter", panelColor?.let(context::getColor) ?: Color.TRANSPARENT)
            views.setInt(R.id.widget_all_panel, "setImageAlpha", options.panelOpacity * FULL_ALPHA / FULL_PERCENT)
            addKeyRows(views, widgetId, options)
        } else {
            kind.keys.forEach { (viewId, key) -> views.setOnClickPendingIntent(viewId, keyIntent(kind, widgetId, key)) }
        }
        manager.updateAppWidget(widgetId, views)
    }

    private fun addKeyRows(views: RemoteViews, widgetId: Int, options: AllWidgetOptions) {
        views.removeAllViews(R.id.widget_all_rows)
        AllWidgetLayout.rows(options.keys, options.columns).forEach { row ->
            val rowViews = RemoteViews(context.packageName, R.layout.widget_all_row)
            if (!options.rowBackground) rowViews.setInt(R.id.widget_all_row, "setBackgroundResource", 0)
            row.forEach { key ->
                val visual = KeyCatalog.visual(key)
                val keyViews = RemoteViews(context.packageName, R.layout.widget_all_key)
                visual.icon?.let { keyViews.setImageViewResource(R.id.widget_all_key, it) }
                visual.description?.let { keyViews.setContentDescription(R.id.widget_all_key, context.getString(it)) }
                keyViews.setOnClickPendingIntent(R.id.widget_all_key, keyIntent(WidgetKind.ALL, widgetId, key))
                rowViews.addView(R.id.widget_all_row, keyViews)
            }
            views.addView(R.id.widget_all_rows, rowViews)
        }
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
            .setData(Uri.parse("elly-widget://${kind.name}/$widgetId/${key.name}"))
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
        const val FULL_ALPHA = 255
        const val FULL_PERCENT = 100
    }
}
