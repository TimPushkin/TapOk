package com.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleNameValidity
import com.github.timpushkin.tapok.data.rules.ViewIdValidity
import com.github.timpushkin.tapok.ui.activity.components.NavigateBackButton
import com.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import com.github.timpushkin.tapok.ui.theme.DefaultSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorPane(
    ruleId: Long?,
    onCreated: (Long) -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean,
    vm: RuleEditorViewModel = hiltViewModel(
        key = ruleId.toString()
    ) { f: RuleEditorViewModelFactory -> f.create(ruleId) }
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (ruleId == null) R.string.create_rule else R.string.edit_rule)) },
                navigationIcon = { if (showBackButton) NavigateBackButton(onBack) },
                actions = {
                    SaveButton(
                        canSave = (state as? RuleEditorUiState.Loaded)?.allowsSaving() == true,
                        onSave = { onCreated(vm.save().id) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        EditableFields(
            state,
            vm.ruleNameState,
            vm.viewIdState,
            vm::selectAction,
            contentPadding = PaddingValues(DefaultSpacing),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = DefaultSpacing)
        )
    }
}

@Composable
private fun SaveButton(canSave: Boolean, onSave: suspend () -> Unit) {
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
        AnimatedContent(savedRecently) {
            Text(stringResource(if (!it) R.string.save else R.string.done))
        }
    }
}

@Composable
private fun EditableFields(
    state: RuleEditorUiState,
    ruleNameState: TextFieldState,
    viewIdState: TextFieldState,
    onSelectAction: (Rule.Action) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(DefaultSpacing),
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
                viewIdState,
                stringResource(R.string.view_id),
                stringResource(R.string.view_id_placeholder),
                loadedState?.viewIdValidity?.toErrorText()
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
private fun ViewIdValidity.toErrorText(): String? {
    val resId = when (this) {
        ViewIdValidity.OK -> return null
        ViewIdValidity.NO_DIVIDER -> R.string.view_id_error_no_divider
        ViewIdValidity.PACKAGE_ILLEGAL_SEGMENT -> R.string.view_id_error_illegal_package_segment
        ViewIdValidity.PACKAGE_NOT_ENOUGH_SEGMENTS -> R.string.view_id_error_not_enough_package_segments
        ViewIdValidity.VIEW_ILLEGAL_START -> R.string.view_id_error_illegal_name_start
        ViewIdValidity.VIEW_ILLEGAL_PART -> R.string.view_id_error_illegal_name_part
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
