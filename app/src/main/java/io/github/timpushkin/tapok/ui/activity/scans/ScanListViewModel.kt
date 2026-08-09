package io.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.Deletion
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ScanListViewModel"

data class ScanListUiState(val scans: List<Scan>)

@HiltViewModel
class ScanListViewModel @Inject constructor(
    application: Application,
    private val scanRepo: ScanRepo
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ScanListUiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    init {
        scanRepo.observeAllScans()
            .onEach { scans -> _uiState.update { it.copy(scans = scans) } }
            .launchIn(viewModelScope)
    }

    fun launchDeleteWithUndo(scan: Scan, promptForUndo: suspend () -> Boolean) {
        Log.d(TAG, "Deleting $scan - requested")
        viewModelScope.launch {
            scanRepo.deleteScan(scan.id, Deletion.SOFT)
            if (promptForUndo()) {
                val undone = scanRepo.undoDeleteScan(scan.id)
                Log.d(TAG, "Deleting $scan - ${if (undone) "undone" else "failed to undo"}")
            } else {
                scanRepo.deleteScan(scan.id, Deletion.HARD_AFTER_SOFT)
                Log.d(TAG, "Deleting $scan - done")
            }
        }
    }

    override fun onCleared() {
        // Cleanup after possible canceled deletions
        scanRepo.scheduleCleanupOfDeletedScans()
    }
}
