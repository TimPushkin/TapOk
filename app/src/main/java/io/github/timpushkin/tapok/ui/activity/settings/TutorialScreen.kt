package io.github.timpushkin.tapok.ui.activity.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.agent.AgentService
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.openAccessibilityServiceSettings
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.lerp as lerpGraphics
import androidx.compose.ui.unit.lerp as lerpUnit

private const val TUTORIAL_PAGES_NUM = 5

@Composable
fun TutorialScreen(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val wideLayout = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        // Not applying DefaultOuterSpacing here because it needs to be inside HorizontalPager to
        // separate the pages from each other
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            // Add insets padding even if it has already been consumed. This is needed to not have
            // the contents jump when the outer NavigationSuiteScaffold's bottom navigation
            // shows/hides and starts/stops consuming the insets.
            .padding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())
            .consumeWindowInsets(ScaffoldDefaults.contentWindowInsets)
    ) {
        val state = rememberPagerState { TUTORIAL_PAGES_NUM }
        HorizontalPager(state, modifier = Modifier.weight(1f)) { i ->
            when (i) {
                0 -> Intro(wideLayout)
                1 -> {
                    val a11yUsageAccepted by viewModel.isAccessibilityUsageAccepted
                        .collectAsStateWithLifecycle()
                    Step1(
                        wideLayout,
                        a11yUsageAccepted,
                        viewModel::markAccessibilityUsageAccepted
                    )
                }

                2 -> Step2(wideLayout)
                3 -> Step3(wideLayout)
                4 -> Step4(wideLayout)
                else -> throw IllegalArgumentException("Excess tutorial page")
            }
        }
        Spacer(Modifier.height(DefaultOuterSpacing))
        PageIndicator(state, onSkip = onDone, onComplete = onDone)
    }
}

