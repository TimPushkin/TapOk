package io.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleInputValidity
import io.github.timpushkin.tapok.ui.activity.components.NavigateBackButton
import io.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.requiredMinPaneWidth

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun RuleEditorPane(
    ruleId: Long?,
    onBack: () -> Unit,
    vm: RuleEditorViewModel = hiltViewModel(
        key = ruleId.toString()
    ) { f: RuleEditorViewModelFactory -> f.create(ruleId) }
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val loadedState = state as? RuleEditorUiState.Loaded

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (ruleId == null) R.string.create_rule else R.string.edit_rule)) },
                navigationIcon = {
                    // TODO: obtain PaneScaffoldDirective from composition-local?
                    val singlePane = calculatePaneScaffoldDirective(
                        currentWindowAdaptiveInfoV2()
                    ).maxHorizontalPartitions == 1
                    if (singlePane) NavigateBackButton(onBack)
                },
                actions = {
                    SaveButton(
                        onClick = vm::launchSaving,
                        enabled = loadedState?.isSaving == false,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
            .requiredMinPaneWidth()
    ) { padding ->
        EditableFields(
            state,
            vm.nameState,
            vm.sourcePackageNameState,
            vm.viewIdState,
            vm.containedTextState,
            vm.packageNameState,
            vm.classNameState,
            vm::selectAction,
            enabled = loadedState?.isSaving == false,
            contentPadding = PaddingValues(DefaultOuterSpacing),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        )
    }

    // Notify when saving completes successfully
    val savedMessage = stringResource(R.string.saved)
    LaunchedEffect(Unit) {
        for (_ in vm.savingNotificationChannel) {
            if (ruleId != null) {
                // Updated existing rule
                snackbarHostState.showSnackbar(savedMessage)
            } else {
                // Created a new rule
                onBack()
            }
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, enabled: Boolean) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun EditableFields(
    state: RuleEditorUiState,
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
        val textInputValidity = (state as? RuleEditorUiState.Loaded)?.textInputValidity
        // TODO: explanations for each field
        item {
            RuleTextField(
                nameState,
                enabled,
                stringResource(R.string.rule_name),
                stringResource(R.string.rule_name_placeholder),
                error = textInputValidity?.name?.toErrorText()
            )
        }
        item {
            RuleTextField(
                sourcePackageNameState,
                enabled,
                stringResource(R.string.window_level_package_ref),
                stringResource(R.string.app_placeholder),
                supportingText = stringResource(R.string.ref_required),
                error = textInputValidity?.sourcePackageName?.toErrorText()
            )
        }
        item {
            RuleTextField(
                viewIdState,
                enabled,
                stringResource(R.string.view_id_ref),
                stringResource(R.string.view_id_placeholder),
                supportingText = stringResource(R.string.ref_required_unless_contained_text_filled),
                error = textInputValidity?.viewId?.toErrorText()
            )
        }
        item {
            RuleTextField(
                containedTextState,
                enabled,
                stringResource(R.string.contained_text_ref),
                stringResource(R.string.contained_text_placeholder),
                supportingText = stringResource(R.string.ref_required_unless_view_id_filled),
                error = textInputValidity?.containedText?.toErrorText()
            )
        }
        item {
            RuleTextField(
                packageNameState,
                enabled,
                stringResource(R.string.element_level_package),
                stringResource(R.string.app_placeholder),
                error = textInputValidity?.packageName?.toErrorText()
            )
        }
        item {
            RuleTextField(
                classNameState,
                enabled,
                stringResource(R.string.view_class),
                stringResource(R.string.view_class_placeholder),
                error = textInputValidity?.className?.toErrorText()
            )
        }
        item {
            RuleDropdownMenu(
                selected = (state as? RuleEditorUiState.Loaded)?.action ?: Rule.Action.CLICK,
                enabled = enabled,
                options = Rule.Action.entries,
                toStringRes = Rule.Action::stringRes,
                toDrawableRes = Rule.Action::drawableRes,
                onSelect = onSelectAction,
                label = stringResource(R.string.action_ref),
                supportingText = stringResource(R.string.ref_required)
            )
        }
    }
}

@Composable
private fun RuleInputValidity.Name.toErrorText() = when (this) {
    RuleInputValidity.Name.OK -> null
}

@Composable
private fun RuleInputValidity.SourcePackageName.toErrorText(): String? {
    val resId = when (this) {
        RuleInputValidity.SourcePackageName.OK -> return null

        RuleInputValidity.SourcePackageName.EMPTY -> R.string.error_this_field_is_required

        RuleInputValidity.SourcePackageName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_START ->
            R.string.error_segments_can_only_start_with

        RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_PART ->
            R.string.error_segments_can_only_contain

        RuleInputValidity.SourcePackageName.NOT_ENOUGH_SEGMENTS ->
            R.string.error_at_least_two_segments_required
    }
    return stringResource(resId)
}

@Composable
private fun RuleInputValidity.ViewId.toErrorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ViewId.OK -> return null

        RuleInputValidity.ViewId.EMPTY_WITH_NO_ALTERNATIVE -> R.string.error_no_search_criteria

        RuleInputValidity.ViewId.NO_DIVIDER -> R.string.error_no_view_id_divider

        RuleInputValidity.ViewId.PACKAGE_EMPTY -> R.string.error_empty_package

        RuleInputValidity.ViewId.PACKAGE_EMPTY_SEGMENT -> R.string.error_empty_package_segment

        RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_START ->
            R.string.error_illegal_package_segment_start

        RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_PART ->
            R.string.error_illegal_package_segment_part

        RuleInputValidity.ViewId.VIEW_EMPTY -> R.string.error_empty_view_name

        RuleInputValidity.ViewId.VIEW_ILLEGAL_START -> R.string.error_illegal_view_name_start

        RuleInputValidity.ViewId.VIEW_ILLEGAL_PART -> R.string.error_illegal_view_name_part
    }
    return stringResource(resId)
}

