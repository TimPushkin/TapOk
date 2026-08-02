package io.github.timpushkin.tapok.ui.activity.rules

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.ui.activity.components.CloseButton
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun RuleEditorScreen(
    ruleId: Long?,
    prefilling: IncompleteRule?,
    onSave: (Rule) -> Unit,
    onClose: () -> Unit,
    vm: RuleEditorViewModel = hiltViewModel(
        key = "$ruleId-$prefilling"
    ) { f: RuleEditorViewModelFactory -> f.create(ruleId, prefilling) }
) {
    var showUnsavedChangesWarning by remember { mutableStateOf(false) }
    if (showUnsavedChangesWarning) {
        UnsavedChangesWarningDialog(
            onSave = {
                vm.launchSaving()
                showUnsavedChangesWarning = false
            },
            onDiscard = onClose,
            onDismiss = { showUnsavedChangesWarning = false }
        )
    }

    val state by vm.uiState.collectAsStateWithLifecycle()
    val loadedState = state as? RuleEditorUiState.Loaded
    val hasUnsavedChanges = loadedState?.status == RuleEditorUiState.Status.EDITED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (ruleId == null) R.string.create_rule else R.string.edit_rule)) },
                navigationIcon = {
                    CloseButton {
                        if (hasUnsavedChanges) {
                            showUnsavedChangesWarning = true
                        } else {
                            onClose()
                        }
                    }
                },
                actions = {
                    TextButton(onClick = vm::launchSaving, enabled = hasUnsavedChanges) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        AnimatedVisibility(
            loadedState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            if (loadedState != null) {
                EditableFields(
                    loadedState,
                    vm.nameState,
                    vm.sourcePackageNameState,
                    vm.viewIdState,
                    vm.containedTextState,
                    vm.packageNameState,
                    vm.classNameState,
                    vm::selectAction,
                    enabled = loadedState.status != RuleEditorUiState.Status.SAVING,
                    contentPadding = PaddingValues(DefaultOuterSpacing)
                )
            }
        }
    }

    // Replace the standard predictive back handler when there are unsaved changes
    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedChangesWarning = true
    }

    // Close after saving completes successfully
    LaunchedEffect(Unit) {
        for (savedRule in vm.savingNotificationChannel) {
            onSave(savedRule)
        }
    }
}

@Composable
private fun UnsavedChangesWarningDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        text = { Text(stringResource(R.string.changes_not_saved)) },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDiscard) { Text(stringResource(R.string.discard)) } }
    )
}

