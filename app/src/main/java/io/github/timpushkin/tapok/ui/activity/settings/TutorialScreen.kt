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
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.agent.AgentService
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.appendParagraphSpacer
import io.github.timpushkin.tapok.ui.utils.openAccessibilityServiceSettings
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.lerp as lerpGraphics
import androidx.compose.ui.unit.lerp as lerpUnit

private const val TUTORIAL_PAGES_NUm = 5

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
        val state = rememberPagerState { TUTORIAL_PAGES_NUm }

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
// inline to allow composable code in the text builder
private inline fun TutorialCard(
    headline: String,
    bodyInlineContent: Map<String, InlineTextContent> = emptyMap(),
    bodyBuilder: AnnotatedString.Builder.() -> Unit
) {
    // Evaluate here to be able to not crossinline bodyBuilder
    val body = buildAnnotatedString(bodyBuilder)

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

                Text(body, inlineContent = bodyInlineContent)
            }
        }
    }
}

@Composable
private fun Intro() {
    TutorialCard(stringResource(R.string.tutorial_intro_headline)) {
        append(stringResource(R.string.tutorial_intro_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_intro_body_2))
    }
}

private val ICON_PLACEHOLDER = Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center)

@Composable
private fun Step1(accessibilityUsageAccepted: Boolean, onAccessibilityUsageAccepted: () -> Unit) {
    val context = LocalContext.current

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

    TutorialCard(stringResource(R.string.tutorial_step1_headline)) {
        append(stringResource(R.string.tutorial_step1_body_1))
        appendParagraphSpacer()

        val updatableAccessibilityUsageAccepted by rememberUpdatedState(accessibilityUsageAccepted)
        withLink(
            LinkAnnotation.Clickable("agent_service_link") {
                if (updatableAccessibilityUsageAccepted) {
                    context.openAccessibilityServiceSettings(AgentService::class)
                } else {
                    showAccessibilityDisclosureDialog = true
                }
            }
        ) { append(stringResource(R.string.tutorial_step1_body_2_1)) }
        append(stringResource(R.string.tutorial_step1_body_2_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step1_body_3))
    }
}

@Composable
private fun Step2() {
    val scanIconId = "scan_icon"
    TutorialCard(
        stringResource(R.string.tutorial_step2_headline),
        mapOf(
            scanIconId to InlineTextContent(ICON_PLACEHOLDER) {
                Icon(painterResource(R.drawable.screenshot_frame), contentDescription = null)
            }
        )
    ) {
        append(stringResource(R.string.tutorial_step2_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step2_body_2_1))
        appendInlineContent(scanIconId, stringResource(R.string.scan_screen))
        append(stringResource(R.string.tutorial_step2_body_2_2))
    }
}

@Composable
private fun Step3() {
    val expandIconId = "expand_icon"
    TutorialCard(
        stringResource(R.string.tutorial_step3_headline),
        mapOf(
            expandIconId to InlineTextContent(ICON_PLACEHOLDER) {
                Icon(painterResource(R.drawable.keyboard_arrow_down), contentDescription = null)
            }
        )
    ) {
        append(stringResource(R.string.tutorial_step3_body_1_1))
        appendInlineContent(expandIconId, stringResource(R.string.toggle_button))
        append(stringResource(R.string.tutorial_step3_body_1_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step3_body_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step3_body_3))
    }
}

@Composable
private fun Step4() {
    TutorialCard(stringResource(R.string.tutorial_step4_headline)) {
        append(stringResource(R.string.tutorial_step4_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step4_body_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step4_body_3))
    }
}
