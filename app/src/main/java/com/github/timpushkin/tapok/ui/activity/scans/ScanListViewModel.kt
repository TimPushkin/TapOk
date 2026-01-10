package com.github.timpushkin.tapok.ui.activity.scans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.data.scans.ScanRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanListUiState(val scans: List<Scan>)

@HiltViewModel
class ScanListViewModel @Inject constructor(private val scanRepo: ScanRepo) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanListUiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    init {
        scanRepo.loadAllScans()
            .flowOn(Dispatchers.IO)
            .onEach { scans -> _uiState.update { it.copy(scans = scans) } }
            .launchIn(viewModelScope)
    }

    fun deleteScan(scan: Scan) {
        viewModelScope.launch(Dispatchers.IO) { scanRepo.deleteScan(scan) }
    }
}