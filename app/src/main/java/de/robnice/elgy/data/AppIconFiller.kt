package de.robnice.elgy.data

import de.robnice.elgy.data.model.CustomElementType
import de.robnice.elgy.remote.RemoteControl
import de.robnice.elgy.remote.SendResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// App keys without an icon (factory layouts, keys added offline) get it from the local cache or the TV.
class AppIconFiller(
    private val remoteControl: RemoteControl,
    private val appSlotsRepository: AppSlotsRepository,
    private val screenLayoutRepository: ScreenLayoutRepository
) {

    private val mutex = Mutex()
    private val unavailable = mutableSetOf<String>()

    suspend fun fillMissing() = mutex.withLock {
        val owner = appSlotsRepository.activeOwner.first() ?: return@withLock
        val screens = screenLayoutRepository.activeScreens.first()
        val missing = screens.values
            .flatMap { it.elements }
            .mapNotNull { element -> element.app?.takeIf { element.type == CustomElementType.APP } }
            .filter { it.showIcon && it.iconPath == null }
            .mapNotNull { it.appId }
            .filter { "$owner/$it" !in unavailable }
            .toSet()
        if (missing.isEmpty()) return@withLock

        val icons = missing.mapNotNull { appId -> appSlotsRepository.cachedIcon(appId)?.let { appId to it } }.toMap().toMutableMap()
        val toDownload = missing - icons.keys
        if (toDownload.isNotEmpty()) {
            val loaded = remoteControl.loadApps()
            if (loaded.result == SendResult.SENT) {
                val apps = loaded.apps.associateBy { it.id }
                toDownload.forEach { appId ->
                    val path = apps[appId]?.iconUrl
                        ?.let { remoteControl.downloadAppIcon(it) }
                        ?.let { appSlotsRepository.storeIcon(appId, it) }
                    if (path != null) icons[appId] = path else unavailable += "$owner/$appId"
                }
            }
        }
        if (icons.isEmpty()) return@withLock

        screens.forEach { (screen, layout) ->
            val targets = layout.elements.filter { element ->
                element.type == CustomElementType.APP && element.app?.iconPath == null && element.app?.appId in icons
            }
            if (targets.isEmpty()) return@forEach
            screenLayoutRepository.updateScreen(screen) { current ->
                targets.fold(current) { result, element ->
                    result.updateApp(element.id) { slot ->
                        slot?.takeIf { it.iconPath == null }?.copy(iconPath = icons[slot.appId]) ?: slot
                    }
                }
            }
        }
    }
}
