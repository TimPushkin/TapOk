package io.github.timpushkin.tapok.ui.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.timpushkin.tapok.R
import java.io.Serializable
import kotlin.math.absoluteValue

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
    colors: ListItemColors = SegmentedListPaneDefaults.colors(),
    content: @Composable (() -> Unit)
) {
    val dismissState = rememberSwipeToDismissBoxState()
    var deleted by rememberSaveable { mutableStateOf(false) }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { Background(dismissState) },
        modifier = Modifier.fillMaxSize(),
        enableDismissFromStartToEnd = false,
        onDismiss = {
            // onDismiss may be called multiple times even if the dismissal occurred just once
            if (!deleted) {
                deleted = true
                onDelete()
            }
        }
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

/**
 * Returns a map that counts how many times each ID reappeared after deletion from the list.
 *
 * This is intended to be used when implementing the delete-undo pattern. The counts are to be
 * added to item's keys (e.g. "id-count") to make compose create new states for deleted-then-revived
 * items instead of reusing the old states.
 */
@Composable
inline fun <reified T : Serializable> rememberDeletableItemEpochs(
    itemIds: Iterable<T>
): Map<T, Int> {
    val epochs = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<T, Int>() }
    var previousIds by rememberSaveable { mutableStateOf(emptySet<T>()) }
    val currentIds = remember(itemIds) { itemIds.toSet() }
    if (currentIds != previousIds) {
        val newIds = currentIds - previousIds
        for (id in newIds) {
            epochs[id] = (epochs[id] ?: -1) + 1
        }
        previousIds = currentIds
    }
    return epochs
}

inline fun <reified K : Serializable, reified V : Serializable> stateMapSaver() =
    listSaver<SnapshotStateMap<K, V>, Pair<K, V>>(
        save = { it.map { entry -> entry.toPair() } },
        restore = { mutableStateMapOf<K, V>().apply { putAll(it) } }
    )
