package com.github.timpushkin.tapok.ui.activity.rules

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.rules.PackageNameValidity
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleNameValidity
import com.github.timpushkin.tapok.data.rules.ViewNameValidity
import com.github.timpushkin.tapok.ui.activity.components.DeletableSegmentedListItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RulesScreen(modifier: Modifier = Modifier, vm: RulesViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    uiState.ruleEditorState?.let {
        RuleEditingDialog(
            it,
            vm.ruleNameState,
            vm.packageNameState,
            vm.viewNameState,
            vm::editRuleAction,
            vm::completeRuleEditing,
            vm::cancelRuleEditing
        )
    }

    if (uiState.rules.isEmpty()) {
        NoRulesBanner(modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        val count = uiState.rules.size
        itemsIndexed(uiState.rules, key = { _, rule -> rule.id }) { index, rule ->
            RuleItem(
                index,
                count,
                rule,
                onClick = { vm.startRuleEditing(rule) },
                onDelete = { vm.deleteRule(rule) }
            )
        }
    }
}

@Composable
private fun NoRulesBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.manage_rules_here),
            modifier = Modifier.fillMaxWidth(0.66f),
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RuleItem(
    index: Int, count: Int, rule: Rule, onClick: () -> Unit, onDelete: () -> Unit
) {
    DeletableSegmentedListItem(
        index,
        count,
        onClick = onClick,
        onDelete = onDelete,
        content = { Text(rule.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(
                painterResource(rule.action.drawableRes()),
                contentDescription = stringResource(rule.action.stringRes())
            )
        },
        supportingContent = {
            Text(rule.viewId, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    )
}

@DrawableRes
private fun Rule.Action.drawableRes(): Int = when (this) {
    Rule.Action.CLICK -> R.drawable.touch_app_outlined
    Rule.Action.MUTE_WHEN_PRESENT -> R.drawable.volume_off_outlined
}

@StringRes
private fun Rule.Action.stringRes() = when (this) {
    Rule.Action.CLICK -> R.string.click
    Rule.Action.MUTE_WHEN_PRESENT -> R.string.mute_when_present
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditingDialog(
    state: RulesUiState.RuleEditorState,
    ruleNameState: TextFieldState,
    packageNameState: TextFieldState,
    viewNameState: TextFieldState,
    onActionSelect: (Rule.Action) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.width(IntrinsicSize.Min),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    stringResource(
                        if (state.originalRule == null) {
                            R.string.create_rule
                        } else {
                            R.string.edit_rule
                        }
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColumnWithScrollClues(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuleTextField(
                        ruleNameState,
                        stringResource(R.string.rule_name),
                        stringResource(R.string.rule_name_placeholder),
                        R.drawable.label_outlined,
                        state.ruleNameValidity?.toErrorText()
                    )
                    RuleTextField(
                        packageNameState,
                        stringResource(R.string.package_name),
                        stringResource(R.string.package_name_placeholder),
                        R.drawable.code_blocks_outlined,
                        state.packageNameValidity?.toErrorText()
                    )
                    RuleTextField(
                        viewNameState,
                        stringResource(R.string.view_name),
                        stringResource(R.string.view_name_placeholder),
                        R.drawable.code_outlined,
                        state.viewNameValidity?.toErrorText()
                    )
                    RuleDropdownMenu(
                        selected = state.action,
                        options = Rule.Action.entries,
                        toStringRes = Rule.Action::stringRes,
                        toDrawableRes = Rule.Action::drawableRes,
                        onSelect = onActionSelect,
                        label = stringResource(R.string.action)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onSave, enabled = state.allowsCompletion()) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnWithScrollClues(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier.verticalScroll(scrollState),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )

        val canScrollUp by remember { derivedStateOf { scrollState.value > 0 } }
        Crossfade(canScrollUp) { show ->
            if (show) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outlineVariant,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        val canScrollDown by remember {
            derivedStateOf { scrollState.value < scrollState.maxValue }
        }
        Crossfade(
            canScrollDown,
            modifier = Modifier.align(Alignment.BottomStart)
        ) { show ->
            if (show) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun RuleNameValidity.toErrorText() = when (this) {
    RuleNameValidity.OK, RuleNameValidity.EMPTY -> null
}

@Composable
private fun PackageNameValidity.toErrorText(): String? {
    val resId = when (this) {
        PackageNameValidity.OK -> return null
        PackageNameValidity.ILLEGAL_SEGMENT -> R.string.package_name_error_illegal_segment
        PackageNameValidity.NOT_ENOUGH_SEGMENTS -> R.string.package_name_error_not_enough_segments
    }
    return stringResource(resId)
}

@Composable
private fun ViewNameValidity.toErrorText(): String? {
    val resId = when (this) {
        ViewNameValidity.OK, ViewNameValidity.EMPTY -> return null
        ViewNameValidity.ILLEGAL_START -> R.string.view_name_error_illegal_start
        ViewNameValidity.ILLEGAL_PART -> R.string.view_name_error_illegal_part
    }
    return stringResource(resId)
}

@Composable
private fun RuleTextField(
    state: TextFieldState,
    label: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    error: String?
) {
    OutlinedTextField(
        state = state,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(iconRes), contentDescription = null) },
        lineLimits = TextFieldLineLimits.SingleLine,
        supportingText = error?.let { { Text(it) } },
        isError = error != null
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> RuleDropdownMenu(
    selected: T,
    options: List<T>,
    toStringRes: (T) -> Int,
    toDrawableRes: (T) -> Int,
    onSelect: (T) -> Unit,
    label: String
) {
    val textState = rememberTextFieldState(stringResource(toStringRes(selected)))
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(options.indexOf(selected)) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(toStringRes(selected)),
            onValueChange = {},
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(painterResource(toDrawableRes(selected)), contentDescription = null)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            options.forEachIndexed { i, option ->
                val text = stringResource(toStringRes(option))
                DropdownMenuItem(
                    text = { Text(text) },
                    selected = selectedIndex == i,
                    onClick = {
                        textState.setTextAndPlaceCursorAtEnd(text)
                        selectedIndex = i
                        onSelect(option)
                    },
                    shapes = MenuDefaults.itemShape(i, options.size),
                    leadingIcon = {
                        Icon(painterResource(toDrawableRes(option)), contentDescription = null)
                    },
                    checkedLeadingIcon = {
                        Icon(
                            painterResource(R.drawable.check_box_filled),
                            contentDescription = null
                        )
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
