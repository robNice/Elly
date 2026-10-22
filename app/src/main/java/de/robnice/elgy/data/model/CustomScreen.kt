package de.robnice.elgy.data.model

import de.robnice.elgy.remote.RemoteKey
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CustomElementType(val defaultWidth: Int, val defaultHeight: Int, val minWidth: Int, val minHeight: Int) {
    KEY(4, 4, 2, 2),
    VOLUME(4, 10, 3, 6),
    CHANNEL(4, 10, 3, 6),
    DPAD(18, 18, 10, 10),
    APP(8, 3, 5, 2)
}

// Proportions of one grid cell on screen: width per height, the gap by which every element is
// drawn inside its frame, in cell heights, and the roundings from the settings.
data class CellShape(
    val aspect: Float,
    val gap: Float,
    val rounding: Float = KeyRounding.DEFAULT,
    val dpadRounding: Float = KeyRounding.DEFAULT
)

data class ElementOutline(val left: Float, val top: Float, val right: Float, val bottom: Float, val radius: Float)

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

    // Without a cell shape only the frames are compared. With it, the visible shapes are compared,
    // so a round key may sit in the empty corner of the directional pad's frame.
    fun overlaps(other: CustomElement, cells: CellShape? = null): Boolean {
        val frames = x < other.x + other.width && other.x < x + width && y < other.y + other.height && other.y < y + height
        if (!frames || cells == null) return frames
        val a = body(cells)
        val b = other.body(cells)
        return a.distanceTo(b) < a.radius + b.radius - TOUCH_TOLERANCE
    }

    // Every element is drawn as a rectangle with rounded corners, measured in cell heights.
    fun outline(cells: CellShape): ElementOutline {
        val w = width * cells.aspect
        val h = height.toFloat()
        val square = type == CustomElementType.DPAD || (type == CustomElementType.KEY && w / h in ROUND_KEY_RATIO)
        val bw = (if (square) min(w, h) else w) - 2 * cells.gap
        val bh = (if (square) min(w, h) else h) - 2 * cells.gap
        val rounding = if (type == CustomElementType.DPAD) cells.dpadRounding else cells.rounding
        val cx = x * cells.aspect + w / 2
        val cy = y + h / 2
        return ElementOutline(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, rounding * min(bw, bh) / 2)
    }

    // The outline as a core rectangle widened by the corner radius.
    private fun body(cells: CellShape): Body = outline(cells).let {
        Body(it.left + it.radius, it.top + it.radius, it.right - it.radius, it.bottom - it.radius, it.radius)
    }

    private class Body(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val radius: Float) {

        fun distanceTo(other: Body): Float =
            hypot(max(0f, max(other.x1 - x2, x1 - other.x2)), max(0f, max(other.y1 - y2, y1 - other.y2)))
    }

    fun scaled(factorX: Float, factorY: Float): CustomElement = copy(
        x = (x * factorX).roundToInt(),
        y = (y * factorY).roundToInt(),
        width = (width * factorX).roundToInt(),
        height = (height * factorY).roundToInt()
    )

    companion object {
        val ROUND_KEY_RATIO = 0.7f..1.5f
        private const val ICON_ONLY_MIN_SIZE = 2
        private const val TOUCH_TOLERANCE = 0.01f
    }
}

data class CustomScreen(val elements: List<CustomElement>) {

    fun canPlace(candidate: CustomElement, cells: CellShape? = null): Boolean =
        fits(candidate) && elements.none { it.id != candidate.id && it.overlaps(candidate, cells) }

    private fun fits(candidate: CustomElement): Boolean =
        candidate.x >= 0 && candidate.y >= 0 &&
            candidate.width >= candidate.minWidth && candidate.height >= candidate.minHeight &&
            candidate.x + candidate.width <= COLUMNS && candidate.y + candidate.height <= ROWS

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

    // Places the whole group at the first spot where all of its elements fit.
    fun addGroup(group: ButtonGroup): CustomScreen? {
        val firstId = (elements.maxOfOrNull { it.id } ?: 0L) + 1
        for (y in 0..ROWS - group.height) {
            for (x in 0..COLUMNS - group.width) {
                val placed = group.elements.mapIndexed { index, item ->
                    CustomElement(firstId + index, item.type, x + item.x, y + item.y, item.width, item.height, item.key)
                }
                if (placed.all { canPlace(it) }) return copy(elements = elements + placed)
            }
        }
        return null
    }

    // Moves several elements by the same number of cells; null if one of them would not fit.
    fun moveAll(ids: Set<Long>, dx: Int, dy: Int, cells: CellShape? = null): CustomScreen? {
        val moved = elements.filter { it.id in ids }.map { it.copy(x = it.x + dx, y = it.y + dy) }
        val others = CustomScreen(elements.filter { it.id !in ids })
        if (!moved.all { others.canPlace(it, cells) }) return null
        return copy(elements = elements.map { element -> moved.firstOrNull { it.id == element.id } ?: element })
    }

    fun move(id: Long, x: Int, y: Int, cells: CellShape? = null): CustomScreen =
        update(id, cells) { it.copy(x = x, y = y) }

    fun resize(id: Long, width: Int, height: Int, cells: CellShape? = null): CustomScreen =
        update(id, cells) { it.copy(width = width, height = height) }

    fun remove(id: Long): CustomScreen = copy(elements = elements.filterNot { it.id == id })

    fun updateApp(id: Long, transform: (AppSlot?) -> AppSlot?): CustomScreen =
        copy(elements = elements.map { if (it.id == id && it.type == CustomElementType.APP) it.copy(app = transform(it.app)) else it })

    private fun update(id: Long, cells: CellShape?, transform: (CustomElement) -> CustomElement): CustomScreen {
        val current = elements.firstOrNull { it.id == id } ?: return this
        val changed = transform(current)
        if (!canPlace(changed, cells)) return this
        return copy(elements = elements.map { if (it.id == id) changed else it })
    }

    companion object {
        // 24 columns divide evenly into rows of 2, 3, 4, 6 and 8 keys; with 48 rows the cells are
        // close to square on common phones, so key sizes match the former fixed screens.
        const val COLUMNS = 24
        const val ROWS = 48
        val EMPTY = CustomScreen(emptyList())

        // Drops invalid elements, e.g. after a damaged save. Overlapping frames are kept, because the
        // editor allows them as long as the visible shapes stay apart.
        fun normalize(elements: List<CustomElement>): CustomScreen =
            elements.fold(EMPTY) { screen, element ->
                val valid = element.type != CustomElementType.KEY || element.key != null
                if (valid && element.id !in screen.elements.map { it.id } && screen.fits(element)) {
                    screen.copy(elements = screen.elements + element)
                } else {
                    screen
                }
            }
    }
}
