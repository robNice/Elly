package de.robnice.elly

import android.content.Context
import de.robnice.elly.data.AppIconCache
import de.robnice.elly.data.AppIconFiller
import de.robnice.elly.data.BackgroundImageStore
import de.robnice.elly.widget.WidgetController
import de.robnice.elly.widget.WidgetStore
import de.robnice.elly.data.AppSlotsRepository
import de.robnice.elly.data.ScreenLayoutRepository
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.platform.DevicePlatform
import de.robnice.elly.remote.RemoteControl
import de.robnice.elly.sensor.MotionPointerSensor
import de.robnice.elly.tv.connection.WebOsConnector
import de.robnice.elly.tv.discovery.LocalNetworkInterfaces
import de.robnice.elly.tv.discovery.SsdpDiscovery
import de.robnice.elly.tv.discovery.SubnetScanner
import de.robnice.elly.tv.discovery.TvDiscovery
import de.robnice.elly.tv.pairing.PairingManifest
import de.robnice.elly.tv.pairing.PairingService
import de.robnice.elly.tv.power.WakeOnLan
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

    val backgroundImageStore = BackgroundImageStore(appContext)

    val tvDiscovery = TvDiscovery(
        SsdpDiscovery(appContext, networkInterfaces),
        SubnetScanner(networkInterfaces)
    )

    val pairingService = PairingService(pairingManifest, connector)

    val remoteControl = RemoteControl(tvRepository, settingsRepository, connector, pairingManifest, WakeOnLan())

    val appIconFiller = AppIconFiller(remoteControl, appSlotsRepository, screenLayoutRepository)

    val widgetController = WidgetController(
        appContext,
        applicationScope,
        remoteControl,
        tvRepository,
        settingsRepository,
        WidgetStore(appContext)
    )

    init {
        applicationScope.launch { appSlotsRepository.clearLegacySlots() }
    }
}
