package io.github.timpushkin.tapok.ui.activity.scans

import android.content.ClipData
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.ui.theme.DefaultSpacing
import io.github.timpushkin.tapok.ui.utils.appendParagraphSpacer
import kotlinx.coroutines.CoroutineScope
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
    vm: ScanInspectorViewModel = hiltViewModel(
        key = scanId.toString()
    ) { f: ScanInspectorViewModelFactory -> f.create(scanId) }
) {
    Box(modifier = Modifier.dottedBackground()) {
        var showBars by rememberSaveable { mutableStateOf(true) }
        val insetsController = LocalActivity.current?.window?.let { window ->
            WindowCompat.getInsetsController(window, LocalView.current)
        }
        DisposableEffect(showBars, insetsController == null) {
            val systemBars = WindowInsetsCompat.Type.systemBars()
            insetsController?.run { if (showBars) show(systemBars) else hide(systemBars) }
            onDispose { if (!showBars) insetsController?.show(systemBars) }
        }

        val bottomSheetState = rememberBottomSheetStateWrapper()

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
                        with(bottomSheetState) { if (node != null) expand() else shrink() }
                    } else if (selectedNode != null) { // Clicked on selected node
                        vm.selectNode(null)
                        bottomSheetState.shrink()
                    } else { // Clicked on empty area when no node is selected
                        showBars = !showBars
                        bottomSheetState.shrink()
                    }
                },
                onZoomIn = { showBars = false }
            )
        }

        val sheetPeekHeight by animateDpAsState(
            if (loadedUiState == null) {
                0.dp
            } else {
                WindowInsets.safeContentBottomDp() + LocalMinimumInteractiveComponentSize.current
            }
        )
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState.bottomSheetState),
            topBar = {
                TopBar(
                    visible = showBars,
                    appPackage = loadedUiState?.run { scan.appPackage },
                    showUnlabeledNodes = loadedUiState?.showUnlabeledNodes ?: false,
                    onBack = onClose,
                    onSwitchShowUnlabeledNodes = vm::switchShowUnlabeledNodes.takeIf {
                        loadedUiState != null
                    }
                )
            },
            sheetContent = {
                loadedUiState?.run {
                    NodeBottomSheet(shownNodes, !showUnlabeledNodes, selectedNode) { node ->
                        vm.selectNode(node)
                        with(bottomSheetState) { if (node != null) expand() else shrink() }
                    }
                }
            },
            sheetDragHandle = { BottomSheetHandle() },
            sheetPeekHeight = sheetPeekHeight,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = {}, // Scan display not here to be rendered behind top bar
            containerColor = Color.Transparent
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
private fun Modifier.windowInsetsPadding(padding: PaddingValues) = this
    .padding(padding)
    .consumeWindowInsets(padding)

