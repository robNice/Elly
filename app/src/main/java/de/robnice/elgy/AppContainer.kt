package de.robnice.elgy

import android.content.Context
import de.robnice.elgy.data.AppIconCache
import de.robnice.elgy.data.AppIconFiller
import de.robnice.elgy.data.AppSlotsRepository
import de.robnice.elgy.data.ScreenLayoutRepository
import de.robnice.elgy.data.SettingsRepository
import de.robnice.elgy.data.TvRepository
import de.robnice.elgy.platform.DevicePlatform
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.sensor.MotionPointerSensor
import de.robnice.elgy.tv.connection.WebOsConnector
import de.robnice.elgy.tv.discovery.LocalNetworkInterfaces
import de.robnice.elgy.tv.discovery.SsdpDiscovery
import de.robnice.elgy.tv.discovery.SubnetScanner
import de.robnice.elgy.tv.discovery.TvDiscovery
import de.robnice.elgy.tv.pairing.PairingManifest
import de.robnice.elgy.tv.pairing.PairingService
import de.robnice.elgy.tv.power.WakeOnLan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val networkInterfaces = LocalNetworkInterfaces()
    private val pairingManifest = PairingManifest(appContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val connector = WebOsConnector(httpClient, trustAllCertificates = DevicePlatform.isDebugEmulator)

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val motionPointerAvailable: Boolean = MotionPointerSensor.isSupported(appContext)

    val tvRepository = TvRepository(appContext)

    val settingsRepository = SettingsRepository(appContext, motionPointerAvailable)

    val appSlotsRepository = AppSlotsRepository(
        appContext,
        tvRepository,
        settingsRepository,
        AppIconCache(appContext)
    )

    val screenLayoutRepository = ScreenLayoutRepository(appContext, appSlotsRepository.activeOwner)

    val tvDiscovery = TvDiscovery(
        SsdpDiscovery(appContext, networkInterfaces),
        SubnetScanner(networkInterfaces)
    )

    val pairingService = PairingService(pairingManifest, connector)

    val remoteControl = RemoteControl(tvRepository, settingsRepository, connector, pairingManifest, WakeOnLan())

    val appIconFiller = AppIconFiller(remoteControl, appSlotsRepository, screenLayoutRepository)

    init {
        applicationScope.launch { appSlotsRepository.clearLegacySlots() }
    }
}