@Composable
private fun PageIndicator(
    state: PagerState,
    onSkip: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            DefaultInnerSpacing,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(DefaultOuterSpacing)
    ) {
        val scope = rememberCoroutineScope()

        val firstPage = state.currentPage == 0
        IconButton(
            {
                if (firstPage) {
                    onSkip()
                } else {
                    scope.launch { state.animateScrollToPage(state.currentPage - 1) }
                }
            }
        ) {
            AnimatedContent(
                firstPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { firstPage ->
                if (firstPage) {
                    Icon(
                        painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.skip_tutorial)
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.to_previous_page)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(state.pageCount) { page ->
                // How close is this page to being the current, from 0 to 1, 1 means is the current
                val progressToCurrent by remember {
                    derivedStateOf {
                        val pageOffset = state.currentPage - page + state.currentPageOffsetFraction
                        1 - pageOffset.absoluteValue.coerceIn(0f, 1f)
                    }
                }

                val color = with(MaterialTheme.colorScheme) {
                    lerpGraphics(outlineVariant, secondary, progressToCurrent)
                }

                val height = 8.dp
                val width = lerpUnit(height, 4 * height, progressToCurrent)

                Box(
                    Modifier
                        .size(width, height)
                        .background(color, CircleShape)
                )
            }
        }

        val lastPage = state.currentPage == state.pageCount - 1
        FilledTonalIconButton(
            {
                if (lastPage) {
                    onComplete()
                } else {
                    scope.launch { state.animateScrollToPage(state.currentPage + 1) }
                }
            },
            colors = if (lastPage) {
                IconButtonDefaults.filledTonalIconButtonColors()
            } else {
                IconButtonDefaults.iconButtonColors()
            }
        ) {
            AnimatedContent(
                lastPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { lastPage ->
                if (lastPage) {
                    Icon(
                        painterResource(R.drawable.check),
                        contentDescription = stringResource(R.string.complete_tutorial)
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.arrow_forward),
                        contentDescription = stringResource(R.string.to_next_page)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialCard(
    wideLayout: Boolean,
    @DrawableRes iconId: Int,
    headline: String,
    bodyParagraphs: Array<CharSequence>,
    bodyLinks: Iterable<LinkAnnotation.Clickable> = emptyList(),
    bodyInlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    val layoutArrangement = Arrangement.spacedBy(DefaultOuterSpacing)
    val layoutModifier = Modifier.padding(DefaultOuterSpacing)
    if (wideLayout) {
        Row(
            horizontalArrangement = layoutArrangement,
            verticalAlignment = Alignment.CenterVertically,
            modifier = layoutModifier
        ) {
            val elementModifier = Modifier.weight(1f)
            TutorialIcon(iconId, elementModifier)
            TutorialText(headline, bodyParagraphs, bodyLinks, bodyInlineContent, elementModifier)
        }
    } else {
        Column(
            verticalArrangement = layoutArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = layoutModifier
        ) {
            val elementModifier = Modifier.weight(1f)
            TutorialIcon(iconId, elementModifier)
            TutorialText(headline, bodyParagraphs, bodyLinks, bodyInlineContent, elementModifier)
        }
    }
}

@Composable
private fun TutorialIcon(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Image(
        painterResource(id),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        modifier = modifier.size(500.dp)
    )
}

@Composable
private fun TutorialText(
    headline: String,
    bodyParagraphs: Array<CharSequence>,
    bodyLinks: Iterable<LinkAnnotation.Clickable>,
    bodyInlineContent: Map<String, InlineTextContent>,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Column(
            modifier = Modifier.widthIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(
                DefaultInnerSpacing,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                headline,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                rememberAnnotatedString(bodyParagraphs) {
                    appendAnnotatedParagraphs(bodyParagraphs, bodyLinks)
                },
                inlineContent = bodyInlineContent,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

private val ICON_PLACEHOLDER = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center)

@Composable
private fun Intro(wideLayout: Boolean) {
    TutorialCard(
        wideLayout,
        R.drawable.tapok,
        headline = stringResource(R.string.tutorial_intro_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_intro_body)
    )
}

@Composable
private fun Step1(
    wideLayout: Boolean,
    accessibilityUsageAccepted: Boolean,
    onAccessibilityUsageAccepted: () -> Unit
) {
    val context = LocalContext.current

    val updatableAccessibilityUsageAccepted by rememberUpdatedState(accessibilityUsageAccepted)
    var showAccessibilityDisclosureDialog by retain { mutableStateOf(false) }
    if (showAccessibilityDisclosureDialog) {
        AccessibilityDisclosureDialog(
            onAccept = {
                onAccessibilityUsageAccepted()
                context.openAccessibilityServiceSettings(AgentService::class)
            },
            onHide = { showAccessibilityDisclosureDialog = false }
        )
    }

    TutorialCard(
        wideLayout,
        R.drawable.accessibility_new,
        headline = stringResource(R.string.tutorial_step1_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_step1_body),
        bodyLinks = listOf(
            LinkAnnotation.Clickable("agent_settings") {
                if (updatableAccessibilityUsageAccepted) {
                    context.openAccessibilityServiceSettings(AgentService::class)
                } else {
                    showAccessibilityDisclosureDialog = true
                }
            }
        )
    )
}

@Composable
private fun Step2(wideLayout: Boolean) {
    TutorialCard(
        wideLayout,
        R.drawable.screenshot_frame,
        headline = stringResource(R.string.tutorial_step2_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_step2_body),
        bodyInlineContent = mapOf(
            "scan_icon" to InlineTextContent(ICON_PLACEHOLDER) {
                Icon(painterResource(R.drawable.screenshot_frame), contentDescription = null)
            }
        )
    )
}

@Composable
private fun Step3(wideLayout: Boolean) {
    TutorialCard(
        wideLayout,
        R.drawable.home_storage_outlined,
        headline = stringResource(R.string.tutorial_step3_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_step3_body),
        bodyInlineContent = mapOf(
            "expand_icon" to InlineTextContent(ICON_PLACEHOLDER) {
                Icon(painterResource(R.drawable.keyboard_arrow_down), contentDescription = null)
            },
            "create_rule_icon" to InlineTextContent(ICON_PLACEHOLDER) {
                Icon(painterResource(R.drawable.create_rule_outlined), contentDescription = null)
            }
        )
    )
}

@Composable
private fun Step4(wideLayout: Boolean) {
    TutorialCard(
        wideLayout,
        R.drawable.page_info_outlined,
        headline = stringResource(R.string.tutorial_step4_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_step4_body)
    )
}
