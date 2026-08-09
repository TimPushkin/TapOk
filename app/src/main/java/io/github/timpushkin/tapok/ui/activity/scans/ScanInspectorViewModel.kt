package io.github.timpushkin.tapok.ui.activity.scans

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanInspectorUiState {
    data object Loading : ScanInspectorUiState

    data class Loaded(
        val scan: Scan,
        val screenshot: ImageBitmap?,
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
        viewModelScope.launch(Dispatchers.IO) {
            // If the scan has been deleted concurrently the VM won't be used
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
            val (scan, nodes, screenshot) = scanRepo.loadScan(scanId, opts) ?: return@launch
            _uiState.value = ScanInspectorUiState.Loaded(
                scan,
                screenshot?.asImageBitmap(),
                nodes,
                null,
                // If no referenceable nodes show at least something by default
                nodes.all { !it.isReferenceableInRules }
            )
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
