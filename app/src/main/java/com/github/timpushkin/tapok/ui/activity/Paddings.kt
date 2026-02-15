package com.github.timpushkin.tapok.ui.activity

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.ui.unit.dp

fun PaneScaffoldDirective.isPartitionedHorizontally() = maxHorizontalPartitions > 1

enum class ThreePaneScaffoldRole { PRIMARY, SECONDARY, TERTIARY }

/**
 * Calculates padding for a pane so that padding around all panes together is equal to the padding
 * between them (i.e. the size of horizontal spacers).
 */
fun calculatePaddingInThreePaneScaffold(
    directive: PaneScaffoldDirective,
    role: ThreePaneScaffoldRole,
    lowestActiveRole: ThreePaneScaffoldRole
): PaddingValues {
    val isPrevPaneDisplayed = directive.isPartitionedHorizontally() && when (role) {
        ThreePaneScaffoldRole.PRIMARY -> false
        ThreePaneScaffoldRole.SECONDARY -> directive.maxHorizontalPartitions > 2 ||
            lowestActiveRole != ThreePaneScaffoldRole.TERTIARY
        ThreePaneScaffoldRole.TERTIARY -> true
    }
    val isNextPaneDisplayed = directive.isPartitionedHorizontally() && when (role) {
        ThreePaneScaffoldRole.PRIMARY -> lowestActiveRole != ThreePaneScaffoldRole.PRIMARY
        ThreePaneScaffoldRole.SECONDARY -> lowestActiveRole == ThreePaneScaffoldRole.TERTIARY
        ThreePaneScaffoldRole.TERTIARY -> false
    }

    val padding = directive.horizontalPartitionSpacerSize.takeIf {
        directive.isPartitionedHorizontally()
    } ?: 0.dp

    return PaddingValues(
        start = if (!isPrevPaneDisplayed) padding else 0.dp,
        end = if (!isNextPaneDisplayed) padding else 0.dp,
        top = padding,
        bottom = padding
    )
}
