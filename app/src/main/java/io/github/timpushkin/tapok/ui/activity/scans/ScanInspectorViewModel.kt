package io.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import io.github.timpushkin.tapok.data.scans.screenshotFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScanInspectorUiState {
    data object Loading : ScanInspectorUiState

    data class Loaded(
        val scan: Scan,
        val screenshotFile: File?,
        val nodes: List<Node>,
        val selectedNode: Node?,
        val showUnreferenceableNodes: Boolean
    ) : ScanInspectorUiState {
        val shownNodes by lazy {
            if (showUnreferenceableNodes) nodes else nodes.filter { it.isReferenceableInRules }
        }
    }
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
            val (scan, nodes) = withContext(Dispatchers.IO) { scanRepo.loadScanWithNodes(scanId) }
                ?: return@launch // If the scan has been deleted concurrently the VM won't be used
            val screenshotFile = scan.screenshotFile?.let { filename ->
                withContext(Dispatchers.IO) { application.screenshotFile(filename) }
            }
            _uiState.update {
                ScanInspectorUiState.Loaded(
                    scan,
                    screenshotFile,
                    nodes,
                    null,
                    // If no referenceable nodes show at least something by default
                    nodes.all { !it.isReferenceableInRules }
                )
            }
        }
    }

    fun switchShowUnreferenceableNodes() {
        _uiState.update { prev ->
            check(prev is ScanInspectorUiState.Loaded) { "Nothing to show until loaded" }
            val showUnreferenceableNodes = !prev.showUnreferenceableNodes
            prev.copy(
                showUnreferenceableNodes = showUnreferenceableNodes,
                selectedNode = prev.selectedNode?.takeIf {
                    showUnreferenceableNodes || it.viewId != null
                }
            )
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
