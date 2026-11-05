package de.robnice.elly.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material3.MaterialTheme
import de.robnice.elly.wear.R

// A straight row of dots at the bottom. The curved indicator of Wear Compose left a thin seam at the top of the
// display, opposite the selected page.
@Composable
fun BoxScope.PageDots(pagerState: PagerState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.page_dot_spacing)),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = dimensionResource(R.dimen.page_dot_bottom))
    ) {
        repeat(pagerState.pageCount) { page ->
            val selected = page == pagerState.currentPage
            Box(
                Modifier
                    .size(dimensionResource(R.dimen.page_dot_size))
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
            )
        }
    }
}
