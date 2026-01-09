package com.github.timpushkin.tapok.ui.activity.scans

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScansScreen(modifier: Modifier = Modifier, vm: ScansViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    if (uiState.selectedScan != null) {
        LaunchedEffect(uiState.selectedScan) {
            // TODO: navigate to scan viewer
        }
    } else if (uiState.scans.isNotEmpty()) {
        ScanList(
            uiState.scans.sortedByDescending { it.timestamp },
            onClick = vm::selectScan,
            onDelete = vm::deleteScan,
            modifier = modifier.padding(horizontal = 8.dp)
        )
    } else {
        NoScansBanner(modifier = modifier)
    }

    // TODO: if scanner service is disabled, show a button to enable it (open the corresponding Settings page)
}

@Composable
private fun NoScansBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.view_scans_here),
            modifier = Modifier.fillMaxWidth(0.66f),
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
