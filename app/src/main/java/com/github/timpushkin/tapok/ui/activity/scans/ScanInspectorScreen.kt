package com.github.timpushkin.tapok.ui.activity.scans

import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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

// TODO: don't show unlabeled nodes by default
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
    Box(modifier = modifier.dottedBackground()) {
        var showBars by rememberSaveable { mutableStateOf(true) }
        val insetsController = LocalActivity.current?.window?.let { window ->
            WindowCompat.getInsetsController(window, LocalView.current)
        }
        DisposableEffect(showBars, insetsController == null) {
            val systemBars = WindowInsetsCompat.Type.systemBars()
            insetsController?.run { if (showBars) show(systemBars) else hide(systemBars) }
            onDispose { if (!showBars) insetsController?.show(systemBars) }
        }

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val loadedUiState = uiState as? ScanInspectorUiState.Loaded

        loadedUiState?.let { (_, screenshot, nodes, selectedNode) ->
            ScanDisplay(
                screenshot,
                nodes,
                selectedNode,
                onNodeClick = { node ->
                    if (node != selectedNode) { // Clicked on deselected node
                        vm.selectNode(node)
                    } else if (selectedNode != null) { // Clicked on selected node
                        vm.selectNode(null)
                    } else { // Clicked on empty area when no node is selected
                        showBars = !showBars
                    }
                }
            )
        }

        val sheetDragHandleHeight by animateDpAsState(
            if (loadedUiState == null || loadedUiState.selectedNode == null) {
                BOTTOM_SHEET_HANDLE_HEIGHT_SMALL
            } else {
                BOTTOM_SHEET_HANDLE_HEIGHT_LARGE
            }
        )
        BottomSheetScaffold(
            topBar = {
                TopBar(
                    visible = showBars,
                    appPackage = loadedUiState?.run { scan.appPackage },
                    onBack = onClose
                )
            },
            sheetContent = {
                loadedUiState?.let { (_, _, nodes, selectedNode) ->
                    NodeBottomSheet(nodes, selectedNode, onClick = vm::selectNode)
                }
            },
            sheetDragHandle = {
                NodeBottomSheetHandle(
                    loadedUiState?.selectedNode,
                    onClose = { vm.selectNode(null) }
                )
            },
            content = {}, // Scan display not here to be rendered behind top bar
            containerColor = Color.Transparent,
            sheetPeekHeight = if (loadedUiState != null) {
                sheetDragHandleHeight + WindowInsets.safeContentBottomDp()
            } else {
                0.dp
            }
        )
    }
}

@Composable
private fun Modifier.dottedBackground(
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    dotColor: Color = MaterialTheme.colorScheme.onBackground,
    @FloatRange(from = 0.0, to = 1.0) dotAlpha: Float = 0.2f,
    dotRadius: Dp = 1.dp,
    spacing: Dp = 10.dp
): Modifier = drawBehind {
    drawRect(backgroundColor)

    val radiusPx = dotRadius.toPx()
    val spacingPx = spacing.toPx()
    val horizontalDots = (size.width / spacingPx).toInt() + 1
    val verticalDots = (size.height / spacingPx).toInt() + 1
    for (i in 0..horizontalDots) {
        for (j in 0..verticalDots) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(x = i * spacingPx, y = j * spacingPx),
                alpha = dotAlpha
            )
        }
    }
}

@Composable
private fun WindowInsets.Companion.safeContentBottomDp() = with(LocalDensity.current) {
    safeContent.getBottom(density = this).toDp()
}

@Composable
@OptIn(ExperimentalTelephotoApi::class)
private fun ScanDisplay(
    screenshotFile: File,
    nodes: List<Node>,
    selectedNode: Node?,
    onNodeClick: (Node?) -> Unit
) {
    val updatableOnNodeClick by rememberUpdatedState(onNodeClick)

    val zoomState = rememberZoomableState(ZoomSpec(minZoomFactor = 0.75f, maxZoomFactor = 10f))
    LaunchedEffect(null) { zoomState.zoomTo(zoomFactor = 0.75f, animationSpec = snap()) }

    // TODO: center on the clicked node
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
                updatableOnNodeClick(node)
            }
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
private fun TopBar(visible: Boolean, appPackage: String?, onBack: () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    appPackage?.let {
                        LocalContext.current.packageManager.getAppLabel(it) ?: it
                    } ?: ""
                )
            },
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

