package de.robnice.elgy.data.model

enum class RemoteScreen { NAVIGATION, NUMBERS, MEDIA }

data class ScreenEntry(
    val screen: RemoteScreen,
    val enabled: Boolean
)

data class ScreenLayout(val entries: List<ScreenEntry>) {

    val enabledScreens: List<RemoteScreen>
        get() = entries.filter { it.enabled }.map { it.screen }

    fun toggle(screen: RemoteScreen): ScreenLayout {
        val target = entries.firstOrNull { it.screen == screen } ?: return this
        if (target.enabled && enabledScreens.size <= 1) return this
        return copy(entries = entries.map { if (it.screen == screen) it.copy(enabled = !it.enabled) else it })
    }

    fun reorder(order: List<RemoteScreen>): ScreenLayout {
        val byScreen = entries.associateBy { it.screen }
        return normalize(order.mapNotNull { byScreen[it] })
    }

    companion object {
        val DEFAULT = ScreenLayout(RemoteScreen.entries.map { ScreenEntry(it, enabled = true) })

        fun normalize(entries: List<ScreenEntry>): ScreenLayout {
            val unique = entries.distinctBy { it.screen }
            val missing = RemoteScreen.entries
                .filter { screen -> unique.none { it.screen == screen } }
                .map { ScreenEntry(it, enabled = true) }
            val complete = unique + missing
            val result = if (complete.none { it.enabled }) {
                complete.mapIndexed { index, entry -> if (index == 0) entry.copy(enabled = true) else entry }
            } else {
                complete
            }
            return ScreenLayout(result)
        }
    }
}
