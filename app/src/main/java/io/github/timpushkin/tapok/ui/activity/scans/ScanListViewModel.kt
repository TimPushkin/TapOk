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
            .onEach { scans ->
                val sortedScans = scans.sortedWith(
                    compareByDescending<Scan> { it.timestamp }.thenByDescending { it.id }
                )
                Log.d(TAG, "Loaded scans: $sortedScans")
                _uiState.update { it.copy(scans = sortedScans) }
            }
            .launchIn(viewModelScope)
    }

    fun softDelete(scan: Scan) {
        viewModelScope.launch {
            scanRepo.deleteScan(scan.id, Deletion.SOFT)
            Log.d(TAG, "Deleting $scan - requested")
        }
    }

    fun hardDelete(scan: Scan) {
        viewModelScope.launch {
            scanRepo.deleteScan(scan.id, Deletion.HARD_AFTER_SOFT)
            Log.d(TAG, "Deleting $scan - done")
        }
    }

    fun undoDelete(scan: Scan) {
        viewModelScope.launch {
            val undone = scanRepo.undoDeleteScan(scan.id)
            Log.d(TAG, "Deleting $scan - ${if (undone) "undone" else "failed to undo"}")
        }
    }
}
