package io.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.ui.activity.components.DeletableSegmentedListItem
import io.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPane
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPaneDefaults
import io.github.timpushkin.tapok.ui.activity.components.rememberDeletableItemEpochs
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.requiredMinPaneWidth

@Composable
fun RuleListPane(
    selectedRuleId: Long?,
    onCreateRule: () -> Unit,
    onSelectRule: (Rule) -> Unit,
    vm: RuleListViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = retain { SnackbarHostState() }

    val listState = rememberLazyListState()
    var showFab by remember { mutableStateOf(true) } // Not savable to reset on rotation
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            // Hide when scrolling down, show when scrolling up
            if (currentIndex != previousIndex) {
                showFab = currentIndex < previousIndex
            } else if (currentOffset != previousScrollOffset) {
                showFab = currentOffset < previousScrollOffset
            }
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(showFab) {
                CreateRuleFab(onClick = onCreateRule)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.rules.isEmpty()) {
            PanePlaceholderText(
                stringResource(R.string.manage_rules_here),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .requiredMinPaneWidth()
            )
        } else {
            val epochs = rememberDeletableItemEpochs(uiState.rules.map { it.id })
            SegmentedListPane(
                state = listState,
                contentPadding = PaddingValues(DefaultOuterSpacing) + padding,
                modifier = Modifier
                    .consumeWindowInsets(padding)
                    .requiredMinPaneWidth()
            ) {
                val count = uiState.rules.size
                itemsIndexed(
                    uiState.rules,
                    key = { _, rule -> "${rule.id}-${epochs[rule.id]}" }
                ) { index, rule ->
                    val deletionMessage = stringResource(
                        R.string.deleted_rule,
                        rule.name.ifEmpty { stringResource(R.string.unnamed_rule) }
                    )
                    val undoString = stringResource(R.string.undo)
                    RuleItem(
                        index,
                        count,
                        rule,
                        selected = rule.id == selectedRuleId,
                        onClick = { onSelectRule(rule) },
                        onActiveChange = { active ->
                            assert(active != rule.isActive)
                            vm.switchRuleActivation(rule)
                        },
                        onDelete = {
                            vm.launchDeleteWithUndo(rule) {
                                snackbarHostState.showSnackbar(
                                    deletionMessage,
                                    undoString,
                                    duration = SnackbarDuration.Short
                                ) == SnackbarResult.ActionPerformed
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateRuleFab(onClick: () -> Unit) {
    MediumFloatingActionButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.add),
            contentDescription = stringResource(R.string.create_rule)
        )
    }
}

@Composable
private fun RuleItem(
    index: Int,
    count: Int,
    rule: Rule,
    selected: Boolean,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val colors = SegmentedListPaneDefaults.colors()
    DeletableSegmentedListItem(
        index,
        count,
        selected = selected,
        onClick = onClick,
        onDelete = onDelete,
        content = {
            Text(
                rule.name.ifEmpty { stringResource(R.string.unnamed_rule) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.run {
                    if (rule.name.isNotEmpty()) contentColor else disabledContentColor
                }
            )
        },
        leadingContent = {
            Icon(rule.action.icon(), contentDescription = rule.action.explanation())
        },
        trailingContent = { Switch(rule.isActive, onActiveChange) },
        supportingContent = {
            Text(rule.sourcePackageName, maxLines = 1, overflow = TextOverflow.StartEllipsis)
        },
        colors = colors
    )
}
