package com.github.timpushkin.tapok.ui.activity.scans

import android.content.ClipData
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.times
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.scans.Node
import kotlinx.coroutines.launch
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.subsamplingimage.SubSamplingImage
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.rememberSubSamplingImageState
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomFocalPoint
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContent
import me.saket.telephoto.zoomable.ZoomableState
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

        loadedUiState?.run {
            ScanDisplay(
                screenshotFile,
                shownNodes,
                selectedNode,
                onNodeClick = { node ->
                    if (node != selectedNode) { // Clicked on deselected node
                        vm.selectNode(node)
                    } else if (selectedNode != null) { // Clicked on selected node
                        vm.selectNode(null)
                    } else { // Clicked on empty area when no node is selected
                        showBars = !showBars
                    }
                },
                onZoomIn = { showBars = false }
            )
        }

        val sheetPeekHeight by animateDpAsState(
            when {
                loadedUiState == null -> 0.dp
                loadedUiState.selectedNode == null -> BottomSheetDefaults.SheetPeekHeight
                else -> 2 * BottomSheetDefaults.SheetPeekHeight
            }
        )
        BottomSheetScaffold(
            topBar = {
                TopBar(
                    visible = showBars,
                    appPackage = loadedUiState?.run { scan.appPackage },
                    showUnlabeledNodes = uiState.showUnlabeledNodes,
                    onBack = onClose,
                    onSwitchShowUnlabeledNodes = vm::switchShowUnlabeledNodes
                )
            },
            sheetContent = {
                loadedUiState?.let { state ->
                    NodeBottomSheet(state.shownNodes, state.selectedNode, onClick = vm::selectNode)
                }
            },
            sheetDragHandle = { BottomSheetHandle() },
            content = {}, // Scan display not here to be rendered behind top bar
            containerColor = Color.Transparent,
            sheetPeekHeight = sheetPeekHeight
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
    onNodeClick: (Node?) -> Unit,
    onZoomIn: () -> Unit
) {
    // Args referenced in the onClick lambda should be wrapped to make updates visible there
    val updatableNodes by rememberUpdatedState(nodes)
    val updatableOnNodeClick by rememberUpdatedState(onNodeClick)

    val zoomState = rememberZoomableState(ZoomSpec(minZoomFactor = 0.75f, maxZoomFactor = 10f))
    var viewportSize by remember { mutableStateOf(Size.Zero) }
    BoxWithConstraints(
        Modifier.zoomable(
            zoomState,
            onClick = { offset ->
                val offsetInContent = with(zoomState.coordinateSystem) {
                    SpatialOffset(offset, CoordinateSpace.Viewport).offsetIn(
                        CoordinateSpace.ZoomableContent
                    )
                }
                // Relies on scanned nodes being sorted from outer to inner
                val node = updatableNodes.lastOrNull { it.boundsRect.contains(offsetInContent) }
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
            return@BoxWithConstraints
        }
        viewportSize = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }

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
                    // White color + difference blending mode = inversion of underlying pixels
                    drawRect(
                        Color.White,
                        node.boundsRect.topLeft,
                        node.boundsRect.size,
                        style = if (node == selectedNode) selectedNodeStroke else defaultNodeStroke,
                        blendMode = BlendMode.Difference
                    )
                }
            }
        }
    }

    // Workaround for not being able to set the initial zoom factor
    var isInitialZoomSet by retain { mutableStateOf(false) }
    LaunchedEffect(null) {
        if (!isInitialZoomSet) {
            // Using zoomFactor = zoomState.zoomSpec.minimum.factor does not work as expected:
            // https://github.com/saket/telephoto/issues/170
            zoomState.zoomTo(zoomFactor = Float.MIN_VALUE, animationSpec = snap())
            isInitialZoomSet = true
        }
    }

    // Center on selected node
    var prevSelectedNode by retain { mutableStateOf<Node?>(null) }
    LaunchedEffect(selectedNode) {
        if (selectedNode != prevSelectedNode) {
            prevSelectedNode = selectedNode?.apply { zoomState.centerOn(boundsRect, viewportSize) }
        }
    }

    // Call onZoom when user zooms-in
    val zoomed by retain { derivedStateOf { (zoomState.zoomFraction ?: 0f) != 0f } }
    LaunchedEffect(zoomed, isInitialZoomSet) {
        if (zoomed && isInitialZoomSet) onZoomIn()
    }
}

