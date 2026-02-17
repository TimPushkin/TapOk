package com.github.timpushkin.tapok.ui.activity.scans

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.scans.Node
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScanInspectorScreen(
    scanId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ScanInspectorViewModel = hiltViewModel(
        key = scanId.toString()
    ) { f: ScanInspectorViewModelFactory -> f.create(scanId) }
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? ScanInspectorUiState.Loaded)?.run {
                            LocalContext.current.packageManager.getAppLabel(scan.appPackage)
                                ?: scan.appPackage
                        } ?: ""
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        ScanInspector(
            state,
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        )
    }
}

@Composable
private fun ScanInspector(state: ScanInspectorUiState, modifier: Modifier = Modifier) {
    val zoomState = rememberZoomableState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zoomable(zoomState)
    ) {
        if (state !is ScanInspectorUiState.Loaded) {
            return@BoxWithConstraints
        }

        val (_, screenshot, nodes, _) = state

        val alignment = Alignment.Center
        val contentScale = ContentScale.Fit
        Image(
            bitmap = screenshot.asImageBitmap(),
            contentDescription = stringResource(R.string.scan_screenshot_description),
            modifier = Modifier.fillMaxSize(),
            alignment = alignment,
            contentScale = contentScale
        )

        val srcSize = with(screenshot) { IntSize(width, height) }
        val dstSize = with(constraints) { IntSize(maxWidth, maxHeight) }
        val scale = contentScale.computeScaleFactor(srcSize.toSize(), dstSize.toSize())
        val offset = alignment.align(
            (srcSize.toSize() * scale).toIntSize(),
            dstSize,
            LocalLayoutDirection.current
        )
        for (node in nodes) {
            if (node.idResourceName != null) {
                NodeView(node, offset, scale)
            }
        }
    }
}

@Composable
private fun NodeView(node: Node, offset: IntOffset, scale: ScaleFactor) {
    val nodeOffset = with(LocalDensity.current) {
        with(node.bounds) {
            DpOffset(
                (offset.x + (left * scale.scaleX)).toDp(),
                (offset.y + (top * scale.scaleX)).toDp()
            )
        }
    }
    val nodeSize = with(LocalDensity.current) {
        with(node.bounds) {
            DpSize(
                ((right - left) * scale.scaleY).toDp(),
                ((bottom - top) * scale.scaleY).toDp()
            )
        }
    }
    Box(
        modifier = Modifier
            .absoluteOffset(nodeOffset.x, nodeOffset.y)
            .requiredSize(nodeSize.width, nodeSize.height)
            .border(width = 3.dp, color = MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center
    ) {
        Text(node.idResourceName ?: "null")
    }
}

private fun PackageManager.getAppLabel(appPackage: String): String? {
    val appInfo = try {
        getApplicationInfo(appPackage, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
    return getApplicationLabel(appInfo).toString()
}
