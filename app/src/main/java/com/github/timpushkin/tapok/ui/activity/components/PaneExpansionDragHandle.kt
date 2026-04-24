package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.PaneScaffoldScope
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun PaneScaffoldScope.PaneExpansionDragHandle(state: PaneExpansionState) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier = Modifier
            .paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource
            )
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets),
        interactionSource = interactionSource
    )
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun rememberAnchoredPaneExpansionState(directive: PaneScaffoldDirective): PaneExpansionState {
    val anchorOffset = with(directive) {
        defaultPanePreferredWidth + (horizontalPartitionSpacerSize / 2)
    }
    return rememberPaneExpansionState(
        anchors = listOf(
            PaneExpansionAnchor.Proportion(0f),
            PaneExpansionAnchor.Offset.fromStart(anchorOffset),
            PaneExpansionAnchor.Proportion(0.5f),
            PaneExpansionAnchor.Offset.fromEnd(anchorOffset),
            PaneExpansionAnchor.Proportion(1f),
        ),
        initialAnchoredIndex = 1
    )
}
