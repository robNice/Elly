package de.robnice.elgy.ui.pager

import androidx.annotation.StringRes
import de.robnice.elgy.R

/**
 * The swipeable screens of the remote, in display order.
 */
enum class RemotePage(@param:StringRes val titleRes: Int) {
    NAVIGATION(R.string.page_navigation),
    NUMBERS(R.string.page_numbers),
    MEDIA(R.string.page_media),
    POINTER(R.string.page_pointer)
}
