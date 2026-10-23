package de.robnice.elly.data.model

import de.robnice.elly.remote.RemoteKey

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
        key(RemoteKey.POWER, 0, 0, 4, 4),
        key(RemoteKey.SCREEN_OFF, 7, 0, 4, 4),
        key(RemoteKey.SCREEN_ON, 13, 0, 4, 4),
        key(RemoteKey.SETTINGS, 20, 0, 4, 4),
        key(RemoteKey.GUIDE, 0, 11, 4, 4),
        key(RemoteKey.HOME, 20, 11, 4, 4),
        element(CustomElementType.DPAD, 4, 12, 16, 17),
        element(CustomElementType.VOLUME, 0, 16, 4, 10),
        element(CustomElementType.CHANNEL, 20, 16, 4, 10),
        key(RemoteKey.MUTE, 0, 27, 4, 4),
        key(RemoteKey.LIVE_TV, 20, 27, 4, 4),
        key(RemoteKey.BACK, 10, 31, 4, 4),
        key(RemoteKey.RED, 0, 43, 6, 2),
        key(RemoteKey.GREEN, 6, 43, 6, 2),
        key(RemoteKey.YELLOW, 12, 43, 6, 2),
        key(RemoteKey.BLUE, 18, 43, 6, 2)
    )

    private val NUMBERS = screen(
        key(RemoteKey.MUTE, 0, 0, 3, 4),
        key(RemoteKey.DIGIT_1, 4, 0, 5, 5),
        key(RemoteKey.DIGIT_2, 9, 0, 6, 5),
        key(RemoteKey.DIGIT_3, 15, 0, 5, 5),
        key(RemoteKey.LIVE_TV, 21, 0, 3, 4),
        key(RemoteKey.DIGIT_4, 4, 6, 5, 5),
        key(RemoteKey.DIGIT_5, 9, 6, 6, 5),
        key(RemoteKey.DIGIT_6, 15, 6, 5, 5),
        element(CustomElementType.VOLUME, 0, 7, 3, 16),
        element(CustomElementType.CHANNEL, 21, 7, 3, 16),
        key(RemoteKey.DIGIT_7, 4, 12, 5, 5),
        key(RemoteKey.DIGIT_8, 9, 12, 6, 5),
        key(RemoteKey.DIGIT_9, 15, 12, 5, 5),
        key(RemoteKey.GUIDE, 4, 18, 5, 5),
        key(RemoteKey.DIGIT_0, 9, 18, 6, 5),
        key(RemoteKey.MORE_ACTIONS, 15, 18, 5, 5),
        element(CustomElementType.DPAD, 7, 27, 10, 12),
        key(RemoteKey.BACK, 0, 31, 4, 4),
        key(RemoteKey.INPUT, 20, 31, 4, 4),
        key(RemoteKey.RED, 0, 43, 6, 2),
        key(RemoteKey.GREEN, 6, 43, 6, 2),
        key(RemoteKey.YELLOW, 12, 43, 6, 2),
        key(RemoteKey.BLUE, 18, 43, 6, 2)
    )

    private val MEDIA = screen(
        key(RemoteKey.RECORD, 0, 0, 3, 3),
        key(RemoteKey.STOP, 3, 0, 3, 3),
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
