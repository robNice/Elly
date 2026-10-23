package de.robnice.elly.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.robnice.elly.R
import de.robnice.elly.ui.elly.EllyMascot
import de.robnice.elly.ui.elly.EllySpeechBubble
import de.robnice.elly.ui.elly.TailSide
import de.robnice.elly.ui.elly.fadeBottomEdge
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pages = OnboardingPages.all
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == pages.lastIndex

    BackHandler {
        if (pagerState.currentPage > 0) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        else onFinish()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            if (!lastPage) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
            } else {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_padding) * 2))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }
        PageDots(pagerState = pagerState)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.onboarding_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                    Text(stringResource(R.string.onboarding_back))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                if (lastPage) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Text(stringResource(if (lastPage) R.string.onboarding_finish else R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fadeBottomEdge({ scrollState.canScrollForward }, dimensionResource(R.dimen.onboarding_fade_height))
            .verticalScroll(scrollState)
            .padding(horizontal = dimensionResource(R.dimen.onboarding_padding)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (page.items.isEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_section_spacing)))
            EllyMascot(size = dimensionResource(R.dimen.onboarding_mascot_size))
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_bubble_spacing)))
            EllySpeechBubble(title = stringResource(page.title), text = stringResource(page.text), tailSide = TailSide.TOP)
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                EllyMascot(size = dimensionResource(R.dimen.onboarding_mascot_size_small))
                EllySpeechBubble(
                    title = stringResource(page.title),
                    text = stringResource(page.text),
                    tailSide = TailSide.START,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (page.items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_section_spacing)))
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_item_spacing))) {
                page.items.forEach { OnboardingItemRow(it) }
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.onboarding_section_spacing)))
    }
}

@Composable
private fun OnboardingItemRow(item: OnboardingItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(dimensionResource(R.dimen.onboarding_item_icon_size)),
            contentAlignment = Alignment.Center
        ) {
            when (val visual = item.visual) {
                is OnboardingVisual.Icon -> Icon(
                    painter = painterResource(visual.res),
                    contentDescription = null,
                    tint = colorResource(R.color.onboarding_item_icon)
                )
                OnboardingVisual.StatusLights -> StatusLights()
                OnboardingVisual.Elly -> Image(
                    painter = painterResource(R.drawable.elly),
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.onboarding_item_icon_spacing)))
        Text(
            text = stringResource(item.text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusLights() {
    val colors = listOf(R.color.status_connected, R.color.status_disconnected, R.color.status_checking, R.color.status_demo)
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_status_dot_spacing))) {
        colors.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_status_dot_spacing))) {
                row.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.onboarding_status_dot_size))
                            .background(colorResource(color), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(pagerState: PagerState) {
    val description = stringResource(R.string.page_indicator_description, pagerState.currentPage + 1, pagerState.pageCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.onboarding_dot_spacing), Alignment.CenterHorizontally)
    ) {
        repeat(pagerState.pageCount) { index ->
            val active = index == pagerState.currentPage
            Box(
                modifier = Modifier
                    .animateContentSize()
                    .height(dimensionResource(R.dimen.onboarding_dot_size))
                    .width(dimensionResource(if (active) R.dimen.onboarding_dot_active_width else R.dimen.onboarding_dot_size))
                    .background(
                        colorResource(if (active) R.color.onboarding_dot_active else R.color.onboarding_dot_inactive),
                        CircleShape
                    )
            )
        }
    }
}

