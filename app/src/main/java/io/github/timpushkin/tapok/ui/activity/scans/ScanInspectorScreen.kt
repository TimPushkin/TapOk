package io.github.timpushkin.tapok.ui.activity.scans

import android.content.ClipData
import androidx.activity.compose.LocalActivity
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.ui.activity.rules.IncompleteRule
import io.github.timpushkin.tapok.ui.activity.rules.prefilledRuleFrom
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomFocalPoint
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContent
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace
import me.saket.telephoto.zoomable.spatial.SpatialOffset
import me.saket.telephoto.zoomable.zoomable

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScanInspectorScreen(
    scanId: Long,
    onCreateRule: (IncompleteRule) -> Unit,
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
                screenshot,
                nodes.firstOrNull(),
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
                    rootPackage = loadedUiState?.run { scan.rootPackageName },
                    loaded = loadedUiState != null,
                    visible = showBars,
                    showUnreferenceableNodes = loadedUiState?.showUnreferenceableNodes ?: false,
                    onBack = onClose,
                    onSwitchShowUnreferenceableNodes = vm::switchShowUnreferenceableNodes
                )
            },
            sheetContent = {
                loadedUiState?.run {
                    NodeBottomSheet(
                        shownNodes,
                        !showUnreferenceableNodes,
                        selectedNode,
                        onSelectNode = { node ->
                            vm.selectNode(node)
                            with(bottomSheetState) { if (node != null) expand() else shrink() }
                        },
                        onCreateRuleFromNode = { node ->
                            onCreateRule(prefilledRuleFrom(scan, node))
                        }
                    )
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

    val horizontalSpacings = (size.width / spacingPx).toInt()
    val verticalSpacings = (size.height / spacingPx).toInt()

    val horizontalOffset = (size.width - (horizontalSpacings * spacingPx)) / 2
    val verticalOffset = (size.height - (verticalSpacings * spacingPx)) / 2

    for (i in 0..horizontalSpacings) {
        for (j in 0..verticalSpacings) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(
                    x = horizontalOffset + (i * spacingPx),
                    y = verticalOffset + (j * spacingPx)
                ),
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
    screenshot: ImageBitmap?,
    rootNode: Node?,
    nodes: List<Node>,
    selectedNode: Node?,
    onNodeClick: (Node?) -> Unit,
    onZoomIn: () -> Unit
) {
    // Args referenced in the onClick lambda should be wrapped to make updates visible there
    val updatableNodes by rememberUpdatedState(nodes)
    val updatableOnNodeClick by rememberUpdatedState(onNodeClick)

    BoxWithConstraints {
        val density = LocalDensity.current
        val viewportSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val scanSize = remember(screenshot, nodes) {
            screenshot?.run { Size(width.toFloat(), height.toFloat()) }
                ?: rootNode?.run { fractionalBounds.size }
                ?: Size(1080f, 1080f)
        }

        val zoomState = rememberZoomableState(
            // minZoomFactor = 2f would be better, but the initial zoom factor is always 1, and it
            // only jumps into the specified range on first zoom attempt. Moreover, because the
            // viewport's size is animated, that leads to off-center zoom. Thus, we have to use the
            // default minZoomFactor of 1.
            ZoomSpec(maxZoomFactor = 10f),
            // Let the node canvas transform itself, otherwise nodes get pixilated when zoomed-in
            autoApplyTransformations = false
        ).apply {
            setContentLocation(ZoomableContentLocation.unscaledAndTopLeftAligned(scanSize))
            // To be able to center on any pixel of the scan we need to be able to over-scroll by
            // half of the corresponding dimension on each size which the padding allows. Sadly,
            // https://github.com/saket/telephoto/issues/173 does not allow making it large enough.
            contentPadding = with(density) { scanSize.toDpSize() }.run {
                PaddingValues(horizontal = width / 3, vertical = height / 3)
            }
        }

        val textMeasurer = rememberTextMeasurer()
        val placeholderBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
        val placeholderText = stringResource(R.string.could_not_take_screenshot)
        val placeholderTextStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.contentColorFor(placeholderBackgroundColor)
        )

        val defaultNodeStroke = Stroke(with(density) { 1.dp.toPx() })
        val selectedNodeStroke = Stroke(with(density) { 4.dp.toPx() })

        Canvas(
            Modifier
                .fillMaxSize()
                .zoomable(
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
                if (screenshot != null) {
                    // TODO: use srcOffset, srcSize to only draw the visible part
                    drawImage(screenshot)
                } else {
                    drawScreenshotPlaceholder(
                        textMeasurer,
                        placeholderText,
                        placeholderTextStyle,
                        placeholderBackgroundColor
                    )
                }

                // White + difference = inversion of underlying pixels. A layer is needed to have
                // the blending applied to all rectangles together. Would it be set for each
                // rectangle separately, the inversion would cancel out on intersections.
                drawIntoCanvas { canvas ->
                    canvas.withSaveLayer(
                        bounds = Rect(Offset.Zero, size),
                        paint = Paint().apply { blendMode = BlendMode.Difference }
                    ) {
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
        }

        // Call onZoom when user zooms-in
        val zoomed by retain { derivedStateOf { (zoomState.zoomFraction ?: 0f) > 0.00001f } }
        LaunchedEffect(zoomed) {
            if (zoomed) onZoomIn()
        }

        // Center on selected node
        var prevSelectedNode by retain { mutableStateOf<Node?>(null) }
        LaunchedEffect(selectedNode) {
            if (selectedNode != prevSelectedNode) {
                prevSelectedNode = selectedNode?.apply {
                    zoomState.centerOn(fractionalBounds, viewportSize)
                }
            }
        }
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
    val factor = (ContentScale.Fit.computeScaleFactor(rect.size, viewportSize * 0.9f).scaleX)
        .takeUnless { it.isNaN() }
        // Larger values make it hard to understand the surroundings, user can zoom more if needed
        ?.coerceAtMost(3f)
        ?: return
    val center = SpatialOffset(rect.center, CoordinateSpace.ZoomableContent)
    zoomTo(factor, ZoomFocalPoint.moveToViewportCenter(center))
}

private fun DrawScope.drawScreenshotPlaceholder(
    textMeasurer: TextMeasurer,
    text: String,
    textStyle: TextStyle,
    backgroundColor: Color
) {
    drawRect(backgroundColor)

    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = textStyle,
        constraints = Constraints(maxWidth = size.width.toInt())
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            (size.width - textLayoutResult.size.width) / 2,
            (size.height - textLayoutResult.size.height) / 2
        )
    )
}

@Composable
private fun TopBar(
    rootPackage: String?,
    loaded: Boolean,
    visible: Boolean,
    showUnreferenceableNodes: Boolean,
    onBack: () -> Unit,
    onSwitchShowUnreferenceableNodes: () -> Unit
) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessLow))
    ) {
        val colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        CenterAlignedTopAppBar(
            title = {
                AnimatedVisibility(loaded, enter = fadeIn(), exit = fadeOut()) {
                    if (loaded) {
                        Text(
                            rootPackage ?: stringResource(R.string.unidentifiable_app),
                            maxLines = 1,
                            color = colors.run {
                                if (rootPackage != null) titleContentColor else subtitleContentColor
                            },
                            modifier = if (rootPackage != null) {
                                val scope = rememberCoroutineScope()
                                val clipboard = LocalClipboard.current
                                Modifier.clickable {
                                    scope.launch { clipboard.setTextEntry(rootPackage) }
                                }
                            } else {
                                Modifier
                            }
                                .basicMarquee()
                        )
                    }
                }
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
                ShowUnreferenceableNodesSwitch(
                    showUnreferenceableNodes,
                    onSwitchShowUnreferenceableNodes.takeIf { loaded }
                )
            },
            colors = colors,
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                )
            )
        )
    }
}

