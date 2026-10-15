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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.times
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing

@Composable
fun SegmentedListPane(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(DefaultOuterSpacing),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = state,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding,
        modifier = modifier,
        content = content
    )
}

object SegmentedListPaneDefaults {
    @Composable
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
    fun GroupSpacer() {
        Spacer(Modifier.height(DefaultOuterSpacing - 2 * ListItemDefaults.SegmentedGap))
    }
}
