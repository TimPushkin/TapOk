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
        val ruleId: Long?,
        val action: Rule.Action = Rule.Action.CLICK,
        val textInputValidity: RuleInputValidity = RuleInputValidity.AllOK,
        val isSaving: Boolean = false
    ) : RuleEditorUiState
}

@HiltViewModel(assistedFactory = RuleEditorViewModelFactory::class)
class RuleEditorViewModel @AssistedInject constructor(
    private val ruleRepo: RuleRepo,
    @Assisted ruleId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow<RuleEditorUiState>(
        RuleEditorUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    // It's not possible to use (isSaving true -> false + validity check) of uiState as a
    // notification since StateFlow may skip values because of conflation
    private val _savingNotificationChannel = Channel<Unit>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val savingNotificationChannel: ReceiveChannel<Unit> = _savingNotificationChannel

    val nameState = TextFieldState()
    val packageNameState = TextFieldState()
    val viewIdState = TextFieldState()
    val containedTextState = TextFieldState()
    val classNameState = TextFieldState()

    @OptIn(FlowPreview::class)
    private fun <T> launchTextValidationFlow(
        textState: TextFieldState,
        validate: (CharSequence) -> T,
        onChange: RuleInputValidity.() -> RuleInputValidity,
        onValidated: RuleInputValidity.(T) -> RuleInputValidity
    ) {
        snapshotFlow { textState.text }
            .onEach { _ ->
                _uiState.updateIfLoaded {
                    check(!it.isSaving) { "Cannot edit while saving" }
                    it.copy(textInputValidity = it.textInputValidity.onChange())
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

    init {
        viewModelScope.launch {
            loadRule(ruleId)
            launchTextValidationFlow(
                nameState,
                ::validateRuleName,
                { relationAwareCopy(name = RuleInputValidity.Name.OK) },
                { relationAwareCopy(name = it) }
            )
            launchTextValidationFlow(
                packageNameState,
                ::validateRulePackageName,
                { relationAwareCopy(packageName = RuleInputValidity.PackageName.OK) },
                { relationAwareCopy(packageName = it) }
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
                classNameState,
                ::validateRuleClassName,
                { relationAwareCopy(className = RuleInputValidity.ClassName.OK) },
                { relationAwareCopy(className = it) }
            )
        }
    }

    private suspend fun loadRule(id: Long?) {
        if (id != null) {
            val rule = withContext(Dispatchers.IO) { ruleRepo.loadRule(id) }
            Log.d(TAG, "Editing $rule")
            _uiState.value = RuleEditorUiState.Loaded(id, rule.action)
            nameState.setTextAndPlaceCursorAtEnd(rule.name)
            packageNameState.setTextAndPlaceCursorAtEnd(rule.packageName)
            rule.viewId?.let { viewIdState.setTextAndPlaceCursorAtEnd(it) }
            rule.containedText?.let { containedTextState.setTextAndPlaceCursorAtEnd(it) }
            rule.className?.let { classNameState.setTextAndPlaceCursorAtEnd(it) }
        } else {
            Log.d(TAG, "Creating new rule")
            _uiState.value = RuleEditorUiState.Loaded(null)
        }
    }

    fun selectAction(action: Rule.Action) {
        _uiState.updateIfLoaded {
            // Should not edit while saving
            if (!it.isSaving) {
                it.copy(action = action)
            } else {
                it
            }
        }
    }

    fun launchSaving() {
        var alreadySaving = false
        _uiState.updateIfLoaded {
            alreadySaving = it.isSaving
            if (alreadySaving) it else it.copy(isSaving = true)
        }
        if (!alreadySaving) {
            viewModelScope.launch(Dispatchers.IO) { save() }
        }
    }

    private suspend fun save() {
        val name = nameState.text
        val packageName = packageNameState.text
        val viewId = viewIdState.text
        val containedText = containedTextState.text
        val className = classNameState.text

        // Perform full validation which takes inter-field relations into account
        val validity = validateRule(name, packageName, viewId, containedText, className)
        if (!validity.isValid) {
            Log.d(TAG, "Invalid rule input: $validity")
            _uiState.updateIfLoaded { it.copy(textInputValidity = validity, isSaving = false) }
            return
        }

        val state = checkNotNull(_uiState.value as? RuleEditorUiState.Loaded)
        val rule = Rule(
            state.ruleId ?: 0,
            name.toString(),
            packageName.toString(),
            viewId.takeIf { it.isNotEmpty() }?.toString(),
            containedText.takeIf { it.isNotEmpty() }?.toString(),
            className.takeIf { it.isNotEmpty() }?.toString(),
            state.action
        )

        val savedId = ruleRepo.insertOrUpdateRule(rule)
        Log.d(
            TAG,
            if (state.ruleId == null) "Created ${rule.copy(id = savedId)}" else "Updated $rule"
        )
        _uiState.updateIfLoaded { it.copy(textInputValidity = validity, isSaving = false) }
        _savingNotificationChannel.send(Unit)
    }
}

@AssistedFactory
fun interface RuleEditorViewModelFactory {
    fun create(ruleId: Long?): RuleEditorViewModel
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
