package io.github.timpushkin.tapok.ui.activity.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepo: PreferencesRepo
) : ViewModel() {
    val isAccessibilityUsageAccepted =
        preferencesRepo.isAccessibilityUsageAccepted().stateInViewModelScope()
    val shouldShowScannerOverlay =
        preferencesRepo.shouldShowScannerOverlay().stateInViewModelScope()
    val shouldScanAllWindows =
        preferencesRepo.shouldScanAllWindows().stateInViewModelScope()

    private fun Flow<Boolean>.stateInViewModelScope(): StateFlow<Boolean> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun markAccessibilityUsageAccepted() {
        viewModelScope.launch {
            preferencesRepo.markAccessibilityUsageAccepted()
        }
    }

    fun setShouldShowScannerOverlay(value: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setShouldShowScannerOverlay(value)
        }
    }

    fun setShouldScanAllWindows(value: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setShouldScanAllWindows(value)
        }
    }
}
