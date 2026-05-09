package io.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.timpushkin.tapok.ui.theme.DefaultPaneSpacing
import io.github.timpushkin.tapok.ui.utils.requiredMinPaneWidth

@Composable
fun RuleListPane(
    selectedRuleId: Long?,
    onEditRule: (Rule?) -> Unit,
    onDeleteRule: (Rule) -> Unit,
    vm: RuleListViewModel = hiltViewModel()
) {
    Scaffold(
        // TODO: hide FAB on content scroll
        floatingActionButton = { CreateRuleFab(onClick = { onEditRule(null) }) },
        containerColor = Color.Transparent
    ) { padding ->
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        if (uiState.rules.isEmpty()) {
            PanePlaceholderText(
                stringResource(R.string.manage_rules_here),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .requiredMinPaneWidth()
            )
        } else {
            SegmentedListPane(
                contentPadding = PaddingValues(DefaultPaneSpacing) + padding,
                modifier = Modifier
                    .consumeWindowInsets(padding)
                    .requiredMinPaneWidth()
            ) {
                val count = uiState.rules.size
                itemsIndexed(uiState.rules, key = { _, rule -> rule.id }) { index, rule ->
                    RuleItem(
                        index,
                        count,
                        rule,
                        selected = rule.id == selectedRuleId,
                        onClick = { onEditRule(rule) },
                        onDelete = {
                            onDeleteRule(rule)
                            vm.deleteRule(rule)
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
    onDelete: () -> Unit
) {
    DeletableSegmentedListItem(
        index,
        count,
        selected = selected,
        onClick = onClick,
        onDelete = onDelete,
        content = { Text(rule.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(
                painterResource(rule.action.drawableRes()),
                contentDescription = stringResource(rule.action.stringRes())
            )
        },
        supportingContent = { Text(rule.viewId, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    )
}
