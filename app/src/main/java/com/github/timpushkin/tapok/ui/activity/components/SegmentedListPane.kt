package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.timpushkin.tapok.ui.theme.DefaultPaneSpacing

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SegmentedListPane(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(DefaultPaneSpacing),
        modifier = modifier,
        content = content
    )
}