@Composable
private fun RuleInputValidity.ContainedText.toErrorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ContainedText.OK -> return null
        RuleInputValidity.ContainedText.EMPTY_WITH_NO_ALTERNATIVE ->
            R.string.error_no_search_criteria
    }
    return stringResource(resId)
}

@Composable
private fun RuleInputValidity.PackageName.toErrorText(): String? {
    val resId = when (this) {
        RuleInputValidity.PackageName.OK -> return null

        RuleInputValidity.PackageName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_START ->
            R.string.error_segments_can_only_start_with

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_PART ->
            R.string.error_segments_can_only_contain

        RuleInputValidity.PackageName.NOT_ENOUGH_SEGMENTS ->
            R.string.error_at_least_two_segments_required
    }
    return stringResource(resId)
}

@Composable
private fun RuleInputValidity.ClassName.toErrorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ClassName.OK -> return null

        RuleInputValidity.ClassName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.ClassName.ILLEGAL_SEGMENT_START ->
            R.string.error_illegal_package_segment_or_class_start

        RuleInputValidity.ClassName.ILLEGAL_SEGMENT_PART ->
            R.string.error_illegal_package_segment_or_class_part
    }
    return stringResource(resId)
}

@Composable
private fun RuleTextField(
    state: TextFieldState,
    enabled: Boolean,
    label: String,
    placeholder: String,
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
        trailingIcon = (@Composable {
            Icon(painterResource(R.drawable.error_filled), contentDescription = null)
        }).takeIf { error != null },
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
    toStringRes: (T) -> Int,
    toDrawableRes: (T) -> Int,
    onSelect: (T) -> Unit,
    label: String,
    supportingText: String? = null
) {
    val textState = rememberTextFieldState(stringResource(toStringRes(selected)))
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(options.indexOf(selected)) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(toStringRes(selected)),
            onValueChange = {},
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(painterResource(toDrawableRes(selected)), contentDescription = null)
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

@Composable
fun RuleEditorPlaceholder() {
    PanePlaceholderText(
        stringResource(R.string.select_rule_or_create_one),
        modifier = Modifier.requiredMinPaneWidth()
    )
}
