package com.github.timpushkin.tapok.ui.activity.rules

import android.app.Application
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleNameValidity
import com.github.timpushkin.tapok.data.rules.RuleRepo
import com.github.timpushkin.tapok.data.rules.ViewIdValidity
import com.github.timpushkin.tapok.data.rules.validateRuleName
import com.github.timpushkin.tapok.data.rules.validateViewId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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

private const val TAG = "RuleEditorViewModel"

sealed interface RuleEditorUiState {
    object Loading : RuleEditorUiState

    data class Loaded(
        val originalRule: Rule? = null,
        val action: Rule.Action = Rule.Action.CLICK,
        val ruleNameValidity: RuleNameValidity? = null,
        val viewIdValidity: ViewIdValidity? = null
    ) : RuleEditorUiState {
        fun allowsSaving() = ruleNameValidity == RuleNameValidity.OK &&
                viewIdValidity == ViewIdValidity.OK
    }
}

@HiltViewModel(assistedFactory = RuleEditorViewModelFactory::class)
class RuleEditorViewModel @AssistedInject constructor(
    application: Application,
    private val ruleRepo: RuleRepo,
    @Assisted ruleId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow<RuleEditorUiState>(
        RuleEditorUiState.Loading
    )
    val uiState = _uiState.asStateFlow()

    val ruleNameState = TextFieldState()
    val viewIdState = TextFieldState()

    @OptIn(FlowPreview::class)
    private fun <T> launchTextValidationFlow(
        textState: TextFieldState,
        validate: (CharSequence) -> T,
        onChange: RuleEditorUiState.Loaded.() -> RuleEditorUiState.Loaded,
        onValidated: RuleEditorUiState.Loaded.(T) -> RuleEditorUiState.Loaded
    ) {
        snapshotFlow { textState.text }
            .onEach { _ -> _uiState.updateWhenLoaded { it.onChange() } }
            .flowOn(Dispatchers.Main.immediate)
            .debounce(500)
            .filter { it.isNotEmpty() }
            .map(validate)
            .flowOn(Dispatchers.Default)
            .onEach { result -> _uiState.updateWhenLoaded { it.onValidated(result) } }
            .launchIn(viewModelScope)
    }

    init {
        viewModelScope.launch {
            if (ruleId != null) {
                val rule = withContext(Dispatchers.IO) { ruleRepo.loadRule(ruleId) }
                Log.d(TAG, "Editing $rule")
                _uiState.value = RuleEditorUiState.Loaded(rule, rule.action)
                ruleNameState.setTextAndPlaceCursorAtEnd(rule.name)
                viewIdState.setTextAndPlaceCursorAtEnd(rule.viewId)
            } else {
                Log.d(TAG, "Creating new rule")
                _uiState.value = RuleEditorUiState.Loaded()
                ruleNameState.setTextAndPlaceCursorAtEnd(
                    application.resources.getString(R.string.rule_name_placeholder)
                )
            }
            launchTextValidationFlow(
                ruleNameState,
                ::validateRuleName,
                { copy(ruleNameValidity = null) },
                { copy(ruleNameValidity = it) }
            )
            launchTextValidationFlow(
                viewIdState,
                ::validateViewId,
                { copy(viewIdValidity = null) },
                { copy(viewIdValidity = it) }
            )
        }
    }

    fun selectAction(action: Rule.Action) {
        _uiState.updateWhenLoaded { it.copy(action = action) }
    }

    suspend fun save(): Rule {
        val state = withContext(Dispatchers.Main.immediate) { _uiState.value }
        check(state is RuleEditorUiState.Loaded && state.allowsSaving())

        val (packageName, viewName) = viewIdState.text.split(":id/", limit = 2)
        val rule = Rule(
            state.originalRule?.id ?: 0,
            ruleNameState.text.toString(),
            packageName,
            viewName,
            state.action
        )
        Log.d(TAG, "Saving $rule")
        val savedId = withContext(Dispatchers.IO) { ruleRepo.insertOrUpdateRule(rule) }

        return if (savedId != -1L) {
            rule.copy(id = savedId).also { saved ->
                Log.d(TAG, "Saved $saved (new)")
                _uiState.updateWhenLoaded { it.copy(originalRule = saved) }
            }
        } else {
            rule.also { saved -> Log.d(TAG, "Saved $saved (update)") }
        }
    }
}

@AssistedFactory
fun interface RuleEditorViewModelFactory {
    fun create(ruleId: Long?): RuleEditorViewModel
}

private fun MutableStateFlow<RuleEditorUiState>.updateWhenLoaded(
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
