package de.robnice.elly.epg

// Everything the guide keeps for one TV: the channels of each favorite list and the programs per channel
// (by signal id, sorted by start). The update times decide what the next refresh fetches.
data class EpgCache(
    val lists: Map<String, List<EpgChannel>> = emptyMap(),
    val programs: Map<String, List<EpgProgram>> = emptyMap(),
    val channelsUpdated: Map<String, Long> = emptyMap(),
    val nearUpdated: Long = 0,
    val farUpdated: Long = 0
) {

    fun withChannels(listId: String, channels: List<EpgChannel>, now: Long): EpgCache = copy(
        lists = lists + (listId to channels),
        channelsUpdated = channelsUpdated + (listId to now)
    )

    // Fetched programs replace what was known for those channels within [from, to) and every program they overlap;
    // older programs outside stay, which is how the guide keeps the past the TV has already dropped.
    fun merge(fetched: List<EpgProgram>, signalIds: Set<String>, from: Long, to: Long): EpgCache {
        val bySignal = fetched.groupBy { it.signalId }
        val merged = programs.toMutableMap()
        signalIds.forEach { signalId ->
            val incoming = bySignal[signalId].orEmpty()
            val kept = programs[signalId].orEmpty().filter { old ->
                (old.end <= from || old.start >= to) && incoming.none { it.start < old.end && old.start < it.end }
            }
            val combined = (kept + incoming).distinctBy { it.start }.sortedBy { it.start }
            if (combined.isEmpty()) merged.remove(signalId) else merged[signalId] = combined
        }
        return copy(programs = merged)
    }

    // At most the last 24 hours and the next 14 days are kept, and only for channels still in a list.
    fun pruned(now: Long): EpgCache {
        val oldest = now - KEEP_PAST_MILLIS
        val newest = now + KEEP_FUTURE_MILLIS
        val used = lists.values.flatten().map { it.signalId }.toSet()
        return copy(
            programs = programs
                .filterKeys { it in used }
                .mapValues { (_, list) -> list.filter { it.end > oldest && it.start < newest } }
                .filterValues { it.isNotEmpty() }
        )
    }

    fun guide(listId: String): EpgGuide? {
        val channels = lists[listId] ?: return null
        return EpgGuide(channels, channels.associate { it.signalId to programs[it.signalId].orEmpty() }, nearUpdated)
    }

    companion object {
        const val HOUR_MILLIS = 60 * 60 * 1000L
        const val DAY_MILLIS = 24 * HOUR_MILLIS
        const val KEEP_PAST_MILLIS = DAY_MILLIS
        const val KEEP_FUTURE_MILLIS = 14 * DAY_MILLIS
    }
}

// What a guide element shows: its channels in order and their programs.
data class EpgGuide(
    val channels: List<EpgChannel>,
    val programs: Map<String, List<EpgProgram>>,
    val updated: Long,
    val logos: Map<String, String> = emptyMap()
)
