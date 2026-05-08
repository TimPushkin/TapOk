package com.github.timpushkin.tapok.ui.activity.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.github.timpushkin.tapok.AgentService
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ScannerService
import com.github.timpushkin.tapok.ui.theme.DefaultPaneSpacing
import com.github.timpushkin.tapok.ui.theme.DefaultSpacing
import com.github.timpushkin.tapok.utils.openAccessibilityServiceSettings
import com.github.timpushkin.tapok.utils.rememberIsAccessibilityServiceEnabled
import kotlin.reflect.KClass

@Composable
fun HomePane(openTutorial: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(DefaultPaneSpacing),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(DefaultPaneSpacing)
                .widthIn(min = 600.dp)
                .width(IntrinsicSize.Min)
        ) {
            ServiceStatusCard(stringResource(R.string.agent_service), AgentService::class)
            ServiceStatusCard(stringResource(R.string.scanner_service), ScannerService::class)
            TutorialSuggestion(openTutorial)
        }
    }
}

@Composable
private fun ServiceStatusCard(serviceName: String, serviceClass: KClass<*>) {
    val context = LocalContext.current
    val serviceEnabled = rememberIsAccessibilityServiceEnabled(serviceClass)
    OutlinedCard(
        onClick = { context.openAccessibilityServiceSettings(serviceClass) },
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (serviceEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(DefaultSpacing * 2)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    serviceName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painterResource(R.drawable.open_in_new),
                    contentDescription = null
                )
            }

            Text(
                stringResource(if (serviceEnabled) R.string.on else R.string.off),
                color = LocalContentColor.current.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TutorialSuggestion(openTutorial: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DefaultSpacing / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(
                painterResource(R.drawable.info_outlined),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.tutorial_suggestion_1))
                    withLink(LinkAnnotation.Clickable("open_tutorial") { openTutorial() }) {
                        append(stringResource(R.string.tutorial_suggestion_2))
                    }
                    append(stringResource(R.string.tutorial_suggestion_3))
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
