package com.github.timpushkin.tapok.ui.activity.scans

import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.scans.Node
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.subsamplingimage.SubSamplingImage
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.rememberSubSamplingImageState
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContent
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace
import me.saket.telephoto.zoomable.spatial.SpatialOffset
import me.saket.telephoto.zoomable.zoomable
import okio.Path.Companion.toOkioPath
import java.io.File

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
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        var showBars by rememberSaveable { mutableStateOf(true) }

        val insetsController = LocalActivity.current?.window?.let { window ->
            WindowCompat.getInsetsController(window, LocalView.current)
        }
        DisposableEffect(showBars, insetsController == null) {
            val systemBars = WindowInsetsCompat.Type.systemBars()
            insetsController?.run { if (showBars) show(systemBars) else hide(systemBars) }
            onDispose { if (!showBars) insetsController?.show(systemBars) }
        }

        val state by vm.uiState.collectAsStateWithLifecycle()
        (state as? ScanInspectorUiState.Loaded)?.let { (_, screenshot, nodes, selectedNode) ->
            ScanDisplay(
                screenshot,
                nodes,
                selectedNode,
                onClick = vm::selectNode,
                onLongClick = { showBars = !showBars }
            )
        }

        TopBar(
            visible = showBars,
            title = (state as? ScanInspectorUiState.Loaded)?.run {
                LocalContext.current.packageManager.getAppLabel(scan.appPackage) ?: scan.appPackage
            } ?: "",
            onBack = onClose
        )
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

@Composable
@OptIn(ExperimentalTelephotoApi::class)
private fun ScanDisplay(
    screenshotFile: File,
    nodes: List<Node>,
    selectedNode: Node?,
    onClick: (Node?) -> Unit,
    onLongClick: () -> Unit,
) {
    val updatableOnClick by rememberUpdatedState(onClick)
    val updatableOnLongClick by rememberUpdatedState(onLongClick)

    val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 10f))
    Box(
        Modifier.zoomable(
            zoomState,
            onClick = { offset ->
                val offsetInContent = with(zoomState.coordinateSystem) {
                    SpatialOffset(offset, CoordinateSpace.Viewport).offsetIn(
                        CoordinateSpace.ZoomableContent
                    )
                }
                // TODO: this relies on scanned nodes being sorted from outer to inner, using a
                //  search tree would be more robust and performant
                val node = nodes.lastOrNull { it.boundsRect.contains(offsetInContent) }
                updatableOnClick(node)
            },
            onLongClick = { updatableOnLongClick() }
        )
    ) {
        val imageState = rememberSubSamplingImageState(
            SubSamplingImageSource.file(screenshotFile.toOkioPath()),
            zoomState
        )
        SubSamplingImage(imageState, contentDescription = null, modifier = Modifier.fillMaxSize())

        if (!imageState.isImageDisplayed) {
            return@Box
        }

        val selectedNodeColor = MaterialTheme.colorScheme.tertiary
        val defaultNodeColor = MaterialTheme.colorScheme.outline
        val selectedNodeStroke = Stroke(with(LocalDensity.current) { 4.dp.toPx() })
        val defaultNodeStroke = Stroke(Stroke.HairlineWidth)
        Canvas(Modifier.matchParentSize()) {
            // Transformations need to be applied manually because SubSamplingImage disables
            // auto-application
            withTransform({
                with(zoomState.contentTransformation) {
                    val pivot = with(transformOrigin) {
                        Offset(size.width * pivotFractionX, size.height * pivotFractionY)
                    }
                    translate(offset.x, offset.y)
                    rotate(rotationZ, pivot)
                    scale(scale.scaleX, scale.scaleY, pivot)
                }
            }) {
                for (node in nodes) {
                    val selected = node == selectedNode
                    drawRect(
                        if (selected) selectedNodeColor else defaultNodeColor,
                        node.boundsRect.topLeft,
                        node.boundsRect.size,
                        style = if (selected) selectedNodeStroke else defaultNodeStroke
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(visible: Boolean, title: String, onBack: () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
    ) {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                )
            )
        )
    }
}
