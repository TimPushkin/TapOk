package com.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.timpushkin.tapok.data.scans.Node
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.data.scans.ScanRepo
import com.github.timpushkin.tapok.data.scans.screenshotFile
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
import java.io.File

sealed interface ScanInspectorUiState {
    val showUnlabeledNodes: Boolean

    data class Loading(override val showUnlabeledNodes: Boolean = false) : ScanInspectorUiState

    data class Loaded(
        val scan: Scan,
        val screenshotFile: File,
        val nodes: List<Node>,
        val selectedNode: Node?,
        override val showUnlabeledNodes: Boolean
    ) : ScanInspectorUiState {
        val shownNodes by lazy {
            if (showUnlabeledNodes) nodes else nodes.filter { it.viewId != null }
        }
    }
}

@HiltViewModel(assistedFactory = ScanInspectorViewModelFactory::class)
class ScanInspectorViewModel @AssistedInject constructor(
    application: Application,
    private val scanRepo: ScanRepo,
    @Assisted scanId: Long
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<ScanInspectorUiState>(ScanInspectorUiState.Loading())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (scan, nodes) = withContext(Dispatchers.IO) { scanRepo.loadScanWithNodes(scanId) }
                ?: return@launch // If the scan has been deleted concurrently the VM won't be used
            val screenshotFile = withContext(Dispatchers.IO) {
                application.screenshotFile(scan.screenshotFile)
            }
            _uiState.update {
                ScanInspectorUiState.Loaded(
                    scan,
                    screenshotFile,
                    nodes,
                    null,
                    it.showUnlabeledNodes
                )
            }
        }
    }

    fun switchShowUnlabeledNodes() {
        _uiState.update { prev ->
            val showUnlabeledNodes = !prev.showUnlabeledNodes
            when (prev) {
                is ScanInspectorUiState.Loading -> prev.copy(
                    showUnlabeledNodes = showUnlabeledNodes
                )

                is ScanInspectorUiState.Loaded -> prev.copy(
                    showUnlabeledNodes = showUnlabeledNodes,
                    selectedNode = prev.selectedNode?.takeIf {
                        showUnlabeledNodes || it.viewId != null
                    }
                )
            }
        }
    }

    fun selectNode(node: Node?) {
        _uiState.update {
            check(it is ScanInspectorUiState.Loaded) { "Node selected from not-yet-loaded scan" }
            it.copy(selectedNode = node)
        }
    }
}

@AssistedFactory
fun interface ScanInspectorViewModelFactory {
    fun create(scanId: Long): ScanInspectorViewModel
}
