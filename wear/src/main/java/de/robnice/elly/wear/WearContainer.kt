package de.robnice.elly.wear

import android.content.Context
import de.robnice.elly.data.TvRepository
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.sensor.MotionPointerSensor
import de.robnice.elly.sensor.PointerMotion
import de.robnice.elly.tv.connection.WebOsConnector
import de.robnice.elly.tv.pairing.PairingManifest
import de.robnice.elly.tv.power.WakeOnLan
import de.robnice.elly.wear.data.AppIconStore
import de.robnice.elly.wear.data.WearSettings
import de.robnice.elly.wear.network.WifiNetwork
import de.robnice.elly.wear.remote.Haptics
import de.robnice.elly.wear.remote.QuickCommands
import de.robnice.elly.wear.sync.PhoneApp
import de.robnice.elly.wear.sync.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class WearContainer(context: Context) {

    private val appContext = context.applicationContext

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tvRepository = TvRepository(appContext)

    val settings = WearSettings(appContext)

    val appIcons = AppIconStore(appContext)

    val pointerSensor = MotionPointerSensor(appContext, PointerMotion.WRIST_AIM, POINTER_PERIOD_MICROS, background = true)

    val phoneState = PhoneState(appContext, tvRepository, settings, appIcons)

    val phoneApp = PhoneApp(appContext)

    val wifiNetwork = WifiNetwork(appContext)

    val remoteControl = RemoteControl(
        tvRepository,
        settings.demoMode,
        WebOsConnector(httpClient, trustAllCertificates = false),
        PairingManifest(appContext),
        WakeOnLan()
    )

    val haptics = Haptics(appContext, settings, applicationScope)

    val quickCommands = QuickCommands(appContext, applicationScope, remoteControl, wifiNetwork, settings, haptics)
}

// 100 Hz, the most Android allows without the permission for high sampling rates.
private const val POINTER_PERIOD_MICROS = 10_000
