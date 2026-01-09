package com.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.data.scans.Node
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.data.scans.ScanRepo
import com.github.timpushkin.tapok.data.screenshotFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScansUiState(
    val scans: List<Scan>,
    val selectedScan: LoadedScan?
) {
    data class LoadedScan(
        val scan: Scan,
        val screenshot: Bitmap,
        val nodes: List<Node>,
        val selectedNode: Node?
    )
}

@HiltViewModel
class ScansViewModel @Inject constructor(
    application: Application, private val scanRepo: ScanRepo
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        ScansUiState(emptyList(), null)
    )
    val uiState = _uiState.asStateFlow()

    init {
        scanRepo.loadAllScans()
            .flowOn(Dispatchers.IO)
            .onEach { scans ->
                _uiState.update {
                    it.copy(
                        scans = scans,
                        selectedScan = it.selectedScan?.takeIf { selectedScan ->
                            selectedScan.scan in scans
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectScan(scan: Scan) {
        viewModelScope.launch {
            val screenshot = async(Dispatchers.IO) {
                BitmapFactory.decodeFile(application.screenshotFile(scan.screenshotFile).path)
            }
            val nodes = async(Dispatchers.IO) { scanRepo.loadNodesOfScan(scan) }
            val loadedScan = ScansUiState.LoadedScan(scan, screenshot.await(), nodes.await(), null)
            _uiState.update { if (scan in it.scans) it.copy(selectedScan = loadedScan) else it }
        }
    }

    fun deleteScan(scan: Scan) {
        viewModelScope.launch(Dispatchers.IO) { scanRepo.deleteScan(scan) }
    }

    fun selectNode(node: Node) {
        _uiState.update {
            val selectedScan = it.selectedScan
            if (selectedScan != null && selectedScan.scan.id == node.scanId) {
                it.copy(selectedScan = selectedScan.copy(selectedNode = node))
            } else {
                it
            }
        }
    }
}
