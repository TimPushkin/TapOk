package io.github.timpushkin.tapok.ui.activity.rules

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedListItem
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
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.isHorizontallyPartitioned
import io.github.timpushkin.tapok.ui.utils.requiredMinPaneWidth
import kotlinx.coroutines.launch

@Composable
fun RuleViewerPane(
    ruleId: Long,
    onEdit: (Rule) -> Unit,
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
                    IconButton({ rule?.let { onEdit(it) } }, enabled = rule != null) {
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
                RuleFields(rule, contentPadding = PaddingValues(DefaultOuterSpacing))
            }
        }
    }

    // Handle rule deletion
    LaunchedEffect(state) {
        if (state is RuleViewerUiState.NotFound) {
            onBack()
        }
    }
}

@Composable
private fun RuleFields(
    rule: Rule,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val fields = listOfNotNull(
        RuleFieldDescriptor(rule.sourcePackageName, stringResource(R.string.app_package)),
        rule.viewId?.let { RuleFieldDescriptor(it, stringResource(R.string.view_id)) },
        rule.containedText?.let {
            RuleFieldDescriptor(it, stringResource(R.string.contained_text))
        },
        rule.packageName?.let {
            RuleFieldDescriptor(it, stringResource(R.string.element_level_package))
        },
        rule.className?.let { RuleFieldDescriptor(it, stringResource(R.string.view_class)) },
        RuleFieldDescriptor(
            rule.action.name(),
            stringResource(R.string.action),
            rule.action.icon(),
            copyable = false
        )
    )
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        itemsIndexed(fields) { i, field ->
            with(field) {
                if (copyable) {
                    CopyableRuleField(i, fields.size, text, label, leadingIcon)
                } else {
                    RuleField(i, fields.size, text, label, leadingIcon)
                }
            }
        }
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
    text: String,
    label: String,
    leadingIcon: Painter? = null
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    SegmentedListItem(
        content = { Text(text) },
        leadingContent = leadingIcon?.let { { Icon(leadingIcon, contentDescription = null) } },
        supportingContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        onClick = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
            }
        },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = SegmentedListPaneDefaults.colors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RuleField(
    index: Int,
    count: Int,
    text: String,
    label: String,
    leadingIcon: Painter? = null
) {
    SegmentedListItem(
        content = { Text(text) },
        leadingContent = leadingIcon?.let { { Icon(leadingIcon, contentDescription = null) } },
        supportingContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = SegmentedListPaneDefaults.colors(),
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
