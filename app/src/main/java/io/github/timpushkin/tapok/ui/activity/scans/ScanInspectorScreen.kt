// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.ui.activity.rules.RuleBasis
import io.github.timpushkin.tapok.ui.activity.rules.ruleBasisFrom
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScanInspectorScreen(
    scanId: Long,
    onDelete: () -> Unit,
    onCreateRule: (RuleBasis) -> Unit,
    onClose: () -> Unit,
    vm: ScanInspectorViewModel = hiltViewModel(
        key = scanId.toString()
    ) { f: ScanInspectorViewModelFactory -> f.create(scanId) }
) {
    Box(
        modifier = Modifier
            // Ensure bottom sheet and zoomed image are not shown out of bounds on predictive-back
            // animation
            .clipToBounds()
            .dottedBackground()
    ) {
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val loadedUiState = uiState as? ScanInspectorUiState.Loaded

        var showBars by rememberSaveable { mutableStateOf(true) }
        val insetsController = LocalActivity.current?.window?.let { window ->
            WindowCompat.getInsetsController(window, LocalView.current)
        }
        DisposableEffect(showBars, insetsController == null) {
            val systemBars = WindowInsetsCompat.Type.systemBars()
            insetsController?.run { if (showBars) show(systemBars) else hide(systemBars) }
            onDispose { if (!showBars) insetsController?.show(systemBars) }
        }

        val sheetState = rememberBottomSheetStateWrapper()
        val sheetPeekHeightDp by animateDpAsState(
            if (loadedUiState == null) 0.dp else bottomSheetPeekHeight()
        )

        loadedUiState?.run {
            ZoomableScanView(
                screenshot,
                shownNodes,
                nodes.firstOrNull(),
                selectedNode,
                onNodeSelected = { node ->
                    if (node != selectedNode) { // Clicked on deselected node
                        vm.selectNode(node)
                        sheetState.shrink()
                    } else if (selectedNode != null) { // Clicked on selected node
                        vm.selectNode(null)
                        sheetState.shrink()
                    } else { // Clicked on empty area when no node is selected
                        showBars = !showBars
                    }
                },
                onZoomIn = { showBars = false },
                obscuredPadding = PaddingValues(bottom = sheetPeekHeightDp)
            )
        }

        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(sheetState.bottomSheetState),
            topBar = {
                TopBar(
                    rootPackage = loadedUiState?.run { scan.rootPackageName },
                    loaded = loadedUiState != null,
                    visible = showBars,
                    showUnreferenceableNodes = loadedUiState?.showUnreferenceableNodes ?: false,
                    onBack = onClose,
                    onSwitchShowUnreferenceableNodes = vm::switchShowUnreferenceableNodes,
                    onDelete = {
                        onDelete()
                        onClose()
                    }
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
                            sheetState.shrink()
                        },
                        onCreateRuleFromNode = { node -> onCreateRule(ruleBasisFrom(scan, node)) }
                    )
                }
            },
            sheetDragHandle = { BottomSheetHandle() },
            sheetPeekHeight = sheetPeekHeightDp,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = {}, // Scan view not here to be rendered behind top bar
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
private fun WindowInsets.Companion.safeDrawingBottomDp() = with(LocalDensity.current) {
    safeDrawing.getBottom(density = this).toDp()
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
private fun TopBar(
    rootPackage: String?,
    loaded: Boolean,
    visible: Boolean,
    showUnreferenceableNodes: Boolean,
    onBack: () -> Unit,
    onSwitchShowUnreferenceableNodes: () -> Unit,
    onDelete: () -> Unit
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
                IconButton(onBack) {
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
                IconButton(onDelete) {
                    Icon(
                        painterResource(R.drawable.delete_outlined),
                        contentDescription = stringResource(R.string.delete)
                    )
                }
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
@OptIn(ExperimentalMaterial3Api::class)
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

private val BOTTOM_SHEET_HANDLE_HEIGHT = 4.dp

@Composable
private fun bottomSheetPeekHeight(): Dp = with(LocalDensity.current) {
    // Height needed for the node description to look good when peeked
    val contentPeekHeight =
        /* BottomSheetHandle */
        BOTTOM_SHEET_HANDLE_HEIGHT + 2 * DefaultInnerSpacing +
                /* NodeDescription: NodeDescriptionHeader */
                maxOf(
                    MaterialTheme.typography.titleLarge.lineHeight.toDp(),
                    LocalMinimumInteractiveComponentSize.current,
                    IconButtonDefaults.extraSmallContainerSize().height
                ) +
                DefaultInnerSpacing +
                /* NodeDescription: first line of FlowRow */
                MaterialTheme.typography.labelMedium.lineHeight.toDp() +
                DefaultInnerSpacing +
                /* Possibly obscured space */
                WindowInsets.safeDrawingBottomDp()
    // Height needed for the sheet to be interactable when peeking
    val minInteractablePeekHeight = LocalMinimumInteractiveComponentSize.current +
            WindowInsets.safeContentBottomDp()
    return contentPeekHeight.coerceAtLeast(minInteractablePeekHeight)
}

@Composable
private fun BottomSheetHandle() {
    Surface(
        modifier = Modifier
            .padding(vertical = DefaultInnerSpacing)
            .size(width = 32.dp, height = BOTTOM_SHEET_HANDLE_HEIGHT),
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
@OptIn(ExperimentalMaterial3Api::class)
private fun NodeDescriptionHeader(onCreateRuleFromNode: () -> Unit, onClose: () -> Unit) {
    // When the height of the peeking content gets changed, bottomSheetPeekHeight must be updated
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                stringResource(R.string.selected_element),
                style = MaterialTheme.typography.titleLarge,
                // Limit the number of lines to be able to calculate bottomSheetPeekHeight
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
