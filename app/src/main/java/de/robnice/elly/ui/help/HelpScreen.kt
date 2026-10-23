package de.robnice.elly.ui.help

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import de.robnice.elly.R
import de.robnice.elly.ui.elly.EllyMascot
import de.robnice.elly.ui.elly.EllyPoses
import de.robnice.elly.ui.elly.EllySpeechBubble
import de.robnice.elly.ui.elly.TailSide
import de.robnice.elly.ui.elly.fadeBottomEdge
import kotlinx.coroutines.launch

@Composable
fun HelpScreen(onClose: () -> Unit, onStartTour: () -> Unit, onOpenPrivacy: () -> Unit) {
    val chapters = HelpContent.chapters
    var query by rememberSaveable { mutableStateOf("") }
    val pagerState = rememberPagerState { chapters.size + 1 }
    val scope = rememberCoroutineScope()
    val chapter = (pagerState.currentPage - 1).takeIf { it >= 0 }
    var topicChapter by rememberSaveable { mutableStateOf(-1) }
    var topic by rememberSaveable { mutableStateOf(0) }
    val open: (Int?, Int) -> Unit = { c, t ->
        topicChapter = c ?: -1
        topic = t
        scope.launch { pagerState.animateScrollToPage((c ?: -1) + 1) }
    }
    val resources = LocalResources.current
    val search = remember(resources) {
        HelpSearch(chapters.flatMapIndexed { c, ch ->
            ch.topics.mapIndexed { t, tp -> HelpEntry(c, t, resources.getString(tp.title), resources.getString(tp.body)) }
        })
    }
    val terms = HelpSearch.terms(query)
    val matches = remember(query, search) { search.search(query) }

    LaunchedEffect(pagerState.settledPage) { if (pagerState.settledPage - 1 != topicChapter) topicChapter = -1 }
    BackHandler { if (chapter != null) open(null, 0) else onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (chapter != null) open(null, 0) else onClose() }) {
                Icon(
                    painter = painterResource(if (chapter != null) R.drawable.ic_chevron_left else R.drawable.ic_close),
                    contentDescription = stringResource(R.string.onboarding_back)
                )
            }
            Text(
                text = chapter?.let { stringResource(chapters[it].title) } ?: stringResource(R.string.help_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorResource(R.color.help_chapter_title)
            )
        }
        val ellyText = when {
            chapter != null -> stringResource(chapters[chapter].ellyIntro)
            terms.isEmpty() -> stringResource(R.string.help_elly_home)
            matches.isEmpty() -> stringResource(R.string.help_elly_no_results)
            else -> pluralStringResource(R.plurals.help_elly_results, matches.size, matches.size)
        }
        Row(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.onboarding_padding))) {
            EllyMascot(size = dimensionResource(R.dimen.help_mascot_size), pose = chapter?.let { chapters[it].elly } ?: EllyPoses.Reading)
            EllySpeechBubble(text = ellyText, tailSide = TailSide.START, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.help_spacing)))
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) { page ->
            if (page == 0) {
                HelpHome(query, { query = it }, terms, matches, onStartTour) { c, t -> open(c, t) }
            } else {
                val current = page - 1
                HelpChapterContent(current, if (current == topicChapter) topic else 0, terms, onOpenPrivacy) { open(it, 0) }
            }
        }
        HelpNavigation(chapter, onHome = { open(null, 0) }) { open(it, 0) }
    }
}

@Composable
private fun HelpNavigation(chapter: Int?, onHome: () -> Unit, onOpen: (Int) -> Unit) {
    val last = HelpContent.chapters.lastIndex
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.onboarding_padding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (chapter == 0) onHome() else chapter?.let { onOpen(it - 1) } }, enabled = chapter != null) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.help_nav_previous))
        }
        IconButton(onClick = onHome, enabled = chapter != null) {
            Icon(painterResource(R.drawable.ic_home), contentDescription = stringResource(R.string.help_nav_home))
        }
        IconButton(onClick = { onOpen(chapter?.plus(1) ?: 0) }, enabled = chapter == null || chapter < last) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.help_nav_next))
        }
    }
}