private fun PackageManager.getAppLabel(appPackage: String): String? {
    val appInfo = try {
        getApplicationInfo(appPackage, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
    return getApplicationLabel(appInfo).toString()
}

@OptIn(ExperimentalMaterial3Api::class)
private val BOTTOM_SHEET_HANDLE_HEIGHT_SMALL = BottomSheetDefaults.SheetPeekHeight
private val BOTTOM_SHEET_HANDLE_HEIGHT_LARGE = 86.dp

@Composable
private fun NodeBottomSheetHandle(selectedNode: Node?, onClose: () -> Unit) {
    val fullHeight by animateDpAsState(
        if (selectedNode == null) {
            BOTTOM_SHEET_HANDLE_HEIGHT_SMALL
        } else {
            BOTTOM_SHEET_HANDLE_HEIGHT_LARGE
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.height(fullHeight)
    ) {
        val handleHeight = 4.dp
        val handleVerticalPadding by animateDpAsState(
            if (selectedNode == null) (fullHeight - handleHeight) / 2 else 8.dp
        )
        Surface(
            modifier = Modifier.padding(vertical = handleVerticalPadding),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(Modifier.size(width = 32.dp, height = handleHeight))
        }

        AnimatedVisibility(
            selectedNode != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Need to remember the node to keep it displayed while disappearing
            var node by remember { mutableStateOf(checkNotNull(selectedNode)) }
            key(selectedNode) {
                if (selectedNode != null) {
                    node = selectedNode
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AnimatedContent(
                    node.viewId,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    modifier = Modifier.weight(1f)
                ) { viewId ->
                    Text(
                        viewId ?: stringResource(R.string.unlabeled_element),
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                FilledTonalIconButton(onClose) {
                    Icon(
                        painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeBottomSheet(nodes: List<Node>, selectedNode: Node?, onClick: (Node?) -> Unit) {
    AnimatedContent(selectedNode, transitionSpec = { fadeIn().togetherWith(fadeOut()) }) { node ->
        if (node == null) {
            NodeList(nodes, onNodeClick = onClick)
        } else {
            NodeDescription(node)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeList(nodes: List<Node>, onNodeClick: (Node) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = WindowInsets.safeContentBottomDp())) {
        items(nodes) { node ->
            val useful = node.viewId != null
            ListItem(
                onClick = { onNodeClick(node) },
                leadingContent = { NodeIcon(node) },
                content = {
                    Text(
                        node.viewId ?: stringResource(R.string.unlabeled_element),
                        color = with(ListItemDefaults.colors()) {
                            if (useful) contentColor else disabledContentColor
                        },
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                },
                supportingContent = {
                    Text(
                        with(node) { "$packageName.$className" },
                        color = with(ListItemDefaults.colors()) {
                            if (useful) supportingContentColor else disabledSupportingContentColor
                        },
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                },
                overlineContent = {
                    Text(
                        node.boundsAsString(),
                        color = with(ListItemDefaults.colors()) {
                            if (useful) overlineContentColor else disabledOverlineContentColor
                        },
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun NodeDescription(node: Node) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(bottom = WindowInsets.safeContentBottomDp())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CopyableText(node.packageName, stringResource(R.string.package_name))
        CopyableText(node.viewName, stringResource(R.string.view_name))

        Spacer(modifier = Modifier.height(4.dp))

        NonCopyableText(node.className, stringResource(R.string.clazz))
        NonCopyableText(node.boundsAsString(), stringResource(R.string.bounds))
        NonCopyableText(
            stringResource(if (node.isClickable) R.string.yes else R.string.no).lowercase(),
            stringResource(R.string.clickable)
        )
    }
}

private fun Node.boundsAsString() = with(bounds) { "x=$left–${right}px, y=$top–${bottom}px" }

@Composable
private fun NodeIcon(node: Node) {
    Icon(
        painterResource(
            if (node.isClickable) R.drawable.touch_app_outlined else R.drawable.code_outlined
        ),
        contentDescription = stringResource(
            if (node.isClickable) R.string.clickable_element else R.string.non_clickable_element
        )
    )
}

@Composable
private fun CopyableText(text: String?, label: String) {
    OutlinedTextField(
        value = text ?: stringResource(R.string.unlabeled_element),
        label = { Text(label) },
        onValueChange = {},
        enabled = text != null,
        readOnly = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NonCopyableText(text: String, label: String) {
    Text(
        buildAnnotatedString {
            withStyle(MaterialTheme.typography.labelMedium.toSpanStyle()) {
                append("$label: ")
            }
            withStyle(MaterialTheme.typography.bodySmall.toSpanStyle()) {
                append(text)
            }
        }
    )
}
