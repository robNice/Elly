package de.robnice.elly.data.model

import de.robnice.elly.remote.RemoteKey

// Pages of the watch app. Their layout is fixed so every key keeps a 48 dp touch target on small watches;
// what the keys do is set on the phone.
enum class WatchPage { NAVIGATION, VOLUME_CHANNEL, NUMBERS, MEDIA, POINTER }

// Every key on the watch that can be given another function. The directional pad and the digits keep theirs.
enum class WatchSlot(val page: WatchPage?) {
    NAV_LEFT(WatchPage.NAVIGATION),
    NAV_RIGHT(WatchPage.NAVIGATION),
    VOL_TOP(WatchPage.VOLUME_CHANNEL),
    VOL_CENTER(WatchPage.VOLUME_CHANNEL),
    VOL_LEFT_TOP(WatchPage.VOLUME_CHANNEL),
    VOL_LEFT_BOTTOM(WatchPage.VOLUME_CHANNEL),
    VOL_RIGHT_TOP(WatchPage.VOLUME_CHANNEL),
    VOL_RIGHT_BOTTOM(WatchPage.VOLUME_CHANNEL),
    NUM_LEFT(WatchPage.NUMBERS),
    NUM_RIGHT(WatchPage.NUMBERS),
    MEDIA_TOP(WatchPage.MEDIA),
    MEDIA_CENTER(WatchPage.MEDIA),
    MEDIA_LEFT(WatchPage.MEDIA),
    MEDIA_RIGHT(WatchPage.MEDIA),
    MEDIA_BOTTOM_LEFT(WatchPage.MEDIA),
    MEDIA_BOTTOM_RIGHT(WatchPage.MEDIA),
    POINTER_LEFT(WatchPage.POINTER),
    POINTER_RIGHT(WatchPage.POINTER),
    TILE_1(null),
    TILE_2(null),
    TILE_3(null),
    TILE_4(null),
    TILE_5(null),
    TILE_6(null);

    companion object {
        val TILE = listOf(TILE_1, TILE_2, TILE_3, TILE_4, TILE_5, TILE_6)

        fun on(page: WatchPage): List<WatchSlot> = entries.filter { it.page == page }
    }
}

sealed interface WatchAction {
    data class Key(val key: RemoteKey) : WatchAction

    // The TV does not tell whether something plays, so the watch alternates between pause and play.
    data object PlayPause : WatchAction

    data class App(val appId: String, val title: String, val display: AppDisplay = AppDisplay.ICON) : WatchAction

    companion object {
        // Keys that open a list or the keyboard on the phone have nothing to show on the watch. Only the input key
        // stays: on the watch it opens the input picker of the TV.
        val ASSIGNABLE_KEYS: List<RemoteKey> = RemoteKey.entries.filter {
            it != RemoteKey.AUDIO_TRACK && it != RemoteKey.SUBTITLE && it != RemoteKey.KEYBOARD
        }
    }
}

// What an app key shows. Without an icon from the TV it shows the name.
enum class AppDisplay { ICON, TEXT, BOTH }

data class WatchPageEntry(val page: WatchPage, val visible: Boolean)

data class WatchLayout(
    val pages: List<WatchPageEntry> = WatchPage.entries.map { WatchPageEntry(it, true) },
    private val actions: Map<WatchSlot, WatchAction> = emptyMap()
) {
    val visiblePages: List<WatchPage>
        get() = pages.filter { it.visible }.map { it.page }

    val assigned: Map<WatchSlot, WatchAction>
        get() = actions

    fun action(slot: WatchSlot): WatchAction = actions[slot] ?: DEFAULT_ACTIONS.getValue(slot)

    fun with(slot: WatchSlot, action: WatchAction?): WatchLayout =
        copy(actions = if (action == null || action == DEFAULT_ACTIONS[slot]) actions - slot else actions + (slot to action))

    // The last visible page cannot be hidden, the watch would show nothing but its settings.
    fun withVisible(page: WatchPage, visible: Boolean): WatchLayout {
        if (!visible && visiblePages == listOf(page)) return this
        return copy(pages = pages.map { if (it.page == page) it.copy(visible = visible) else it })
    }

    fun reordered(order: List<WatchPage>): WatchLayout =
        copy(pages = order.mapNotNull { page -> pages.firstOrNull { it.page == page } } + pages.filter { it.page !in order })

    fun reset(slots: List<WatchSlot>): WatchLayout = copy(actions = actions - slots.toSet())

    val apps: List<WatchAction.App>
        get() = WatchSlot.entries.map(::action).filterIsInstance<WatchAction.App>().distinctBy { it.appId }

    companion object {
        val DEFAULT_ACTIONS: Map<WatchSlot, WatchAction> = mapOf(
            WatchSlot.NAV_LEFT to WatchAction.Key(RemoteKey.BACK),
            WatchSlot.NAV_RIGHT to WatchAction.Key(RemoteKey.HOME),
            WatchSlot.VOL_TOP to WatchAction.Key(RemoteKey.POWER),
            WatchSlot.VOL_CENTER to WatchAction.Key(RemoteKey.MUTE),
            WatchSlot.VOL_LEFT_TOP to WatchAction.Key(RemoteKey.CHANNEL_UP),
            WatchSlot.VOL_LEFT_BOTTOM to WatchAction.Key(RemoteKey.CHANNEL_DOWN),
            WatchSlot.VOL_RIGHT_TOP to WatchAction.Key(RemoteKey.VOLUME_UP),
            WatchSlot.VOL_RIGHT_BOTTOM to WatchAction.Key(RemoteKey.VOLUME_DOWN),
            WatchSlot.NUM_LEFT to WatchAction.Key(RemoteKey.BACK),
            WatchSlot.NUM_RIGHT to WatchAction.Key(RemoteKey.OK),
            WatchSlot.MEDIA_TOP to WatchAction.Key(RemoteKey.STOP),
            WatchSlot.MEDIA_CENTER to WatchAction.PlayPause,
            WatchSlot.MEDIA_LEFT to WatchAction.Key(RemoteKey.REWIND),
            WatchSlot.MEDIA_RIGHT to WatchAction.Key(RemoteKey.FAST_FORWARD),
            WatchSlot.MEDIA_BOTTOM_LEFT to WatchAction.Key(RemoteKey.BACK),
            WatchSlot.MEDIA_BOTTOM_RIGHT to WatchAction.Key(RemoteKey.HOME),
            WatchSlot.POINTER_LEFT to WatchAction.Key(RemoteKey.BACK),
            WatchSlot.POINTER_RIGHT to WatchAction.Key(RemoteKey.HOME),
            WatchSlot.TILE_1 to WatchAction.Key(RemoteKey.POWER),
            WatchSlot.TILE_2 to WatchAction.Key(RemoteKey.MUTE),
            WatchSlot.TILE_3 to WatchAction.PlayPause,
            WatchSlot.TILE_4 to WatchAction.Key(RemoteKey.VOLUME_DOWN),
            WatchSlot.TILE_5 to WatchAction.Key(RemoteKey.HOME),
            WatchSlot.TILE_6 to WatchAction.Key(RemoteKey.VOLUME_UP)
        )
    }
}
