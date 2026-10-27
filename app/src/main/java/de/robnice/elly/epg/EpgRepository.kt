package de.robnice.elly.epg

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

// Keeps the guide of the active TV on the phone and refreshes it from the TV.
// Refresh plan: the near window (today until tomorrow) on every call, at most every 10 minutes;
// the days after that every 6 hours, day by day until the TV has no more data; channel lists daily.
@OptIn(ExperimentalCoroutinesApi::class)
class EpgRepository(
    private val files: EpgFiles,
    private val access: EpgAccess,
    private val activeOwner: Flow<String?>,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private class Loaded(val owner: String, val cache: EpgCache, val logos: Map<String, String>)

    private val loaded = MutableStateFlow<Loaded?>(null)
    private val loadMutex = Mutex()
    private val refreshMutex = Mutex()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun guide(listId: String): Flow<EpgGuide?> = activeOwner.distinctUntilChanged().flatMapLatest { owner ->
        if (owner == null) {
            flowOf(null)
        } else {
            flow<EpgGuide?> {
                load(owner)
                emitAll(loaded.map { current ->
                    current?.takeIf { it.owner == owner }?.let { it.cache.guide(listId)?.copy(logos = it.logos) }
                })
            }
        }
    }.distinctUntilChanged()

    suspend fun refresh(listIds: Set<String>) {
        if (listIds.isEmpty() || !refreshMutex.tryLock()) return
        try {
            val owner = activeOwner.first() ?: return
            val source = access.source() ?: return
            _refreshing.value = true
            val started = clock()
            update(owner, source, listIds)
            loaded.value?.let { current ->
                val programs = current.cache.programs.values.sumOf { it.size }
                Log.i(TAG, "Guide refreshed in ${clock() - started} ms: ${current.cache.lists.values.sumOf { it.size }} channels, $programs programs, ${current.logos.size} logos")
            }
            _failed.value = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Guide refresh failed: ${e.message}")
            _failed.value = true
        } finally {
            _refreshing.value = false
            refreshMutex.unlock()
        }
    }

    private val _reservations = MutableStateFlow<List<EpgReservation>>(emptyList())
    val reservations: StateFlow<List<EpgReservation>> = _reservations.asStateFlow()

    private val _canRecord = MutableStateFlow(false)
    val canRecord: StateFlow<Boolean> = _canRecord.asStateFlow()

    private var reservationChannels: List<EpgChannel> = emptyList()

    suspend fun videoInfo(channel: EpgChannel): EpgVideoInfo? = try {
        access.source()?.videoInfo(channel)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Video info not loaded: ${e.message}")
        null
    }

    private val programImages = LruCache<String, ByteArray>(PROGRAM_IMAGE_CACHE_SIZE)

    suspend fun programImage(url: String): ByteArray? {
        programImages.get(url)?.let { return it }
        return try {
            access.source()?.programImage(url)?.also { programImages.put(url, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Program image not loaded: ${e.message}")
            null
        }
    }

    // Null without root access; throws when the TV cannot be asked.
    suspend fun audioTracks(): AudioTracks? = access.source()?.audioTracks()

    suspend fun selectAudioTrack(id: Int, channelId: String?): Boolean =
        access.source()?.selectAudioTrack(id, channelId) ?: false

    suspend fun setAudioDescription(enabled: Boolean) {
        access.source()?.setAudioDescription(enabled)
    }

    // Plans or removes a reminder or recording on the TV and reloads the plan afterwards.
    suspend fun perform(action: EpgAction) {
        val source = access.source() ?: throw EpgActionException(null)
        when (action) {
            is EpgAction.Remind -> plan(source, action.program, ReservationType.REMINDER) { source.remind(action.channel, action.program) }
            is EpgAction.Record -> plan(source, action.program, ReservationType.RECORDING) { source.record(action.channel, action.program) }
            is EpgAction.Cancel -> cancel(source, action.reservation)
            is EpgAction.StopRecording -> {
                source.stopRecording(action.channel)
                _reservations.update { list -> list.filterNot { it.id == action.reservation.id } }
            }
        }
        loadReservations(source, withStorage = false)
    }

    // If the TV refuses because the same reminder or recording already exists ("Crash schedule"), that counts as done.
    private suspend fun plan(source: EpgSource, program: EpgProgram, type: ReservationType, add: suspend () -> Unit) {
        try {
            add()
        } catch (e: EpgActionException) {
            loadReservations(source, withStorage = false)
            if (_reservations.value.none { it.type == type && it.belongsTo(program) }) throw e
        }
    }

    // A reservation already gone from the TV counts as deleted; the TV then answers "Invalid schedule ID".
    private suspend fun cancel(source: EpgSource, reservation: EpgReservation) {
        try {
            source.cancel(reservation)
        } catch (e: EpgActionException) {
            loadReservations(source, withStorage = false)
            if (_reservations.value.any { it.id == reservation.id }) throw e
        }
        _reservations.update { list -> list.filterNot { it.id == reservation.id } }
    }

    // Reservations and the recording storage are extras; failing to read them does not fail the guide.
    private suspend fun loadReservations(source: EpgSource, withStorage: Boolean = true) {
        val now = clock()
        try {
            _reservations.value = source.reservations(reservationChannels, now - EpgCache.DAY_MILLIS, now + EpgCache.KEEP_FUTURE_MILLIS)
            if (withStorage) _canRecord.value = source.canRecord()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Reservations not loaded: ${e.message}")
        }
    }

    suspend fun removeOwner(owner: String) {
        loadMutex.withLock {
            if (loaded.value?.owner == owner) loaded.value = null
            files.removeOwner(owner)
        }
    }

    private suspend fun update(owner: String, source: EpgSource, listIds: Set<String>) {
        var cache = load(owner).cache
        val now = clock()

        var newChannels = false
        listIds.forEach { id ->
            val age = now - (cache.channelsUpdated[id] ?: 0L)
            if (cache.lists[id] == null || age > CHANNELS_MAX_AGE_MILLIS) {
                newChannels = newChannels || cache.lists[id] == null
                cache = cache.withChannels(id, source.channels(id), now)
                publish(owner, cache)
            }
        }
        val channels = listIds.flatMap { cache.lists[it].orEmpty() }.distinctBy { it.signalId }
        val signals = channels.map { it.signalId }.toSet()

        val nearFrom = startOfDay(now)
        val nearTo = now + NEAR_AHEAD_MILLIS
        if (newChannels || now - cache.nearUpdated > NEAR_MIN_INTERVAL_MILLIS) {
            cache = cache.merge(source.programs(channels, nearFrom, nearTo), signals, nearFrom, nearTo)
                .copy(nearUpdated = now)
                .pruned(now)
            publish(owner, cache)
            files.save(owner, cache)
        }

        reservationChannels = channels
        loadReservations(source)

        if (newChannels || now - cache.farUpdated > FAR_MAX_AGE_MILLIS) {
            val limit = now + EpgCache.KEEP_FUTURE_MILLIS
            var from = nearTo
            while (from < limit) {
                val to = minOf(from + EpgCache.DAY_MILLIS, limit)
                val fetched = source.programs(channels, from, to)
                cache = cache.merge(fetched, signals, from, to)
                publish(owner, cache)
                if (fetched.isEmpty()) break
                from = to
            }
            cache = cache.copy(farUpdated = now).pruned(now)
            publish(owner, cache)
            files.save(owner, cache)
        }

        // Logos come last and quietly; the guide is complete without them.
        _refreshing.value = false
        val missingLogos = channels.filter { it.id !in (loaded.value?.logos.orEmpty()) && (it.logoPath != null || it.logoUrl != null) }
        if (missingLogos.isNotEmpty()) {
            source.logos(missingLogos).forEach { (channelId, bytes) -> files.storeLogo(owner, channelId, bytes) }
            publish(owner, cache, files.logos(owner, cache.lists.values.flatten()))
        }
    }

    private suspend fun load(owner: String): Loaded = loadMutex.withLock {
        loaded.value?.takeIf { it.owner == owner } ?: run {
            val cache = files.load(owner).pruned(clock())
            Loaded(owner, cache, files.logos(owner, cache.lists.values.flatten())).also { loaded.value = it }
        }
    }

    private fun publish(owner: String, cache: EpgCache, logos: Map<String, String>? = null) {
        val current = loaded.value
        loaded.value = Loaded(owner, cache, logos ?: current?.takeIf { it.owner == owner }?.logos.orEmpty())
    }

    private fun startOfDay(now: Long): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private companion object {
        const val TAG = "EllyEpg"
        const val PROGRAM_IMAGE_CACHE_SIZE = 40
        const val MINUTE_MILLIS = 60_000L
        const val NEAR_AHEAD_MILLIS = 26 * EpgCache.HOUR_MILLIS
        const val NEAR_MIN_INTERVAL_MILLIS = 10 * MINUTE_MILLIS
        const val FAR_MAX_AGE_MILLIS = 6 * EpgCache.HOUR_MILLIS
        const val CHANNELS_MAX_AGE_MILLIS = EpgCache.DAY_MILLIS
    }
}
