package io.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.times
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SegmentedListPane(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DefaultOuterSpacing),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding,
        modifier = modifier,
        content = content
    )
}

object SegmentedListPaneDefaults {
    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        contentVariantColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = ListItemDefaults.segmentedColors(
        containerColor = containerColor,
        contentColor = contentColor,
        leadingContentColor = contentVariantColor,
        trailingContentColor = contentVariantColor,
        overlineContentColor = contentVariantColor,
        supportingContentColor = contentVariantColor
    )

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun GroupSpacer() {
        Spacer(Modifier.height(DefaultOuterSpacing - 2 * ListItemDefaults.SegmentedGap))
    }
}
