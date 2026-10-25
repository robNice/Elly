package de.robnice.elly.epg

import android.content.Context
import de.robnice.elly.data.RootAccessRepository
import de.robnice.elly.data.SettingsRepository
import de.robnice.elly.data.TvRepository
import de.robnice.elly.tv.ssh.RootAccessService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient

// Decides whether the program guide can be used (demo mode or SSH set up for the active TV) and which source serves it.
@OptIn(ExperimentalCoroutinesApi::class)
class EpgAccess(
    private val context: Context,
    private val tvRepository: TvRepository,
    private val settingsRepository: SettingsRepository,
    rootAccessRepository: RootAccessRepository,
    private val rootAccessService: RootAccessService,
    private val httpClient: OkHttpClient
) {

    private val rootAccessSet: Flow<Boolean> = tvRepository.activeTv
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(false) else rootAccessRepository.access(id).map { it != null } }

    val available: Flow<Boolean> = combine(settingsRepository.demoMode, rootAccessSet) { demo, root -> demo || root }
        .distinctUntilChanged()

    suspend fun source(): EpgSource? {
        if (settingsRepository.demoMode.first()) return DemoEpgSource(context)
        if (!available.first()) return null
        val tv = tvRepository.activeTv.first() ?: return null
        return RootedEpgSource(tv, rootAccessService, httpClient)
    }
}
