package de.robnice.elgy.data.model

import de.robnice.elgy.remote.RemoteKey

// Factory layouts on the 24 x 48 grid, matching the former fixed screens as closely as the grid allows.
object DefaultScreens {

    fun layout(screen: RemoteScreen): CustomScreen = when (screen) {
        RemoteScreen.NAVIGATION -> NAVIGATION
        RemoteScreen.NUMBERS -> NUMBERS
        RemoteScreen.MEDIA -> MEDIA
        RemoteScreen.CUSTOM -> CustomScreen.EMPTY
    }

    fun all(): Map<RemoteScreen, CustomScreen> = RemoteScreen.entries.associateWith(::layout)

    private val NAVIGATION = screen(
        key(RemoteKey.POWER, 1, 0, 4, 4),
        key(RemoteKey.HOME, 7, 0, 4, 4),
        key(RemoteKey.SETTINGS, 13, 0, 4, 4),
        key(RemoteKey.LIVE_TV, 19, 0, 4, 4),
        element(CustomElementType.VOLUME, 3, 7, 4, 10),
        key(RemoteKey.MUTE, 10, 10, 4, 4),
        element(CustomElementType.CHANNEL, 17, 7, 4, 10),
        element(CustomElementType.DPAD, 3, 20, 18, 18),
        key(RemoteKey.BACK, 5, 43, 4, 4),
        key(RemoteKey.GUIDE, 15, 43, 4, 4)
    )

    private val NUMBERS = screen(
        key(RemoteKey.INPUT, 5, 4, 4, 4),
        key(RemoteKey.LIVE_TV, 15, 4, 4, 4),
        key(RemoteKey.DIGIT_1, 2, 12, 6, 3),
        key(RemoteKey.DIGIT_2, 9, 12, 6, 3),
        key(RemoteKey.DIGIT_3, 16, 12, 6, 3),
        key(RemoteKey.DIGIT_4, 2, 19, 6, 3),
        key(RemoteKey.DIGIT_5, 9, 19, 6, 3),
        key(RemoteKey.DIGIT_6, 16, 19, 6, 3),
        key(RemoteKey.DIGIT_7, 2, 26, 6, 3),
        key(RemoteKey.DIGIT_8, 9, 26, 6, 3),
        key(RemoteKey.DIGIT_9, 16, 26, 6, 3),
        key(RemoteKey.GUIDE, 2, 33, 6, 3),
        key(RemoteKey.DIGIT_0, 9, 33, 6, 3),
        key(RemoteKey.MORE_ACTIONS, 16, 33, 6, 3),
        key(RemoteKey.BACK, 5, 40, 4, 4),
        key(RemoteKey.OK, 15, 40, 4, 4)
    )

    private val MEDIA = screen(
        key(RemoteKey.RECORD, 0, 0, 3, 3),
        key(RemoteKey.LIVE_STOP, 3, 0, 3, 3),
        key(RemoteKey.LIVE_PLAY, 6, 0, 3, 3),
        key(RemoteKey.REWIND, 12, 0, 3, 3),
        key(RemoteKey.PAUSE, 15, 0, 3, 3),
        key(RemoteKey.PLAY, 18, 0, 3, 3),
        key(RemoteKey.FAST_FORWARD, 21, 0, 3, 3),
        element(CustomElementType.DPAD, 3, 10, 18, 18),
        key(RemoteKey.BACK, 10, 29, 4, 3),
        key(RemoteKey.RED, 1, 41, 4, 2),
        key(RemoteKey.GREEN, 7, 41, 4, 2),
        key(RemoteKey.YELLOW, 13, 41, 4, 2),
        key(RemoteKey.BLUE, 19, 41, 4, 2),
        app("netflix", "Netflix", 0, 45),
        app("amazon", "Prime Video", 8, 45),
        app("ui30", "Rakuten TV", 16, 45)
    )

    private fun screen(vararg elements: (Long) -> CustomElement): CustomScreen =
        CustomScreen(elements.mapIndexed { index, create -> create(index + 1L) })

    private fun key(key: RemoteKey, x: Int, y: Int, width: Int, height: Int): (Long) -> CustomElement =
        { id -> CustomElement(id, CustomElementType.KEY, x, y, width, height, key = key) }

    private fun element(type: CustomElementType, x: Int, y: Int, width: Int, height: Int): (Long) -> CustomElement =
        { id -> CustomElement(id, type, x, y, width, height) }

    private fun app(appId: String, title: String, x: Int, y: Int): (Long) -> CustomElement =
        { id -> CustomElement(id, CustomElementType.APP, x, y, 8, 3, app = AppSlot(enabled = true, appId = appId, title = title, showText = true)) }
}
