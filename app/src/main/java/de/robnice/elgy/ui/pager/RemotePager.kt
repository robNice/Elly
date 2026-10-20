package de.robnice.elgy.ui.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.robnice.elgy.R

/**
 * Hosts all [RemotePage]s side by side; the user switches between them by swiping.
 */
@Composable
fun RemotePager(modifier: Modifier = Modifier) {
    val pages = RemotePage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { index ->
            RemotePageContent(page = pages[index])
        }
        PageIndicator(pagerState = pagerState)
    }
}

@Composable
private fun RemotePageContent(page: RemotePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.page_title_spacing)))
        Text(
            text = stringResource(R.string.page_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
    val description = stringResource(
        R.string.page_indicator_description,
        pagerState.currentPage + 1,
        pagerState.pageCount
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.indicator_padding))
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.indicator_dot_spacing),
            Alignment.CenterHorizontally
        )
    ) {
        repeat(pagerState.pageCount) { index ->
            val color = if (index == pagerState.currentPage) {
                colorResource(R.color.indicator_active)
            } else {
                colorResource(R.color.indicator_inactive)
            }
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.indicator_dot_size))
                    .background(color, CircleShape)
            )
        }
    }
}
