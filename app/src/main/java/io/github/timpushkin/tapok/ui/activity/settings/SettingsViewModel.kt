package io.github.timpushkin.tapok.ui.activity.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.agent.ShortcutRequestBroker
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.preferences.Shortcut
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepo: PreferencesRepo,
    private val scanShortcutRequester: ShortcutRequestBroker
) : ViewModel() {
    val isAccessibilityUsageAccepted =
        preferencesRepo.isAccessibilityUsageAccepted().stateInViewModelScope(false)
    val shouldShowScannerOverlay =
        preferencesRepo.shouldShowScannerOverlay().stateInViewModelScope(false)
    val scanShortcut =
        preferencesRepo.scanShortcut().stateInViewModelScope(null)
    val shouldScanAllWindows =
        preferencesRepo.shouldScanAllWindows().stateInViewModelScope(false)

    private fun <T> Flow<T>.stateInViewModelScope(initialValue: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue
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

    fun setScanShortcut(value: Shortcut?) {
        viewModelScope.launch {
            preferencesRepo.setScanShortcut(value)
        }
    }

    fun setShouldScanAllWindows(value: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setShouldScanAllWindows(value)
        }
    }

    suspend fun recordScanShortcut() = scanShortcutRequester.request(Unit)
}