@OptIn(ExperimentalTelephotoApi::class)
private suspend fun ZoomableState.centerOn(rect: Rect, viewportSize: Size) {
    // ContentScale.Fit is uniform so scaleX == scaleY
    val factor = ContentScale.Fit.computeScaleFactor(rect.size, viewportSize).scaleX
        .takeUnless { it.isNaN() }
        // Larger values make it hard to understand the surroundings, user can zoom more if needed
        ?.coerceAtMost(3f)
        // zoomTo coerces internally but only for some calculations
        ?.coerceIn(zoomSpec.minimum.factor, zoomSpec.maximum.factor)
        ?: return
    val center = SpatialOffset(rect.center, CoordinateSpace.ZoomableContent)
    zoomTo(factor, ZoomFocalPoint.moveToViewportCenter(center))
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBar(
    visible: Boolean,
    appPackage: String?,
    showUnlabeledNodes: Boolean,
    onBack: () -> Unit,
    onSwitchShowUnlabeledNodes: () -> Unit
) {
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
            actions = {
                ShowUnlabeledNodesSwitch(showUnlabeledNodes, onSwitchShowUnlabeledNodes)
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

@Composable
private fun ShowUnlabeledNodesSwitch(
    showUnlabeledNodes: Boolean,
    onSwitchShowUnlabeledNodes: () -> Unit
) {
    val iconRes = if (showUnlabeledNodes) {
        R.drawable.visibility_filled
    } else {
        R.drawable.visibility_off_outlined
    }
    val descriptionRes = R.string.show_hide_elements_that_cannot_be_referenced
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { Text(stringResource(descriptionRes)) },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onSwitchShowUnlabeledNodes) {
            Icon(painterResource(iconRes), stringResource(descriptionRes))
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Surface(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .size(width = 32.dp, height = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    ) {}
}

@Composable
private fun NodeBottomSheet(nodes: List<Node>, selectedNode: Node?, onClick: (Node?) -> Unit) {
    AnimatedContent(selectedNode, transitionSpec = { fadeIn().togetherWith(fadeOut()) }) { node ->
        if (node == null) {
            NodeList(nodes, onNodeClick = onClick)
        } else {
            NodeDescription(node, onClose = { onClick(null) })
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
                        node.className,
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
private fun NodeDescription(node: Node, onClose: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(bottom = WindowInsets.safeContentBottomDp())
            .padding(horizontal = 16.dp)
    ) {
        NodeDescriptionHeader(node.className.substringAfterLast('.'), onClose)
        NodeDescriptionElement(
            node.viewId ?: "—",
            stringResource(R.string.view_id),
            copyable = node.viewId != null
        )
        NodeDescriptionElement(node.boundsAsString(), stringResource(R.string.bounds))
        NodeDescriptionElement(node.className, stringResource(R.string.clazz))
        NodeDescriptionElement(
            stringResource(if (node.isClickable) R.string.yes else R.string.no).lowercase(),
            stringResource(R.string.clickable)
        )
    }
}

private fun Node.boundsAsString() = with(bounds) { "x=$left–${right}px, y=$top–${bottom}px" }

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeDescriptionHeader(title: String, onClose: () -> Unit) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
        }

        FilledTonalIconButton(
            onClose,
            shape = IconButtonDefaults.extraSmallRoundShape,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.extraSmallContainerSize())
        ) {
            Icon(
                painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.close),
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize)
            )
        }
    }
}

private const val INLINE_COPY_BUTTON_ID = "copyButton"

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeDescriptionElement(text: String, label: String, copyable: Boolean = false) {
    Text(
        buildAnnotatedString {
            withStyle(MaterialTheme.typography.labelMedium.toSpanStyle()) {
                append("$label: ")
            }
            if (copyable) {
                withStyle(MaterialTheme.typography.bodySmallEmphasized.toSpanStyle()) {
                    append(text)
                    append('\u00A0') // Non-line-breaking space
                    appendInlineContent(INLINE_COPY_BUTTON_ID)
                }
            } else {
                withStyle(MaterialTheme.typography.bodySmall.toSpanStyle()) {
                    append(text)
                }
            }
        },
        inlineContent = if (copyable) {
            mapOf(
                INLINE_COPY_BUTTON_ID to InlineTextContent(
                    Placeholder(1.em, 1.em, PlaceholderVerticalAlign.Center),
                ) {
                    val clipboard = LocalClipboard.current
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.content_copy_outlined),
                            contentDescription = stringResource(R.string.copy),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        } else {
            emptyMap()
        }
    )
}
