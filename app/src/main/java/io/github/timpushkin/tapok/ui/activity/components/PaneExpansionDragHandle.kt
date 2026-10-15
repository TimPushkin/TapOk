// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
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
            .background(MaterialTheme.colorScheme.background)
            .fillMaxHeight(),
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