@Composable
private fun EditableFields(
    state: RuleEditorUiState.Loaded,
    nameState: TextFieldState,
    sourcePackageNameState: TextFieldState,
    viewIdState: TextFieldState,
    containedTextState: TextFieldState,
    packageNameState: TextFieldState,
    classNameState: TextFieldState,
    onSelectAction: (Rule.Action) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(DefaultInnerSpacing),
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        val textInputValidity = state.textInputValidity
        item {
            RuleTextField(
                nameState,
                enabled,
                stringResource(R.string.rule_name),
                stringResource(R.string.rule_name_placeholder),
                explanation = stringResource(R.string.rule_name_explanation),
                error = textInputValidity.name.errorText()
            )
        }

        item {
            SectionHeader(
                header = stringResource(R.string.triggering_ui_element),
                subheader = stringResource(R.string.triggering_ui_element_intro)
            )
        }
        item {
            RuleTextField(
                sourcePackageNameState,
                enabled,
                stringResourceWithAsterisk(R.string.window_level_package),
                stringResource(R.string.package_placeholder),
                explanation = stringResource(R.string.window_level_package_explanation),
                supportingText = stringResource(R.string.ref_required),
                error = textInputValidity.sourcePackageName.errorText()
            )
        }

        item {
            SubsectionHeader(
                header = stringResource(R.string.search_criteria),
                subheader = stringResource(R.string.search_criteria_intro)
            )
        }
        item {
            RuleTextField(
                viewIdState,
                enabled,
                stringResourceWithAsterisk(R.string.view_id),
                stringResource(R.string.view_id_placeholder),
                explanation = stringResource(R.string.view_id_explanation),
                supportingText =
                    stringResource(R.string.ref_required_unless_filled_in_another_criterion),
                error = textInputValidity.viewId.errorText()
            )
            RuleTextField(
                containedTextState,
                enabled,
                stringResourceWithAsterisk(R.string.contained_text),
                stringResource(R.string.contained_text_placeholder),
                explanation = stringResource(R.string.contained_text_explanation),
                supportingText =
                    stringResource(R.string.ref_required_unless_filled_in_another_criterion),
                error = textInputValidity.containedText.errorText()
            )
        }

        item {
            SubsectionHeader(
                header = stringResource(R.string.post_search_filters),
                subheader = stringResource(R.string.post_search_filters_intro)
            )
        }
        item {
            RuleTextField(
                packageNameState,
                enabled,
                stringResource(R.string.element_level_package),
                stringResource(R.string.package_placeholder),
                explanation = stringResource(R.string.element_level_package_explanation),
                error = textInputValidity.packageName.errorText()
            )
            RuleTextField(
                classNameState,
                enabled,
                stringResource(R.string.view_class),
                stringResource(R.string.view_class_placeholder),
                explanation = stringResource(R.string.view_class_explanation),
                error = textInputValidity.className.errorText()
            )
        }

        item {
            SectionHeader(
                header = stringResource(R.string.action),
                subheader = stringResource(R.string.action_intro)
            )
        }
        item {
            RuleDropdownMenu(
                selected = state.action,
                enabled = enabled,
                options = Rule.Action.entries,
                getIcon = Rule.Action::icon,
                getName = Rule.Action::name,
                getExplanation = Rule.Action::explanation,
                onSelect = onSelectAction,
                label = stringResourceWithAsterisk(R.string.action),
                supportingText = stringResource(R.string.ref_required)
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun stringResourceWithAsterisk(@StringRes id: Int) = "${stringResource(id)} *"

@Composable
private fun SectionHeader(header: String, subheader: String) {
    Column {
        Text(
            header,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            subheader,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubsectionHeader(header: String, subheader: String) {
    Column {
        Text(
            header,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            subheader,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RuleTextField(
    state: TextFieldState,
    enabled: Boolean,
    label: String,
    placeholder: String,
    explanation: String,
    supportingText: String? = null,
    error: String? = null
) {
    OutlinedTextField(
        state = state,
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = { TextOrSpacer(error ?: supportingText) },
        isError = error != null,
        trailingIcon = {
            if (error == null) {
                val scope = rememberCoroutineScope()
                val tooltipState = rememberTooltipState(isPersistent = true)
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                    tooltip = { PlainTooltip { Text(explanation) } },
                    state = tooltipState
                ) {
                    IconButton(
                        onClick = {
                            if (tooltipState.isVisible) {
                                tooltipState.dismiss()
                            } else {
                                scope.launch { tooltipState.show() }
                            }
                        }
                    ) {
                        if (tooltipState.isVisible) {
                            Icon(
                                painterResource(R.drawable.info_filled),
                                contentDescription = stringResource(R.string.hide_field_explanation)
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.info_outlined),
                                contentDescription = stringResource(R.string.show_field_explanation)
                            )
                        }
                    }
                }
            } else {
                Icon(
                    painterResource(R.drawable.error_filled),
                    contentDescription = stringResource(R.string.invalid_input)
                )
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> RuleDropdownMenu(
    selected: T,
    enabled: Boolean,
    options: List<T>,
    getIcon: @Composable (T) -> Painter,
    getName: @Composable (T) -> String,
    getExplanation: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    label: String,
    supportingText: String? = null
) {
    val textState = rememberTextFieldState(getName(selected))
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(options.indexOf(selected)) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = getName(selected),
            onValueChange = {},
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(getIcon(selected), contentDescription = null)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            supportingText = { TextOrSpacer(supportingText) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupStandardContainerColor,
            shape = MenuDefaults.standaloneGroupShape,
        ) {
            options.forEachIndexed { i, option ->
                val text = getName(option)
                DropdownMenuItem(
                    text = { Text(text) },
                    selected = selectedIndex == i,
                    onClick = {
                        textState.setTextAndPlaceCursorAtEnd(text)
                        expanded = false
                        selectedIndex = i
                        onSelect(option)
                    },
                    shapes = MenuDefaults.itemShape(i, options.size),
                    leadingIcon = {
                        Icon(getIcon(option), contentDescription = null)
                    },
                    selectedLeadingIcon = {
                        Icon(
                            painterResource(R.drawable.check_box_filled),
                            contentDescription = null
                        )
                    },
                    supportingText = { Text(getExplanation(option)) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun TextOrSpacer(
    text: String?,
    spacerHeight: Dp = with(LocalDensity.current) { LocalTextStyle.current.lineHeight.toDp() }
) {
    if (text != null) {
        Text(text)
    } else {
        Spacer(Modifier.height(spacerHeight))
    }
}
