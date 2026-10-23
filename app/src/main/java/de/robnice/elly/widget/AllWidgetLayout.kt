package de.robnice.elly.widget

import de.robnice.elly.remote.RemoteKey

// The all-in-one widget offers exactly the keys of the single widgets, in three groups.
object AllWidgetLayout {

    val groups: List<List<RemoteKey>> = listOf(
        WidgetKind.POWER.keys.values + WidgetKind.SCREEN.keys.values + WidgetKind.MUTE.keys.values,
        WidgetKind.VOLUME.keys.values + WidgetKind.CHANNEL.keys.values,
        WidgetKind.MEDIA.keys.values.toList()
    )

    val keys: List<RemoteKey> = groups.flatten()

    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 8
    const val DEFAULT_COLUMNS = 5

    // Small groups share a row while it holds at most `columns` keys; a group that does not fit
    // into one row is split into evenly filled rows.
    fun rows(enabled: Set<RemoteKey>, columns: Int = DEFAULT_COLUMNS): List<List<RemoteKey>> {
        val capacity = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val rows = mutableListOf<MutableList<RemoteKey>>()
        groups.map { group -> group.filter { it in enabled } }
            .filter { it.isNotEmpty() }
            .forEach { group ->
                val last = rows.lastOrNull()
                if (last != null && last.size + group.size <= capacity) {
                    last += group
                } else {
                    rows += split(group, capacity)
                }
            }
        return rows
    }

    private fun split(group: List<RemoteKey>, capacity: Int): List<MutableList<RemoteKey>> {
        val count = (group.size + capacity - 1) / capacity
        val base = group.size / count
        val larger = group.size % count
        var start = 0
        return List(count) { index ->
            val size = base + if (index < larger) 1 else 0
            group.subList(start, start + size).toMutableList().also { start += size }
        }
    }
}
