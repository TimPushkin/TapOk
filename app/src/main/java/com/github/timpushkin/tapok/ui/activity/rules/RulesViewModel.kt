package com.github.timpushkin.tapok.ui.activity.rules

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.data.rules.PackageNameValidity
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleNameValidity
import com.github.timpushkin.tapok.data.rules.RuleRepo
import com.github.timpushkin.tapok.data.rules.ViewNameValidity
import com.github.timpushkin.tapok.data.rules.validatePackageName
import com.github.timpushkin.tapok.data.rules.validateRuleName
import com.github.timpushkin.tapok.data.rules.validateViewName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "RulesViewModel"

data class RulesUiState(
    val rules: List<Rule>,
    val ruleEditorState: RuleEditorState?
) {
    data class RuleEditorState(
        val originalRule: Rule?,
        val action: Rule.Action,
        val ruleNameValidity: RuleNameValidity?,
        val packageNameValidity: PackageNameValidity?,
        val viewNameValidity: ViewNameValidity?
    ) {
        fun allowsCompletion() = ruleNameValidity == RuleNameValidity.OK &&
            packageNameValidity == PackageNameValidity.OK &&
            viewNameValidity == ViewNameValidity.OK
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RulesViewModel @Inject constructor(private val ruleRepo: RuleRepo) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RulesUiState(emptyList(), null)
    )
    val uiState = _uiState.asStateFlow()

    val ruleNameState = TextFieldState()
    val packageNameState = TextFieldState()
    val viewNameState = TextFieldState()

    private fun <T> launchTextValidationFlow(
        textState: TextFieldState,
        validate: (CharSequence) -> T,
        onChange: RulesUiState.RuleEditorState.() -> RulesUiState.RuleEditorState,
        onValidated: RulesUiState.RuleEditorState.(T) -> RulesUiState.RuleEditorState
    ) {
        snapshotFlow { textState.text }
            .onEach { _ ->
                _uiState.update { it.copy(ruleEditorState = it.ruleEditorState?.onChange()) }
            }
            .flowOn(Dispatchers.Main.immediate)
            .debounce(500)
            .filter { it.isNotEmpty() }
            .map(validate)
            .flowOn(Dispatchers.Default)
            .onEach { result ->
                _uiState.update {
                    it.copy(ruleEditorState = it.ruleEditorState?.onValidated(result))
                }
            }
            .launchIn(viewModelScope)
    }

    init {
        launchTextValidationFlow(
            ruleNameState,
            ::validateRuleName,
            { copy(ruleNameValidity = null) },
            { copy(ruleNameValidity = it) }
        )
        launchTextValidationFlow(
            packageNameState,
            ::validatePackageName,
            { copy(packageNameValidity = null) },
            { copy(packageNameValidity = it) }
        )
        launchTextValidationFlow(
            viewNameState,
            ::validateViewName,
            { copy(viewNameValidity = null) },
            { copy(viewNameValidity = it) }
        )
    }

    private suspend fun reloadRules() {
        val rules = withContext(Dispatchers.IO) { ruleRepo.loadAllRules() }
        Log.d(TAG, "Loaded rules: $rules")
        _uiState.update { it.copy(rules = rules) }
    }

    init {
        viewModelScope.launch { reloadRules() }
    }

    fun deleteRule(rule: Rule) {
        Log.d(TAG, "Deleting $rule")
        viewModelScope.launch {
            withContext(Dispatchers.IO) { ruleRepo.deleteRule(rule) }
            reloadRules()
        }
    }

    fun startRuleEditing(rule: Rule? = null) {
        Log.d(TAG, "Editing $rule")
        _uiState.update {
            it.copy(
                ruleEditorState = RulesUiState.RuleEditorState(
                    rule, rule?.action ?: Rule.Action.CLICK, null, null, null
                )
            )
        }
        ruleNameState.setTextAndPlaceCursorAtEnd(rule?.name ?: "")
        packageNameState.setTextAndPlaceCursorAtEnd(rule?.packageName ?: "")
        viewNameState.setTextAndPlaceCursorAtEnd(rule?.viewName ?: "")
    }

    fun cancelRuleEditing() {
        Log.d(TAG, "Editing cancelled")
        _uiState.update { it.copy(ruleEditorState = null) }
    }

    fun editRuleAction(action: Rule.Action) {
        _uiState.update { it.copy(ruleEditorState = it.ruleEditorState?.copy(action = action)) }
    }

    fun completeRuleEditing() {
        val editorState = _uiState.getAndUpdate { it.copy(ruleEditorState = null) }.ruleEditorState
        requireNotNull(editorState)
        require(editorState.allowsCompletion())
        viewModelScope.launch {
            val rule = Rule(
                editorState.originalRule?.id ?: 0,
                ruleNameState.text.toString(),
                packageNameState.text.toString(),
                viewNameState.text.toString(),
                editorState.action
            )
            Log.d(TAG, "Saving $rule")
            withContext(Dispatchers.IO) { ruleRepo.insertOrUpdateRule(rule) }
            reloadRules()
        }
    }
}
