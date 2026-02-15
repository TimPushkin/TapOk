package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.timpushkin.tapok.ui.activity.ThreePaneScaffoldRole
import com.github.timpushkin.tapok.ui.activity.calculatePaddingInThreePaneScaffold
import com.github.timpushkin.tapok.ui.activity.isPartitionedHorizontally

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SegmentedListPane(
    directive: PaneScaffoldDirective,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = if (directive.isPartitionedHorizontally()) {
            calculatePaddingInThreePaneScaffold(
                directive,
                ThreePaneScaffoldRole.PRIMARY,
                lowestActiveRole = ThreePaneScaffoldRole.SECONDARY
            )
        } else {
            PaddingValues(16.dp)
        },
        modifier = modifier,
        content = content
    )
}
