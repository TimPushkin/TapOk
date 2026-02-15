package com.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.data.scans.Node
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.data.scans.ScanRepo
import com.github.timpushkin.tapok.data.screenshotFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanInspectorUiState {
    object Loading : ScanInspectorUiState

    data class Loaded(
        val scan: Scan,
        val screenshot: Bitmap,
        val nodes: List<Node>,
        val selectedNode: Node?
    ) : ScanInspectorUiState
}

@HiltViewModel(assistedFactory = ScanInspectorViewModelFactory::class)
class ScanInspectorViewModel @AssistedInject constructor(
    application: Application,
    private val scanRepo: ScanRepo,
    @Assisted scanId: Long
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<ScanInspectorUiState>(ScanInspectorUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // If the scan has been deleted concurrently the VM won't be used
            val (scan, nodes) = withContext(Dispatchers.IO) { scanRepo.loadScanWithNodes(scanId) }
                ?: return@launch
            val screenshot = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(application.screenshotFile(scan.screenshotFile).path)
            }
            _uiState.value = ScanInspectorUiState.Loaded(scan, screenshot, nodes, null)
        }
    }

    fun selectNode(node: Node?) {
        _uiState.update {
            check(it is ScanInspectorUiState.Loaded) { "Node selected from not-yet-loaded scan" }
            check(node == null || node in it.nodes) { "$node not in ${it.nodes}" }
            it.copy(selectedNode = node)
        }
    }
}

@AssistedFactory
fun interface ScanInspectorViewModelFactory {
    fun create(scanId: Long): ScanInspectorViewModel
}