private suspend fun Clipboard.setTextEntry(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
}

@Composable
private fun ShowUnreferenceableNodesSwitch(value: Boolean, onSwitch: (() -> Unit)?) {
    val iconRes = if (value) R.drawable.visibility_filled else R.drawable.visibility_off_outlined
    val descriptionRes = R.string.show_hide_elements_that_cannot_be_referenced
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(stringResource(descriptionRes)) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onSwitch ?: {}, enabled = onSwitch != null) {
            Icon(painterResource(iconRes), stringResource(descriptionRes))
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Surface(
        modifier = Modifier
            .padding(vertical = DefaultInnerSpacing)
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
    onSelectNode: (Node?) -> Unit,
    onCreateRuleFromNode: (Node) -> Unit
) {
    if (selectedNode == null) {
        if (nodes.isEmpty()) {
            EmptyNodeListPlaceholder(filtered)
        } else {
            NodeList(nodes, onNodeClick = onSelectNode)
        }
    } else {
        NodeDescription(
            selectedNode,
            onCreateRuleFromNode = { onCreateRuleFromNode(selectedNode) },
            onClose = { onSelectNode(null) }
        )
    }
}

@Composable
private fun EmptyNodeListPlaceholder(filtered: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(PaddingValues(bottom = WindowInsets.safeContentBottomDp()))
            .padding(DefaultOuterSpacing)
    ) {
        Text(
            if (filtered) {
                stringResource(R.string.no_elements_match_filters).let {
                    rememberAnnotatedString(it) { append(it) }
                }
            } else {
                annotatedStringArrayResource(R.array.no_elements_in_scan).let {
                    rememberAnnotatedString(it) { appendAnnotatedParagraphs(it) }
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
            val searchCriterion = node.run {
                viewId ?: contentDescription?.let { "\"$it\"" } ?: text?.let { "\"$it\"" }
            }
            assert((searchCriterion != null) == node.isReferenceableInRules)
            ListItem(
                onClick = { onNodeClick(node) },
                leadingContent = { NodeIcon(node) },
                content = {
                    Text(
                        node.className ?: stringResource(R.string.element_of_unspecified_class),
                        color = with(ListItemDefaults.colors()) {
                            if (node.isReferenceableInRules) {
                                supportingContentColor
                            } else {
                                disabledSupportingContentColor
                            }
                        },
                        maxLines = 1,
                        overflow = if (node.className != null) {
                            TextOverflow.StartEllipsis
                        } else {
                            TextOverflow.Ellipsis
                        }
                    )
                },
                supportingContent = searchCriterion?.let { viewId ->
                    {
                        Text(
                            viewId,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                },
                overlineContent = {
                    Text(
                        node.boundsAsString(),
                        color = with(ListItemDefaults.colors()) {
                            if (node.isReferenceableInRules) {
                                overlineContentColor
                            } else {
                                disabledOverlineContentColor
                            }
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
            if (node.isClickable) R.string.clickable else R.string.not_clickable
        )
    )
}

private fun Node.boundsAsString() = "${horizontalBoundsAsString()}, ${verticalBoundsAsString()}"
private fun Node.horizontalBoundsAsString() = with(bounds) { "x=$left–${right}px" }
private fun Node.verticalBoundsAsString() = with(bounds) { "y=$top–${bottom}px" }

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeDescription(node: Node, onCreateRuleFromNode: () -> Unit, onClose: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DefaultInnerSpacing),
        modifier = Modifier
            .consumeWindowInsets(PaddingValues(bottom = WindowInsets.safeContentBottomDp()))
            .padding(horizontal = DefaultOuterSpacing)
            .verticalScroll(rememberScrollState())
    ) {
        NodeDescriptionHeader(onCreateRuleFromNode, onClose)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(DefaultInnerSpacing / 2)) {
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

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            NodeDescriptionElement(
                node.viewId,
                stringResource(R.string.view_id),
                index = 0, count = 3
            )
            NodeDescriptionElement(
                node.contentDescription,
                stringResource(R.string.content_description),
                index = 1, count = 3
            )
            NodeDescriptionElement(
                node.text,
                stringResource(R.string.text),
                index = 2, count = 3
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            NodeDescriptionElement(
                node.packageName,
                stringResource(R.string.package_name),
                index = 0, count = 2
            )
            NodeDescriptionElement(
                node.className,
                stringResource(R.string.class_name),
                index = 1, count = 2
            )
        }

        NodeRawInfo(node.rawInfo)

        Spacer(Modifier.height(WindowInsets.safeContentBottomDp()))
    }
}

@Composable
private fun NodeDescriptionHeader(onCreateRuleFromNode: () -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                stringResource(R.string.selected_element),
                style = MaterialTheme.typography.titleLarge
            )
        }

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Below
            ),
            tooltip = {
                PlainTooltip { Text(stringResource(R.string.create_rule_from_this_element)) }
            },
            state = rememberTooltipState()
        ) {
            FilledTonalIconButton(
                onCreateRuleFromNode,
                shape = IconButtonDefaults.extraSmallRoundShape,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(IconButtonDefaults.extraSmallContainerSize())
            ) {
                Icon(
                    painterResource(R.drawable.create_rule_outlined),
                    contentDescription = stringResource(R.string.create_rule_from_this_element),
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize)
                )
            }
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
    text: String?,
    label: String,
    index: Int,
    count: Int
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    SegmentedListItem(
        onClick = { if (text != null) scope.launch { clipboard.setTextEntry(text) } },
        enabled = text != null,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        supportingContent = { Text(label) },
        content = { Text(text ?: "—") }
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun NodeRawInfo(info: String) {
    var collapsed by retain { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
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
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
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
}
