package de.robnice.elgy.data.model

import de.robnice.elgy.remote.RemoteKey
import kotlin.math.roundToInt

enum class CustomElementType(val defaultWidth: Int, val defaultHeight: Int, val minWidth: Int, val minHeight: Int) {
    KEY(4, 4, 2, 2),
    VOLUME(4, 10, 3, 6),
    CHANNEL(4, 10, 3, 6),
    DPAD(18, 18, 10, 10),
    APP(8, 3, 5, 2)
}

data class CustomElement(
    val id: Long,
    val type: CustomElementType,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val key: RemoteKey? = null,
    val app: AppSlot? = null
) {
    private val iconOnly: Boolean
        get() = type == CustomElementType.APP && app != null && app.showIcon && !app.showText && app.iconPath != null

    val minWidth: Int
        get() = if (iconOnly) ICON_ONLY_MIN_SIZE else type.minWidth

    val minHeight: Int
        get() = if (iconOnly) ICON_ONLY_MIN_SIZE else type.minHeight

    fun overlaps(other: CustomElement): Boolean =
        x < other.x + other.width && other.x < x + width && y < other.y + other.height && other.y < y + height

    fun scaled(factorX: Float, factorY: Float): CustomElement = copy(
        x = (x * factorX).roundToInt(),
        y = (y * factorY).roundToInt(),
        width = (width * factorX).roundToInt(),
        height = (height * factorY).roundToInt()
    )

    private companion object {
        const val ICON_ONLY_MIN_SIZE = 2
    }
}

data class CustomScreen(val elements: List<CustomElement>) {

    fun canPlace(candidate: CustomElement): Boolean =
        candidate.x >= 0 && candidate.y >= 0 &&
            candidate.width >= candidate.minWidth && candidate.height >= candidate.minHeight &&
            candidate.x + candidate.width <= COLUMNS && candidate.y + candidate.height <= ROWS &&
            elements.none { it.id != candidate.id && it.overlaps(candidate) }

    // Places the element at the first free spot, shrinking it towards its minimum size if needed.
    fun add(type: CustomElementType, key: RemoteKey? = null): CustomScreen? {
        val id = (elements.maxOfOrNull { it.id } ?: 0L) + 1
        val sizes = (type.minWidth..type.defaultWidth)
            .flatMap { width -> (type.minHeight..type.defaultHeight).map { height -> width to height } }
            .sortedByDescending { (width, height) -> width * height }
        for ((width, height) in sizes) {
            for (y in 0..ROWS - height) {
                for (x in 0..COLUMNS - width) {
                    val candidate = CustomElement(id, type, x, y, width, height, key)
                    if (canPlace(candidate)) return copy(elements = elements + candidate)
                }
            }
        }
        return null
    }

    fun move(id: Long, x: Int, y: Int): CustomScreen = update(id) { it.copy(x = x, y = y) }

    fun resize(id: Long, width: Int, height: Int): CustomScreen = update(id) { it.copy(width = width, height = height) }

    fun remove(id: Long): CustomScreen = copy(elements = elements.filterNot { it.id == id })

    fun updateApp(id: Long, transform: (AppSlot?) -> AppSlot?): CustomScreen =
        copy(elements = elements.map { if (it.id == id && it.type == CustomElementType.APP) it.copy(app = transform(it.app)) else it })

    private fun update(id: Long, transform: (CustomElement) -> CustomElement): CustomScreen {
        val current = elements.firstOrNull { it.id == id } ?: return this
        val changed = transform(current)
        if (!canPlace(changed)) return this
        return copy(elements = elements.map { if (it.id == id) changed else it })
    }

    companion object {
        // 24 columns divide evenly into rows of 2, 3, 4, 6 and 8 keys; with 48 rows the cells are
        // close to square on common phones, so key sizes match the former fixed screens.
        const val COLUMNS = 24
        const val ROWS = 48
        val EMPTY = CustomScreen(emptyList())

        // Drops elements that are invalid or overlap earlier ones, e.g. after a damaged save.
        fun normalize(elements: List<CustomElement>): CustomScreen =
            elements.fold(EMPTY) { screen, element ->
                val valid = element.type != CustomElementType.KEY || element.key != null
                if (valid && element.id !in screen.elements.map { it.id } && screen.canPlace(element)) {
                    screen.copy(elements = screen.elements + element)
                } else {
                    screen
                }
            }
    }
}