@Composable
private fun HelpHome(
    query: String,
    onQuery: (String) -> Unit,
    terms: List<String>,
    matches: List<HelpMatch>,
    onStartTour: () -> Unit,
    onOpen: (Int, Int) -> Unit
) {
    val chapters = HelpContent.chapters
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .fadeBottomEdge({ listState.canScrollForward }, dimensionResource(R.dimen.onboarding_fade_height)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = dimensionResource(R.dimen.onboarding_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.help_item_spacing))
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.help_search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                trailingIcon = if (query.isEmpty()) null else ({
                    IconButton(onClick = { onQuery("") }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.help_search_clear))
                    }
                }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (terms.isNotEmpty()) {
            itemsIndexed(matches) { _, match ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(match.entry.chapter, match.entry.topic) }
                    .padding(vertical = dimensionResource(R.dimen.help_row_padding))) {
                    Text(highlighted(match.entry.title, terms), style = MaterialTheme.typography.titleSmall, color = colorResource(R.color.help_heading))
                    Text(stringResource(chapters[match.entry.chapter].title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(highlighted(match.snippet, terms), style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.help_text))
                }
            }
        } else {
            item {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.onboarding_bubble), RoundedCornerShape(dimensionResource(R.dimen.onboarding_bubble_corner)))
                    .clickable(onClick = onStartTour)
                    .padding(dimensionResource(R.dimen.onboarding_bubble_padding))) {
                    Text(stringResource(R.string.help_quick_title), style = MaterialTheme.typography.titleMedium, color = colorResource(R.color.help_heading))
                    Text(stringResource(R.string.help_quick_text), style = MaterialTheme.typography.bodyMedium, color = colorResource(R.color.help_text))
                }
            }
            item { Text(stringResource(R.string.help_contents), style = MaterialTheme.typography.titleMedium) }
            chapters.forEachIndexed { c, ch ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(c, 0) }.padding(vertical = dimensionResource(R.dimen.help_row_padding))
                    ) {
                        Icon(painterResource(ch.icon), contentDescription = null, tint = colorResource(R.color.onboarding_item_icon),
                            modifier = Modifier.size(dimensionResource(R.dimen.onboarding_item_icon_size)))
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.onboarding_item_icon_spacing)))
                        Text(stringResource(R.string.help_chapter_number, c + 1, stringResource(ch.title)), style = MaterialTheme.typography.titleSmall)
                    }
                    ch.topics.forEachIndexed { t, tp ->
                        Text(
                            text = stringResource(tp.title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(c, t) }
                                .padding(start = dimensionResource(R.dimen.help_topic_indent), top = dimensionResource(R.dimen.help_row_padding), bottom = dimensionResource(R.dimen.help_row_padding))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpChapterContent(
    chapter: Int,
    topic: Int,
    terms: List<String>,
    onOpenPrivacy: () -> Unit,
    onOpenChapter: (Int) -> Unit
) {
    val chapters = HelpContent.chapters
    val listState: LazyListState = rememberLazyListState()
    LaunchedEffect(chapter, topic) { listState.scrollToItem(topic) }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .fadeBottomEdge({ listState.canScrollForward }, dimensionResource(R.dimen.onboarding_fade_height)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = dimensionResource(R.dimen.onboarding_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.help_section_spacing))
    ) {
        itemsIndexed(chapters[chapter].topics) { _, tp ->
            Column {
                Text(highlighted(stringResource(tp.title), terms), style = MaterialTheme.typography.titleMedium, color = colorResource(R.color.help_heading))
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.help_item_spacing)))
                HelpBody(stringResource(tp.body), terms, onOpenPrivacy)
                tp.image?.let { HelpImage(it, stringResource(tp.title)) }
            }
        }
        if (chapter < chapters.lastIndex) {
            item {
                TextButton(onClick = { onOpenChapter(chapter + 1) }) {
                    Text(stringResource(R.string.help_next_chapter, stringResource(chapters[chapter + 1].title)))
                }
            }
        }
    }
}

@Composable
private fun HelpImage(@DrawableRes image: Int, description: String) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.help_image_corner))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(image),
            contentDescription = description,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .padding(top = dimensionResource(R.dimen.help_spacing))
                .fillMaxWidth(HELP_IMAGE_WIDTH)
                .clip(shape)
                .border(dimensionResource(R.dimen.help_image_border), MaterialTheme.colorScheme.outlineVariant, shape)
        )
    }
}

@Composable
private fun HelpBody(body: String, terms: List<String>, onOpenPrivacy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.help_item_spacing))) {
        body.split('\n').forEach { line ->
            if (line.startsWith(BULLET)) {
                Row {
                    Text(BULLET, style = MaterialTheme.typography.bodyMedium, color = colorResource(R.color.help_accent))
                    Text(
                        highlighted(line.removePrefix(BULLET), terms, boldLead = true, onOpenPrivacy = onOpenPrivacy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.help_text)
                    )
                }
            } else {
                Text(highlighted(line, terms, onOpenPrivacy = onOpenPrivacy), style = MaterialTheme.typography.bodyMedium, color = colorResource(R.color.help_text))
            }
        }
    }
}

@Composable
private fun highlighted(
    text: String,
    terms: List<String>,
    boldLead: Boolean = false,
    onOpenPrivacy: (() -> Unit)? = null
): AnnotatedString {
    val highlight = colorResource(R.color.help_highlight)
    val privacyLink = stringResource(R.string.help_privacy_link)
    val accent = colorResource(R.color.help_accent)
    val linkStyle = TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        append(text)
        val colon = text.indexOf(": ")
        if (boldLead && colon in 1 until BOLD_LEAD_MAX) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent), 0, colon + 1)
        HelpSearch.matchRanges(text, terms).forEach { addStyle(SpanStyle(background = highlight), it.first, it.last + 1) }
        if (onOpenPrivacy != null) {
            var start = text.indexOf(privacyLink, ignoreCase = true)
            while (start >= 0) {
                addLink(LinkAnnotation.Clickable(PRIVACY_LINK_TAG, linkStyle) { onOpenPrivacy() }, start, start + privacyLink.length)
                start = text.indexOf(privacyLink, start + privacyLink.length, ignoreCase = true)
            }
        }
    }
}

private const val BULLET = "• "
private const val HELP_IMAGE_WIDTH = 0.62f
private const val PRIVACY_LINK_TAG = "privacy"
private const val BOLD_LEAD_MAX = 45
