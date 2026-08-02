package io.github.timpushkin.tapok.ui.activity.rules

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleInputValidity
import io.github.timpushkin.tapok.data.rules.RuleRepo
import io.github.timpushkin.tapok.data.rules.validateRule
import io.github.timpushkin.tapok.data.rules.validateRuleClassName
import io.github.timpushkin.tapok.data.rules.validateRuleContainedText
import io.github.timpushkin.tapok.data.rules.validateRuleName
import io.github.timpushkin.tapok.data.rules.validateRulePackageName
import io.github.timpushkin.tapok.data.rules.validateRuleSourcePackageName
import io.github.timpushkin.tapok.data.rules.validateRuleViewId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "RuleEditorViewModel"

sealed interface RuleEditorUiState {
    object Loading : RuleEditorUiState

    data class Loaded(
        val savedRule: Rule?,
        val action: Rule.Action,
        val textInputValidity: RuleInputValidity = RuleInputValidity.AllOK,
        val status: Status = Status.SAVED
    ) : RuleEditorUiState

    enum class Status { SAVED, EDITED, SAVING }
}

@HiltViewModel(assistedFactory = RuleEditorViewModelFactory::class)
class RuleEditorViewModel @AssistedInject constructor(
    private val ruleRepo: RuleRepo,
    @Assisted ruleId: Long?,
    @Assisted prefilling: IncompleteRule?
) : ViewModel() {
    private val _uiState = MutableStateFlow<RuleEditorUiState>(RuleEditorUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // It's not possible to use (isSaving true -> false + validity check) of uiState as a
    // notification since StateFlow may skip values because of conflation
    private val _savingNotificationChannel = Channel<Rule>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val savingNotificationChannel: ReceiveChannel<Rule> = _savingNotificationChannel

    val nameState = TextFieldState()
    val sourcePackageNameState = TextFieldState()
    val viewIdState = TextFieldState()
    val containedTextState = TextFieldState()
    val packageNameState = TextFieldState()
    val classNameState = TextFieldState()

    init {
        viewModelScope.launch {
            val rule = ruleId?.let { id ->
                withContext(Dispatchers.IO) { ruleRepo.loadRule(id) }
            }
            if (rule != null) {
                Log.d(TAG, "Editing $rule, prefilled with $prefilling")
            } else {
                Log.d(TAG, "Creating new rule, prefilled with $prefilling")
            }

            rule?.name?.let {
                nameState.setTextAndPlaceCursorAtEnd(it)
            }
            (prefilling?.sourcePackageName ?: rule?.sourcePackageName)?.let {
                sourcePackageNameState.setTextAndPlaceCursorAtEnd(it)
            }
            (prefilling?.viewId ?: rule?.viewId)?.let {
                viewIdState.setTextAndPlaceCursorAtEnd(it)
            }
            (prefilling?.containedText ?: rule?.containedText)?.let {
                containedTextState.setTextAndPlaceCursorAtEnd(it)
            }
            (prefilling?.packageName ?: rule?.packageName)?.let {
                packageNameState.setTextAndPlaceCursorAtEnd(it)
            }
            (prefilling?.className ?: rule?.className)?.let {
                classNameState.setTextAndPlaceCursorAtEnd(it)
            }
            val action = (prefilling?.action ?: rule?.action) ?: Rule.Action.CLICK

            launchTextValidationFlow(
                nameState,
                ::validateRuleName,
                { relationAwareCopy(name = RuleInputValidity.Name.OK) },
                { relationAwareCopy(name = it) }
            )
            launchTextValidationFlow(
                sourcePackageNameState,
                ::validateRuleSourcePackageName,
                { relationAwareCopy(sourcePackageName = RuleInputValidity.SourcePackageName.OK) },
                { relationAwareCopy(sourcePackageName = it) }
            )
            launchTextValidationFlow(
                viewIdState,
                ::validateRuleViewId,
                { relationAwareCopy(viewId = RuleInputValidity.ViewId.OK) },
                { relationAwareCopy(viewId = it) }
            )
            launchTextValidationFlow(
                containedTextState,
                ::validateRuleContainedText,
                { relationAwareCopy(containedText = RuleInputValidity.ContainedText.OK) },
                { relationAwareCopy(containedText = it) }
            )
            launchTextValidationFlow(
                packageNameState,
                ::validateRulePackageName,
                { relationAwareCopy(packageName = RuleInputValidity.PackageName.OK) },
                { relationAwareCopy(packageName = it) }
            )
            launchTextValidationFlow(
                classNameState,
                ::validateRuleClassName,
                { relationAwareCopy(className = RuleInputValidity.ClassName.OK) },
                { relationAwareCopy(className = it) }
            )

            _uiState.value = RuleEditorUiState.Loaded(savedRule = rule, action = action).let {
                it.copy(status = getEditingStatus(it))
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun <T> launchTextValidationFlow(
        textState: TextFieldState,
        validate: (CharSequence) -> T,
        onChange: RuleInputValidity.() -> RuleInputValidity,
        onValidated: RuleInputValidity.(T) -> RuleInputValidity
    ) {
        snapshotFlow { textState.text }
            // Without this Dispatchers.Default misses the initial value of textState set in init
            .flowOn(Dispatchers.Main.immediate)
            .onEach { _ ->
                _uiState.updateIfLoaded {
                    check(it.status != RuleEditorUiState.Status.SAVING) { "Cannot edit now" }
                    it.copy(
                        textInputValidity = it.textInputValidity.onChange(),
                        status = getEditingStatus(it)
                    )
                }
            }
            .debounce(500.milliseconds)
            .filter { it.isNotEmpty() }
            .map(validate)
            .onEach { result ->
                _uiState.updateIfLoaded {
                    it.copy(textInputValidity = it.textInputValidity.onValidated(result))
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun getEditingStatus(uiState: RuleEditorUiState.Loaded): RuleEditorUiState.Status {
        Log.i(TAG, "Calculating status from: ${nameState.text}, ${sourcePackageNameState.text}, ${viewIdState.text}, ${containedTextState.text}, ${packageNameState.text}, ${classNameState.text}")
        val hasUnsavedChanges = if (uiState.savedRule == null) {
            nameState.text.isNotEmpty() ||
                    sourcePackageNameState.text.isNotEmpty() ||
                    viewIdState.text.isNotEmpty() ||
                    containedTextState.text.isNotEmpty() ||
                    packageNameState.text.isNotEmpty() ||
                    classNameState.text.isNotEmpty()
            // action by itself is not valuable, so not checking it
        } else {
            val rule = uiState.savedRule
            nameState.text != rule.name ||
                    sourcePackageNameState.text != rule.sourcePackageName ||
                    viewIdState.text != (rule.viewId ?: "") ||
                    containedTextState.text != (rule.containedText ?: "") ||
                    packageNameState.text != (rule.packageName ?: "") ||
                    classNameState.text != (rule.className ?: "") ||
                    uiState.action != rule.action
        }
        return if (hasUnsavedChanges) {
            RuleEditorUiState.Status.EDITED
        } else {
            RuleEditorUiState.Status.SAVED
        }
    }

    fun selectAction(action: Rule.Action) {
        _uiState.updateIfLoaded {
            // Should not edit while saving
            if (it.status != RuleEditorUiState.Status.SAVING) {
                it.copy(action = action, status = getEditingStatus(it))
            } else {
                it
            }
        }
    }

    fun launchSaving() {
        var alreadySaving = false
        _uiState.updateIfLoaded {
            alreadySaving = it.status == RuleEditorUiState.Status.SAVING
            if (alreadySaving) it else it.copy(status = RuleEditorUiState.Status.SAVING)
        }
        if (!alreadySaving) {
            viewModelScope.launch(Dispatchers.IO) { save() }
        }
    }

    private suspend fun save() {
        val name = nameState.text
        val sourcePackageName = sourcePackageNameState.text
        val viewId = viewIdState.text
        val containedText = containedTextState.text
        val packageName = packageNameState.text
        val className = classNameState.text

        // Perform full validation which takes inter-field relations into account
        val validity =
            validateRule(name, sourcePackageName, viewId, containedText, packageName, className)
        if (!validity.isValid) {
            Log.d(TAG, "Invalid rule input: $validity")
            _uiState.updateIfLoaded {
                check(it.status == RuleEditorUiState.Status.SAVING) { "Must be saving" }
                it.copy(textInputValidity = validity, status = getEditingStatus(it))
            }
            return
        }

        val state = checkNotNull(_uiState.value as? RuleEditorUiState.Loaded)
        val rule = Rule(
            state.savedRule?.id ?: 0,
            name.toString(),
            sourcePackageName.toString(),
            viewId.takeIf { it.isNotEmpty() }?.toString(),
            containedText.takeIf { it.isNotEmpty() }?.toString(),
            packageName.takeIf { it.isNotEmpty() }?.toString(),
            className.takeIf { it.isNotEmpty() }?.toString(),
            state.action
        )
        val savedRule = ruleRepo.insertOrUpdateRule(rule).let { id ->
            if (id != -1L) rule.copy(id = id) else rule
        }
        Log.d(TAG, if (state.savedRule == null) "Created $savedRule" else "Updated $savedRule")

        _uiState.updateIfLoaded {
            check(it.status == RuleEditorUiState.Status.SAVING) { "Must be saving" }
            it.copy(
                savedRule = savedRule,
                textInputValidity = validity,
                status = RuleEditorUiState.Status.SAVED
            )
        }
        _savingNotificationChannel.send(savedRule)
    }
}

@AssistedFactory
fun interface RuleEditorViewModelFactory {
    fun create(ruleId: Long?, prefilling: IncompleteRule?): RuleEditorViewModel
}

private fun MutableStateFlow<RuleEditorUiState>.updateIfLoaded(
    function: (RuleEditorUiState.Loaded) -> RuleEditorUiState.Loaded
) {
    update {
        if (it is RuleEditorUiState.Loaded) {
            function(it)
        } else {
            it
        }
    }
}
