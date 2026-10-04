package io.github.timpushkin.tapok.ui.activity.rules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.Deletion
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RuleListViewModel"

data class RulesUiState(val rules: List<Rule>)

@HiltViewModel
class RuleListViewModel @Inject constructor(private val ruleRepo: RuleRepo) : ViewModel() {
    private val _uiState = MutableStateFlow(RulesUiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    init {
        ruleRepo.observeAllRules()
            .onEach { rules ->
                val sortedRules = rules.sortedByDescending { it.id }
                Log.d(TAG, "Loaded rules: $sortedRules")
                _uiState.update { it.copy(rules = sortedRules) }
            }
            .launchIn(viewModelScope)
    }

    fun softDelete(rule: Rule) {
        viewModelScope.launch {
            ruleRepo.deleteRule(rule.id, Deletion.SOFT)
            Log.d(TAG, "Deleting $rule - requested")
        }
    }

    fun hardDelete(rule: Rule) {
        viewModelScope.launch {
            ruleRepo.deleteRule(rule.id, Deletion.HARD_AFTER_SOFT)
            Log.d(TAG, "Deleting $rule - done")
        }
    }

    fun undoDelete(rule: Rule) {
        viewModelScope.launch {
            val undone = ruleRepo.undoDeleteRule(rule.id)
            Log.d(TAG, "Deleting $rule - ${if (undone) "undone" else "failed to undo"}")
        }
    }
}
