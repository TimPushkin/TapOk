package io.github.timpushkin.tapok.ui.activity.rules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
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
        ruleRepo.loadAllRules()
            .flowOn(Dispatchers.IO)
            .onEach { rules ->
                Log.d(TAG, "Loaded rules: $rules")
                _uiState.update { it.copy(rules = rules) }
            }
            .launchIn(viewModelScope)
    }

    fun switchRuleActivation(rule: Rule) {
        Log.d(TAG, "Switching activation status of $rule")
        viewModelScope.launch(Dispatchers.IO) {
            ruleRepo.insertOrUpdateRule(rule.copy(isActive = !rule.isActive))
        }
    }

    fun deleteRule(rule: Rule) {
        Log.d(TAG, "Deleting $rule")
        viewModelScope.launch(Dispatchers.IO) { ruleRepo.deleteRule(rule) }
    }
}
