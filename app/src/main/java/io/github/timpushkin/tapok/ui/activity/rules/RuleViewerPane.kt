package io.github.timpushkin.tapok.ui.activity.rules

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.ui.activity.components.NavigateBackButton
import io.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPaneDefaults
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.isHorizontallyPartitioned
import io.github.timpushkin.tapok.ui.utils.requiredMinPaneWidth
import kotlinx.coroutines.launch

@Composable
fun RuleViewerPane(
    ruleId: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    directive: PaneScaffoldDirective,
    vm: RuleViewerViewModel = hiltViewModel(
        key = ruleId.toString()
    ) { f: RuleViewerViewModelFactory -> f.create(ruleId) }
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val rule = (state as? RuleViewerUiState.Loaded)?.rule

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (rule != null) {
                        Text(
                            rule.name.ifEmpty { stringResource(R.string.unnamed_rule) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current.let {
                                if (rule.name.isNotEmpty()) it else it.copy(alpha = 0.38f)
                            }
                        )
                    }
                },
                navigationIcon = {
                    if (!directive.isHorizontallyPartitioned()) {
                        NavigateBackButton(onBack)
                    }
                },
                actions = {
                    IconButton(onEdit, enabled = rule != null) {
                        Icon(
                            painterResource(R.drawable.edit_outlined),
                            contentDescription = stringResource(R.string.edit_rule)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = Modifier
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
            .requiredMinPaneWidth()
    ) { padding ->
        AnimatedVisibility(
            rule != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            if (rule != null) {
                ViewerColumn(
                    rule,
                    onSwitchActivation = vm::switchActivation,
                    onDelete = {
                        onDelete()
                        // Actual deletion happens only when rule list is visible
                        onBack()
                    },
                    contentPadding = PaddingValues(DefaultOuterSpacing)
                )
            }
        }
    }

    // Handle concurrent rule deletion
    LaunchedEffect(state) {
        if (state is RuleViewerUiState.NotFound) {
            onBack()
        }
    }
}

@Composable
private fun ViewerColumn(
    rule: Rule,
    onSwitchActivation: () -> Unit,
    onDelete: () -> Unit,
    contentPadding: PaddingValues
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = Modifier
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection)
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(contentPadding.calculateTopPadding()))

        Column(verticalArrangement = Arrangement.spacedBy(DefaultInnerSpacing)) {
            val fields = listOfNotNull(
                RuleFieldDescriptor(rule.sourcePackageName, stringResource(R.string.app_package)),
                rule.viewId?.let { RuleFieldDescriptor(it, stringResource(R.string.view_id)) },
                rule.containedText?.let {
                    RuleFieldDescriptor(it, stringResource(R.string.contained_text))
                },
                rule.packageName?.let {
                    RuleFieldDescriptor(it, stringResource(R.string.element_level_package))
                },
                rule.className?.let {
                    RuleFieldDescriptor(it, stringResource(R.string.view_class))
                },
                RuleFieldDescriptor(
                    rule.action.name(),
                    stringResource(R.string.action),
                    rule.action.icon(),
                    copyable = false
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                fields.forEachIndexed { i, field ->
                    with(field) {
                        if (copyable) {
                            CopyableRuleField(i, fields.size, text, label, leadingIcon)
                        } else {
                            RuleField(i, fields.size, text, label, leadingIcon)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ViewerItem(
                    0, 2,
                    "Active",
                    trailingContent = {
                        Switch(
                            rule.isActive,
                            onCheckedChange = null,
                            //modifier = Modifier.focusProperties { canFocus = false }
                        )
                    },
                    onClick = onSwitchActivation,
                    colors = SegmentedListPaneDefaults.colors(containerColor = containerColor)
                )
                ViewerItem(
                    1, 2,
                    stringResource(R.string.delete),
                    onClick = onDelete,
                    colors = SegmentedListPaneDefaults.colors(
                        containerColor = containerColor,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }

        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

private data class RuleFieldDescriptor(
    val text: String,
    val label: String,
    val leadingIcon: Painter? = null,
    val copyable: Boolean = true
)

@Composable
private fun CopyableRuleField(
    index: Int,
    count: Int,
    value: String,
    label: String,
    leadingIcon: Painter? = null
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    ViewerItem(index, count, value, label, leadingIcon) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(value, value)))
        }
    }
}

@Composable
private fun RuleField(
    index: Int,
    count: Int,
    value: String,
    label: String,
    leadingIcon: Painter? = null
) {
    ViewerItem(index, count, value, label, leadingIcon)
}

@Composable
private fun ViewerItem(
    index: Int,
    count: Int,
    value: String,
    label: String? = null,
    leadingIcon: Painter? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = SegmentedListPaneDefaults.colors(),
    onClick: () -> Unit = {}
) {
    SegmentedListItem(
        content = { Text(value) },
        leadingContent = leadingIcon?.let {
            { Icon(leadingIcon, contentDescription = null) }
        },
        supportingContent = label?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = trailingContent,
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun RuleViewerPlaceholder() {
    PanePlaceholderText(
        stringResource(R.string.select_rule_to_view),
        modifier = Modifier.requiredMinPaneWidth()
    )
}
