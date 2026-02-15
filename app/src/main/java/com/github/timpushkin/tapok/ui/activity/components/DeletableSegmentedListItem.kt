package com.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.timpushkin.tapok.R
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeletableSegmentedListItem(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    selected: Boolean = false,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    content: @Composable (() -> Unit)
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { Background(dismissState) },
        modifier = Modifier.fillMaxSize(),
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() }
    ) {
        SegmentedListItem(
            onClick = onClick,
            content = content,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            selected = selected,
            shapes = ListItemDefaults.segmentedShapes(index, count),
            colors = colors
        )
    }
}

@Composable
private fun Background(state: SwipeToDismissBoxState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        val width = if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
            with(LocalDensity.current) { state.requireOffset().absoluteValue.toDp() }
        } else {
            0.dp
        }
        val color = MaterialTheme.colorScheme.error
        Icon(
            painterResource(R.drawable.delete_outlined),
            contentDescription = stringResource(R.string.delete),
            modifier = Modifier
                .fillMaxHeight()
                .width(width)
                .background(color, shape = RoundedCornerShape(16.dp))
                .wrapContentSize(Alignment.Center),
            tint = MaterialTheme.colorScheme.contentColorFor(color)
        )
    }
}
