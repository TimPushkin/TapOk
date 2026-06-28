package io.github.timpushkin.tapok.ui.activity.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.agent.AgentService
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.openAccessibilityServiceSettings
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.lerp as lerpGraphics
import androidx.compose.ui.unit.lerp as lerpUnit

private const val TUTORIAL_PAGES_NUM = 5

@Composable
fun TutorialScreen(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // Add insets padding even if it has already been consumed. This is needed to not have
            // the contents jump when the outer NavigationSuiteScaffold's bottom navigation
            // shows/hides and starts/stops consuming the insets.
            .padding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())
            .consumeWindowInsets(ScaffoldDefaults.contentWindowInsets)
    ) {
        val state = rememberPagerState { TUTORIAL_PAGES_NUM }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(DefaultOuterSpacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(state, modifier = Modifier.weight(1f, fill = false)) { i ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (i) {
                        0 -> Intro()
                        1 -> {
                            val a11yUsageAccepted by viewModel.isAccessibilityUsageAccepted
                                .collectAsStateWithLifecycle()
                            Step1(a11yUsageAccepted, viewModel::markAccessibilityUsageAccepted)
                        }

                        2 -> Step2()
                        3 -> Step3()
                        4 -> Step4()
                        else -> throw IllegalArgumentException("Excess tutorial page")
                    }
                }
            }
            PageIndicator(state)
        }

        // Not using state.canScrollForward because its value is wrong during the first frame
        DoneButton(completed = state.currentPage == state.pageCount - 1, onClick = onDone)
    }
}

@Composable
private fun PageIndicator(state: PagerState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = DefaultOuterSpacing)
    ) {
        repeat(state.pageCount) { page ->
            // How close is this page to being the current, from 0 to 1, 1 means being the current
            val progressToCurrent by remember {
                derivedStateOf {
                    val pageOffset = ((state.currentPage - page) + state.currentPageOffsetFraction)
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
}

@Composable
private fun DoneButton(completed: Boolean, onClick: () -> Unit) {
    Crossfade(
        completed,
        modifier = Modifier
            .padding(DefaultOuterSpacing)
            .width(256.dp)
    ) { completed ->
        if (completed) {
            Button(onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.complete_tutorial))
            }
        } else {
            OutlinedButton(onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.skip_tutorial))
            }
        }
    }
}

@Composable
private fun TutorialCard(
    headline: String,
    bodyParagraphs: Array<CharSequence>,
    bodyLinks: Iterable<LinkAnnotation.Clickable> = emptyList(),
    bodyInlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    val size = 512.dp
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .padding(horizontal = DefaultOuterSpacing)
            .size(size)
    ) {
        Column {
            // TODO: place some graphics
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    .fillMaxWidth()
                    // Take not more than 40% of the height, less if the text below needs more
                    .height(size * 0.4f)
                    .weight(1f, fill = false)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    headline,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    rememberAnnotatedString(bodyParagraphs) {
                        appendAnnotatedParagraphs(bodyParagraphs, bodyLinks)
                    },
                    inlineContent = bodyInlineContent
                )
            }
        }
    }
}

private val ICON_PLACEHOLDER = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center)

@Composable
private fun Intro() {
    TutorialCard(
        headline = stringResource(R.string.tutorial_intro_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_intro_body)
    )
}

@Composable
private fun Step1(accessibilityUsageAccepted: Boolean, onAccessibilityUsageAccepted: () -> Unit) {
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
private fun Step2() {
    TutorialCard(
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
private fun Step3() {
    TutorialCard(
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
private fun Step4() {
    TutorialCard(
        headline = stringResource(R.string.tutorial_step4_headline),
        bodyParagraphs = annotatedStringArrayResource(R.array.tutorial_step4_body)
    )
}
