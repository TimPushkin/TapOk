package com.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.rules.PackageNameValidity
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleNameValidity
import com.github.timpushkin.tapok.data.rules.ViewNameValidity
import com.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import com.github.timpushkin.tapok.ui.activity.components.TitledPane
import com.github.timpushkin.tapok.ui.activity.isPartitionedHorizontally
import com.github.timpushkin.tapok.ui.utils.thenIf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RuleEditorPane(
    ruleId: Long?,
    onClose: () -> Unit,
    onCreated: (Long) -> Unit,
    directive: PaneScaffoldDirective,
    modifier: Modifier = Modifier,
    vm: RuleEditorViewModel = hiltViewModel(
        key = ruleId.toString()
    ) { f: RuleEditorViewModelFactory -> f.create(ruleId) }
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    TitledPane(
        title = {
            Text(stringResource(if (ruleId == null) R.string.create_rule else R.string.edit_rule))
        },
        onClose = onClose,
        actions = {
            SaveButton(
                canSave = (state as? RuleEditorUiState.Loaded)?.allowsSaving() == true,
                onSave = {
                    val rule = vm.save()
                    if (!directive.isPartitionedHorizontally()) onClose() else onCreated(rule.id)
                },
                animate = directive.isPartitionedHorizontally()
            )
        },
        directive = directive,
        modifier = modifier
    ) {
        EditableFields(
            state,
            vm.ruleNameState,
            vm.packageNameState,
            vm.viewNameState,
            vm::selectAction,
            contentPadding = PaddingValues(
                vertical = if (!directive.isPartitionedHorizontally()) 24.dp else 0.dp
            ),
            modifier = Modifier
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
                .thenIf(!directive.isPartitionedHorizontally()) {
                    padding(horizontal = 24.dp)
                }
        )
    }
}

@Composable
private fun SaveButton(canSave: Boolean, onSave: suspend () -> Unit, animate: Boolean) {
    val scope = rememberCoroutineScope()

    var savedRecently by remember { mutableStateOf(false) }
    LaunchedEffect(savedRecently) {
        if (savedRecently) {
            delay(1000)
            savedRecently = false
        }
    }

    TextButton(
        onClick = {
            scope.launch {
                savedRecently = false
                onSave()
                savedRecently = true
            }
        },
        enabled = canSave
    ) {
        AnimatedContent(animate && savedRecently) {
            Text(stringResource(if (!it) R.string.save else R.string.done))
        }
    }
}

@Composable
private fun EditableFields(
    state: RuleEditorUiState,
    ruleNameState: TextFieldState,
    packageNameState: TextFieldState,
    viewNameState: TextFieldState,
    onSelectAction: (Rule.Action) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        val loadedState = state as? RuleEditorUiState.Loaded
        item {
            RuleTextField(
                ruleNameState,
                stringResource(R.string.rule_name),
                stringResource(R.string.rule_name_placeholder),
                loadedState?.ruleNameValidity?.toErrorText()
            )
        }
        item {
            RuleTextField(
                packageNameState,
                stringResource(R.string.package_name),
                stringResource(R.string.package_name_placeholder),
                loadedState?.packageNameValidity?.toErrorText()
            )
        }
        item {
            RuleTextField(
                viewNameState,
                stringResource(R.string.view_name),
                stringResource(R.string.view_name_placeholder),
                loadedState?.viewNameValidity?.toErrorText()
            )
        }
        item {
            RuleDropdownMenu(
                selected = loadedState?.action ?: Rule.Action.CLICK,
                options = Rule.Action.entries,
                toStringRes = Rule.Action::stringRes,
                toDrawableRes = Rule.Action::drawableRes,
                onSelect = onSelectAction,
                label = stringResource(R.string.action)
            )
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
    error: String?
) {
    OutlinedTextField(
        state = state,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        lineLimits = TextFieldLineLimits.SingleLine,
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(painterResource(toDrawableRes(selected)), contentDescription = null)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true
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
                    selectedLeadingIcon = {
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

@Composable
fun RuleEditorPlaceholder(modifier: Modifier = Modifier) {
    PanePlaceholderText(
        stringResource(R.string.select_rule_or_create_one),
        modifier = modifier.fillMaxSize()
    )
}
