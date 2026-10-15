// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