@Stable
@OptIn(ExperimentalMaterial3Api::class)
private data class BottomSheetStateWrapper(
    val bottomSheetState: SheetState,
    val coroutineScope: CoroutineScope
) {
    fun expand() {
        coroutineScope.launch { bottomSheetState.expand() }
    }

    fun shrink() {
        coroutineScope.launch { bottomSheetState.partialExpand() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun rememberBottomSheetStateWrapper(
    bottomSheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded)
    ),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(bottomSheetState, coroutineScope) {
    BottomSheetStateWrapper(bottomSheetState, coroutineScope)
}

@Composable
@OptIn(ExperimentalTelephotoApi::class)
private fun ScanDisplay(
    screenshotFile: File?,
    nodes: List<Node>,
    selectedNode: Node?,
    onNodeClick: (Node?) -> Unit,
    onZoomIn: () -> Unit
) {
    // Args referenced in the onClick lambda should be wrapped to make updates visible there
    val updatableNodes by rememberUpdatedState(nodes)
    val updatableOnNodeClick by rememberUpdatedState(onNodeClick)

    val zoomState = rememberZoomableState(
        ZoomSpec(minZoomFactor = 0.75f, maxZoomFactor = 10f)
    ).apply {
        // Let the node canvas transform itself, otherwise it gets pixilated when zoomed-in
        autoApplyTransformations = false
    }
    BoxWithConstraints(
        Modifier.zoomable(
            zoomState,
            onClick = { offset ->
                val offsetInContent = with(zoomState.coordinateSystem) {
                    SpatialOffset(offset, CoordinateSpace.Viewport).offsetIn(
                        CoordinateSpace.ZoomableContent
                    )
                }
                updatableOnNodeClick(updatableNodes.findClicked(offsetInContent))
            }
        )
    ) {
        Screenshot(screenshotFile, zoomState)
        NodeCanvas(nodes, selectedNode, zoomState)

        // Center on selected node
        var prevSelectedNode by retain { mutableStateOf<Node?>(null) }
        val viewportSize = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        LaunchedEffect(selectedNode) {
            if (selectedNode != prevSelectedNode) {
                prevSelectedNode = selectedNode?.apply {
                    zoomState.centerOn(fractionalBounds, viewportSize)
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

    // Call onZoom when user zooms-in
    val zoomed by retain { derivedStateOf { (zoomState.zoomFraction ?: 0f) > 0.00001f } }
    LaunchedEffect(zoomed, isInitialZoomSet) {
        if (zoomed && isInitialZoomSet) onZoomIn()
    }
}

// Originally, scanned nodes are sorted from outer to inner. But the user does not see this
// 3rd dimension and expects the "smaller" node or the node the center of which is closer to be
// selected.
private fun Iterable<Node>.findClicked(click: Offset): Node? =
    filter { it.fractionalBounds.contains(click) }
        .mapIndexed { i, node -> i to node }
        .sortedWith(
            compareBy<Pair<Int, Node>> { (_, node) -> node.fractionalBounds.area() }
                .thenBy { (_, node) -> (node.fractionalBounds.center - click).getDistanceSquared() }
                .thenBy { (i, _) -> i }
        )
        .firstOrNull()?.second

private fun Rect.area() = width * height

@OptIn(ExperimentalTelephotoApi::class)
private suspend fun ZoomableState.centerOn(rect: Rect, viewportSize: Size) {
    // ContentScale.Fit is uniform so scaleX == scaleY
    val factor = (ContentScale.Fit.computeScaleFactor(rect.size, viewportSize).scaleX * 0.9f)
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
private fun Screenshot(screenshotFile: File?, zoomState: ZoomableState) {
    if (screenshotFile != null) {
        SubSamplingImage(
            state = rememberSubSamplingImageState(
                SubSamplingImageSource.file(screenshotFile.toOkioPath()),
                zoomState
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        MissingScreenshotPlaceholder(zoomState)
    }
}

@Composable
private fun MissingScreenshotPlaceholder(zoomState: ZoomableState) {
    check(!zoomState.autoApplyTransformations)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Text(
        text = stringResource(R.string.could_not_take_screenshot),
        color = MaterialTheme.colorScheme.contentColorFor(containerColor).copy(alpha = 0.38f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .graphicsLayer {
                val transformation = zoomState.contentTransformation
                scaleX = transformation.scale.scaleX
                scaleY = transformation.scale.scaleY
                rotationZ = transformation.rotationZ
                translationX = transformation.offset.x
                translationY = transformation.offset.y
                transformOrigin = transformation.transformOrigin
            }
            .background(containerColor)
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
    )
}

@Composable
private fun NodeCanvas(nodes: List<Node>, selectedNode: Node?, zoomState: ZoomableState) {
    check(!zoomState.autoApplyTransformations)
    val defaultNodeStroke = Stroke(with(LocalDensity.current) { 1.dp.toPx() })
    val selectedNodeStroke = Stroke(with(LocalDensity.current) { 4.dp.toPx() })
    // White color + difference blending mode = inversion of underlying pixels. A graphicsLayer
    // is needed to have the blending mode applied to all rectangles together, if the mode would
    // be set for each rectangle separately the inversion would cancel out on intersections.
    Canvas(
        modifier = Modifier
            .graphicsLayer { blendMode = BlendMode.Difference }
            .fillMaxSize()
    ) {
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
                drawRect(
                    Color.White,
                    node.fractionalBounds.topLeft,
                    node.fractionalBounds.size,
                    style = defaultNodeStroke
                )
            }
            selectedNode?.run {
                drawRect(
                    Color.White,
                    fractionalBounds.topLeft,
                    fractionalBounds.size,
                    style = selectedNodeStroke
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    visible: Boolean,
    appPackage: String?,
    showUnlabeledNodes: Boolean,
    onBack: () -> Unit,
    onSwitchShowUnlabeledNodes: (() -> Unit)?
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
    onSwitchShowUnlabeledNodes: (() -> Unit)?
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
        tooltip = { PlainTooltip { Text(stringResource(descriptionRes)) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onSwitchShowUnlabeledNodes ?: {},
            enabled = onSwitchShowUnlabeledNodes != null
        ) {
            Icon(painterResource(iconRes), stringResource(descriptionRes))
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Surface(
        modifier = Modifier
            .padding(vertical = DefaultSpacing)
            .size(width = 32.dp, height = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    ) {}
}

@Composable
private fun NodeBottomSheet(
    nodes: List<Node>,
    filtered: Boolean,
    selectedNode: Node?,
    onClick: (Node?) -> Unit
) {
    if (selectedNode == null) {
        if (nodes.isEmpty()) {
            EmptyNodeListPlaceholder(filtered)
        } else {
            NodeList(nodes, onNodeClick = onClick)
        }
    } else {
        NodeDescription(selectedNode, onClose = { onClick(null) })
    }
}

@Composable
private fun EmptyNodeListPlaceholder(filtered: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(PaddingValues(bottom = WindowInsets.safeContentBottomDp()))
            .padding(horizontal = 2 * DefaultSpacing, vertical = DefaultSpacing)
    ) {
        Text(
            buildAnnotatedString {
                if (filtered) {
                    append(stringResource(R.string.no_elements_match_filters))
                } else {
                    append(stringResource(R.string.no_elements_in_scan))
                    appendParagraphSpacer()
                    append(stringResource(R.string.no_elements_in_scan_hints))
                }
            },
            textAlign = TextAlign.Center,
            color = ListItemDefaults.colors().disabledContentColor
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeList(nodes: List<Node>, onNodeClick: (Node) -> Unit) {
    val insetsPadding = PaddingValues(bottom = WindowInsets.safeContentBottomDp())
    LazyColumn(
        contentPadding = insetsPadding,
        modifier = Modifier.consumeWindowInsets(insetsPadding)
    ) {
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
            if (node.isClickable) R.drawable.touch_app_outlined else R.drawable.code
        ),
        contentDescription = stringResource(
            if (node.isClickable) R.string.clickable_element else R.string.not_clickable_element
        )
    )
}

private fun Node.boundsAsString() = "${horizontalBoundsAsString()}, ${verticalBoundsAsString()}"
private fun Node.horizontalBoundsAsString() = with(bounds) { "x=$left–${right}px" }
private fun Node.verticalBoundsAsString() = with(bounds) { "y=$top–${bottom}px" }

@Composable
private fun NodeDescription(node: Node, onClose: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DefaultSpacing / 2),
        modifier = Modifier
            .consumeWindowInsets(PaddingValues(bottom = WindowInsets.safeContentBottomDp()))
            .padding(horizontal = DefaultSpacing * 2)
            .verticalScroll(rememberScrollState())
    ) {
        NodeDescriptionHeader(node.className.substringAfterLast('.'), onClose)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(node.horizontalBoundsAsString())
                NodeDescriptionFlowRowDivider()
                Text(node.verticalBoundsAsString())
                NodeDescriptionFlowRowDivider()
                Text(
                    stringResource(
                        if (node.isClickable) R.string.clickable else R.string.not_clickable
                    )
                )
            }
        }

        Spacer(Modifier.height(DefaultSpacing))

        NodeDescriptionElement(
            node.viewId ?: "—",
            stringResource(R.string.view_id),
            index = 0, count = 2,
            enabled = node.viewId != null
        )
        NodeDescriptionElement(
            node.className,
            stringResource(R.string.clazz),
            index = 1, count = 2
        )

        Spacer(Modifier.height(DefaultSpacing))

        NodeRawInfo(node.rawInfo)

        Spacer(Modifier.height(WindowInsets.safeContentBottomDp()))
    }
}

@Composable
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

@Composable
private fun NodeDescriptionFlowRowDivider() {
    Text("•")
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeDescriptionElement(
    text: String,
    label: String,
    index: Int,
    count: Int,
    enabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    SegmentedListItem(
        onClick = {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(text, text))) }
        },
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        supportingContent = { Text(label) },
        content = { Text(text) }
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeRawInfo(info: String) {
    var collapsed by retain { mutableStateOf(true) }
    SegmentedListItem(
        onClick = { collapsed = !collapsed },
        shapes = ListItemDefaults.segmentedShapes(0, if (collapsed) 1 else 2),
        trailingContent = {
            val rotation by animateFloatAsState(if (collapsed) 0f else 180f)
            Icon(
                painterResource(R.drawable.keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    ) {
        Text(stringResource(R.string.raw_info))
    }
    AnimatedVisibility(
        !collapsed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SegmentedListItem(
            onClick = { collapsed = !collapsed },
            shapes = ListItemDefaults.segmentedShapes(1, 2)
        ) {
            SelectionContainer {
                Text(info, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
