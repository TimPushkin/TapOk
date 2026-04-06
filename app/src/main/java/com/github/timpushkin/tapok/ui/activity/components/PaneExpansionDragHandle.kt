package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.PaneScaffoldScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun PaneScaffoldScope.PaneExpansionDragHandle(state: PaneExpansionState) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier = Modifier.paneExpansionDraggable(
            state,
            LocalMinimumInteractiveComponentSize.current,
            interactionSource
        ),
        interactionSource = interactionSource
    )
}
