package de.robnice.elgy.data.model

import de.robnice.elgy.remote.RemoteKey

data class GroupElement(
    val type: CustomElementType,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val key: RemoteKey? = null
)

// Several elements added in one go; on the screen they are ordinary single elements.
enum class ButtonGroup(val elements: List<GroupElement>) {
    NUMBERS(
        listOf(
            RemoteKey.DIGIT_1, RemoteKey.DIGIT_2, RemoteKey.DIGIT_3,
            RemoteKey.DIGIT_4, RemoteKey.DIGIT_5, RemoteKey.DIGIT_6,
            RemoteKey.DIGIT_7, RemoteKey.DIGIT_8, RemoteKey.DIGIT_9
        ).mapIndexed { index, key -> key(key, index % 3 * 6, index / 3 * 5, 5, 4) } +
            key(RemoteKey.DIGIT_0, 6, 15, 5, 4)
    ),
    APPS((0 until 4).map { GroupElement(CustomElementType.APP, it * 6, 0, 6, 3) }),
    PLAYBACK(
        listOf(RemoteKey.REWIND, RemoteKey.PLAY, RemoteKey.PAUSE, RemoteKey.FAST_FORWARD, RemoteKey.STOP)
            .mapIndexed { index, key -> key(key, index * 5, 0, 4, 4) }
    ),
    COLORS(
        listOf(RemoteKey.RED, RemoteKey.GREEN, RemoteKey.YELLOW, RemoteKey.BLUE)
            .mapIndexed { index, key -> key(key, index * 6, 0, 6, 2) }
    ),
    VOLUME_CHANNEL(
        listOf(
            GroupElement(CustomElementType.VOLUME, 0, 0, 4, 10),
            key(RemoteKey.MUTE, 10, 3, 4, 4),
            GroupElement(CustomElementType.CHANNEL, 20, 0, 4, 10)
        )
    ),
    NAVIGATION(
        listOf(
            GroupElement(CustomElementType.DPAD, 4, 0, 16, 16),
            key(RemoteKey.BACK, 0, 16, 4, 4),
            key(RemoteKey.HOME, 20, 16, 4, 4)
        )
    ),
    POWER(
        listOf(RemoteKey.POWER, RemoteKey.SCREEN_OFF, RemoteKey.SCREEN_ON)
            .mapIndexed { index, key -> key(key, index * 5, 0, 4, 4) }
    );

    val width: Int get() = elements.maxOf { it.x + it.width }
    val height: Int get() = elements.maxOf { it.y + it.height }
}

private fun key(key: RemoteKey, x: Int, y: Int, width: Int, height: Int) =
    GroupElement(CustomElementType.KEY, x, y, width, height, key)
