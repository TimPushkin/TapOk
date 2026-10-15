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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "RuleViewerViewModel"

sealed interface RuleViewerUiState {
    data object Loading : RuleViewerUiState
    data class Loaded(val rule: Rule) : RuleViewerUiState
    data object NotFound : RuleViewerUiState
}

@HiltViewModel(assistedFactory = RuleViewerViewModelFactory::class)
class RuleViewerViewModel @AssistedInject constructor(
    private val ruleRepo: RuleRepo,
    @Assisted ruleId: Long
) : ViewModel() {
    val uiState: StateFlow<RuleViewerUiState> = ruleRepo.observeRuleOrNull(ruleId)
        .map { if (it != null) RuleViewerUiState.Loaded(it) else RuleViewerUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuleViewerUiState.Loading)

    fun switchActivation() {
        val rule = (uiState.value as? RuleViewerUiState.Loaded)?.rule ?: return
        Log.d(TAG, "Switching activation status of $rule")
        viewModelScope.launch { ruleRepo.insertOrUpdateRule(rule.copy(isActive = !rule.isActive)) }
    }
}

@AssistedFactory
fun interface RuleViewerViewModelFactory {
    fun create(ruleId: Long): RuleViewerViewModel
}
