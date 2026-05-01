package com.github.timpushkin.tapok.ui.activity.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.github.timpushkin.tapok.AgentService
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ScannerService
import com.github.timpushkin.tapok.ui.theme.DefaultPaneSpacing
import com.github.timpushkin.tapok.ui.utils.appendParagraphSpacer
import com.github.timpushkin.tapok.ui.utils.openAccessibilityServiceSettings
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.lerp as lerpGraphics
import androidx.compose.ui.unit.lerp as lerpUnit

private val TUTORIAL_PAGES: List<@Composable (Modifier) -> Unit> = listOf(
    ::Intro, ::Step1, ::Step2, ::Step3, ::Step4, ::Step5
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // Add insets padding even if it has already been consumed. This is needed to not have
            // the contents jump when the outer NavigationSuiteScaffold's bottom navigation
            // shows/hides and starts/stops consuming the insets.
            .padding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())
            .consumeWindowInsets(ScaffoldDefaults.contentWindowInsets)
    ) {
        val state = rememberPagerState { TUTORIAL_PAGES.size }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(state, modifier = Modifier.weight(1f, fill = false)) { i ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TUTORIAL_PAGES[i](Modifier.padding(DefaultPaneSpacing))
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
        modifier = Modifier.padding(DefaultPaneSpacing)
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
            .width(256.dp)
            .padding(vertical = DefaultPaneSpacing)
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
// inline to allow composable code in the text builder
private inline fun TutorialCard(
    headline: String,
    modifier: Modifier = Modifier,
    bodyBuilder: AnnotatedString.Builder.() -> Unit
) {
    // Evaluate here to be able to not crossinline bodyBuilder
    val body = buildAnnotatedString(bodyBuilder)

    val size = 512.dp
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.size(size)
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

                Text(body)
            }
        }
    }
}

@Composable
private fun Intro(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_intro_headline), modifier) {
        append(stringResource(R.string.tutorial_intro_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_intro_body_2))
    }
}

private const val LINK_SCANNER = "scanner_service_link"
private const val LINK_AGENT = "agent_service_link"

@Composable
private fun Step1(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_step1_headline), modifier) {
        append(stringResource(R.string.tutorial_step1_body_1))
        appendParagraphSpacer()

        val context = LocalContext.current
        withLink(
            LinkAnnotation.Clickable(LINK_SCANNER) {
                context.openAccessibilityServiceSettings(ScannerService::class)
            }
        ) { append(stringResource(R.string.tutorial_step1_body_2_1)) }
        append(stringResource(R.string.tutorial_step1_body_2_2))
    }
}

@Composable
private fun Step2(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_step2_headline), modifier) {
        append(stringResource(R.string.tutorial_step2_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step2_body_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step2_body_3_1))
        val context = LocalContext.current
        withLink(
            LinkAnnotation.Clickable(LINK_SCANNER) {
                context.openAccessibilityServiceSettings(ScannerService::class)
            }
        ) {
            append(stringResource(R.string.tutorial_step2_body_3_2))
        }
        append(stringResource(R.string.tutorial_step2_body_3_3))
    }
}

@Composable
private fun Step3(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_step3_headline), modifier) {
        append(stringResource(R.string.tutorial_step3_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step3_body_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step3_body_3))
    }
}

@Composable
private fun Step4(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_step4_headline), modifier) {
        append(stringResource(R.string.tutorial_step4_body_1))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step4_body_2))
    }
}

@Composable
private fun Step5(modifier: Modifier = Modifier) {
    TutorialCard(stringResource(R.string.tutorial_step5_headline), modifier) {
        append(stringResource(R.string.tutorial_step5_body_1))
        appendParagraphSpacer()

        val context = LocalContext.current
        withLink(
            LinkAnnotation.Clickable(LINK_AGENT) {
                context.openAccessibilityServiceSettings(AgentService::class)
            }
        ) {
            append(stringResource(R.string.tutorial_step5_body_2_1))
        }
        append(stringResource(R.string.tutorial_step5_body_2_2))
        appendParagraphSpacer()

        append(stringResource(R.string.tutorial_step5_body_3))
    }
}
