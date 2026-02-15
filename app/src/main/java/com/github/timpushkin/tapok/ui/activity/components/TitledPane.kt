package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.ThreePaneScaffoldRole
import com.github.timpushkin.tapok.ui.activity.calculatePaddingInThreePaneScaffold
import com.github.timpushkin.tapok.ui.activity.isPartitionedHorizontally
import com.github.timpushkin.tapok.ui.utils.thenIf

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun TitledPane(
    title: @Composable (() -> Unit),
    onClose: () -> Unit,
    directive: PaneScaffoldDirective,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable (BoxScope.() -> Unit)
) {
    Column(
        modifier = modifier
            .padding(
                calculatePaddingInThreePaneScaffold(
                    directive,
                    ThreePaneScaffoldRole.SECONDARY,
                    lowestActiveRole = ThreePaneScaffoldRole.SECONDARY
                )
            )
            .fillMaxSize()
            .thenIf(directive.isPartitionedHorizontally()) {
                background(
                    MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.largeIncreased
                ).padding(24.dp)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Header(
            title,
            actions,
            onBack = onClose.takeIf { !directive.isPartitionedHorizontally() },
            containerColor = if (!directive.isPartitionedHorizontally()) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            }
        )
        Box(
            modifier = Modifier.consumeWindowInsets(
                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top)
            ),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    title: @Composable (() -> Unit),
    actions: @Composable (RowScope.() -> Unit),
    onBack: (() -> Unit)?,
    containerColor: Color
) {
    TopAppBar(
        title = title,
        actions = actions,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
    )
}
