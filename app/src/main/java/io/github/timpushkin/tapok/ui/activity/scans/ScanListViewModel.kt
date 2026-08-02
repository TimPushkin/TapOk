package io.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import io.github.timpushkin.tapok.data.scans.screenshotFile
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
class ScanListViewModel @Inject constructor(
    application: Application,
    private val scanRepo: ScanRepo
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ScanListUiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    init {
        scanRepo.observeAllScans()
            .flowOn(Dispatchers.IO)
            .onEach { scans -> _uiState.update { it.copy(scans = scans) } }
            .launchIn(viewModelScope)
    }

    fun deleteScan(scan: Scan) {
        viewModelScope.launch(Dispatchers.IO) {
            scanRepo.deleteScan(scan)
            application.screenshotFile(scan).delete() // no-op if no screenshot exists
        }
    }
}
