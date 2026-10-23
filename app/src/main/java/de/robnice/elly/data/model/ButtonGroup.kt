package de.robnice.elly.data.model

import de.robnice.elly.remote.RemoteKey

data class GroupElement(
    val type: CustomElementType,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val key: RemoteKey? = null
)

// Several elements added in one go; on the screen they are ordinary single elements.
// Every element has the default size of its type, and every group has an even width so it can be
// centered on the 24 column grid.
enum class ButtonGroup(val elements: List<GroupElement>) {
    NUMBERS(
        listOf(
            RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3,
            RemoteKey.DIGIT_4, RemoteKey.DIGIT_5, RemoteKey.DIGIT_6,
            RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9
        ).mapIndexed { index, key -> key(key, index % 3 * KEY_STEP, index / 3 * KEY_STEP) } +
            key(RemoteKey.DIGIT_0, KEY_STEP, 3 * KEY_STEP)
    ),
    APPS((0 until 4).map { app(it % 2 * APP_STEP_X, it / 2 * APP_STEP_Y) }),
    PLAYBACK(
        listOf(RemoteKey.REWIND, RemoteKey.PLAY, RemoteKey.PAUSE, RemoteKey.FAST_FORWARD, RemoteKey.STOP)
            .mapIndexed { index, key -> key(key, index * KEY_STEP_TIGHT, 0) }
    ),
    COLORS(
        listOf(RemoteKey.RED, RemoteKey.GREEN, RemoteKey.YELLOW, RemoteKey.BLUE)
            .mapIndexed { index, key -> key(key, index * KEY_STEP, 0) }
    ),
    VOLUME_CHANNEL(
        listOf(
            element(CustomElementType.VOLUME, 0, 0),
            key(RemoteKey.MUTE, 10, 3),
            element(CustomElementType.CHANNEL, 20, 0)
        )
    ),
    NAVIGATION(
        listOf(
            element(CustomElementType.DPAD, 3, 0),
            key(RemoteKey.BACK, 0, 18),
            key(RemoteKey.HOME, 20, 18)
        )
    ),
    POWER(
        listOf(RemoteKey.POWER, RemoteKey.SCREEN_OFF, RemoteKey.SCREEN_ON)
            .mapIndexed { index, key -> key(key, index * KEY_STEP, 0) }
    );

    val width: Int get() = elements.maxOf { it.x + it.width }
    val height: Int get() = elements.maxOf { it.y + it.height }
}

private fun element(type: CustomElementType, x: Int, y: Int, key: RemoteKey? = null) =
    GroupElement(type, x, y, type.defaultWidth, type.defaultHeight, key)

private fun key(key: RemoteKey, x: Int, y: Int) = element(CustomElementType.KEY, x, y, key)

private fun app(x: Int, y: Int) = element(CustomElementType.APP, x, y)

private const val KEY_STEP = 6
private const val KEY_STEP_TIGHT = 5
private const val APP_STEP_X = 10
private const val APP_STEP_Y = 4
