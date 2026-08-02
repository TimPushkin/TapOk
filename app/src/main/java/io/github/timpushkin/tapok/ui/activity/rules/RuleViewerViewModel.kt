package io.github.timpushkin.tapok.ui.activity.rules

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

sealed interface RuleViewerUiState {
    data object Loading : RuleViewerUiState
    data class Loaded(val rule: Rule) : RuleViewerUiState
    data object NotFound : RuleViewerUiState
}

@HiltViewModel(assistedFactory = RuleViewerViewModelFactory::class)
class RuleViewerViewModel @AssistedInject constructor(
    ruleRepo: RuleRepo,
    @Assisted ruleId: Long
) : ViewModel() {
    val uiState: StateFlow<RuleViewerUiState> = ruleRepo.observeRuleOrNull(ruleId)
        .map { if (it != null) RuleViewerUiState.Loaded(it) else RuleViewerUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuleViewerUiState.Loading)
}

@AssistedFactory
fun interface RuleViewerViewModelFactory {
    fun create(ruleId: Long): RuleViewerViewModel
}
