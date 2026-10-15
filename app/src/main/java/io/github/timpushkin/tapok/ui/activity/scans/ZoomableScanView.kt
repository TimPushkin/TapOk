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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.scans.Node
import kotlinx.coroutines.flow.drop
import kotlin.math.max
import kotlin.math.min

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 10f
private const val INITIAL_ZOOM = 0.8f
private const val MAX_AUTO_ZOOM = 3f

/**
 * Fraction of the visible container area occupied by selected node.
 */
private const val SELECTION_FIT_FRACTION = 0.8f

private val NORMAL_BORDER_WIDTH = 1.dp
private val NORMAL_HALO_WIDTH = 3.dp
private val SELECTED_BORDER_WIDTH = 3.dp
private val SELECTED_HALO_WIDTH = 7.dp

private val DEFAULT_CONTENT_SIZE = IntSize(1080, 1080)

@Composable
fun ZoomableScanView(
    image: ImageBitmap?,
    nodes: List<Node>,
    rootNode: Node?,
    selectedNode: Node?,
    onNodeSelected: (Node?) -> Unit,
    onZoomIn: () -> Unit,
    obscuredPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val contentSize = remember(image, nodes) {
        when {
            image != null -> IntSize(image.width, image.height)
            rootNode != null -> rootNode.normalizedBounds.size
            else -> DEFAULT_CONTENT_SIZE
        }
    }
    val panBounds = remember(nodes, contentSize) {
        var left = 0
        var top = 0
        var right = contentSize.width
        var bottom = contentSize.height
        for (node in nodes) {
            left = min(left, node.normalizedBounds.left)
            top = min(top, node.normalizedBounds.top)
            right = max(right, node.normalizedBounds.right)
            bottom = max(bottom, node.normalizedBounds.bottom)
        }
        IntRect(left, top, right, bottom)
    }
    val state = rememberZoomState(
        contentSize.toSize(), panBounds.toRect(), MIN_ZOOM..MAX_ZOOM, INITIAL_ZOOM
    )

    val bodyMediumTextStyle = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    val placeholderText = stringResource(R.string.could_not_take_screenshot)
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerLow
    val placeholderTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val placeholderTextStyle = remember(bodyMediumTextStyle, placeholderTextColor) {
        bodyMediumTextStyle.copy(color = placeholderTextColor)
    }
    val measuredPlaceholderText = remember(placeholderText, placeholderTextStyle) {
        textMeasurer.measure(placeholderText, style = placeholderTextStyle)
    }

    val currentNodes by rememberUpdatedState(nodes)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)
    Canvas(
        modifier = modifier
            .zoomable(state, onClick = { currentOnNodeSelected(currentNodes.findClicked(it)) })
            .fillMaxSize()
    ) {
        withTransform(
            {
                translate(state.offset.x, state.offset.y)
                scale(state.scale, state.scale, pivot = Offset.Zero)
            }
        ) {
            if (image != null) {
                drawImage(image, filterQuality = FilterQuality.Medium)
            } else {
                drawPlaceholder(
                    measuredPlaceholderText,
                    placeholderColor,
                    contentSize.toSize()
                )
            }

            val inverseScale = if (state.scale > 0f) 1f / state.scale else 1f
            val borderWidth = NORMAL_BORDER_WIDTH.toPx() * inverseScale
            val haloWidth = NORMAL_HALO_WIDTH.toPx() * inverseScale
            val selBorderWidth = SELECTED_BORDER_WIDTH.toPx() * inverseScale
            val selHaloWidth = SELECTED_HALO_WIDTH.toPx() * inverseScale
            drawNodes(nodes, selectedNode, borderWidth, haloWidth, selBorderWidth, selHaloWidth)
        }
    }

    // Trigger onZoomIn whenever scale goes above baseScale
    val currentOnZoomIn by rememberUpdatedState(onZoomIn)
    LaunchedEffect(state) {
        snapshotFlow { state.scale > state.baseScale }
            .drop(1)
            .collect { zoomed -> if (zoomed) currentOnZoomIn() }
    }

    // Focus on selected node
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val obscuredRect = remember(obscuredPadding, density, layoutDirection) {
        obscuredPadding.toPixelRect(density, layoutDirection)
    }
    var prevSelectedNodeHash by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(selectedNode) {
        val currSelectedNodeHash = selectedNode.hashCode()
        if (currSelectedNodeHash != prevSelectedNodeHash) {
            prevSelectedNodeHash = currSelectedNodeHash
            if (selectedNode != null) {
                state.focus(selectedNode.normalizedFloatBounds, obscuredRect)
            }
        }
    }
}

private fun DrawScope.drawPlaceholder(measuredText: TextLayoutResult, color: Color, size: Size) {
    drawRect(color = color, size = size)
    drawText(measuredText, topLeft = size.center - measuredText.size.toSize().center)
}

private fun DrawScope.drawNodes(
    nodes: List<Node>,
    selectedNode: Node?,
    borderWidth: Float,
    haloWidth: Float,
    selBorderWidth: Float,
    selHaloWidth: Float
) {
    for (node in nodes) {
        val selected = node == selectedNode
        val currentBorder = if (selected) selBorderWidth else borderWidth
        val currentHalo = if (selected) selHaloWidth else haloWidth

        drawRect(
            color = Color.White,
            topLeft = node.normalizedFloatBounds.topLeft,
            size = node.normalizedFloatBounds.size,
            style = Stroke(currentHalo)
        )
        drawRect(
            color = Color.Black,
            topLeft = node.normalizedFloatBounds.topLeft,
            size = node.normalizedFloatBounds.size,
            style = Stroke(currentBorder)
        )
    }
}

private fun Iterable<Node>.findClicked(click: Offset): Node? =
    filter { it.normalizedFloatBounds.contains(click) }
        .sortedWith(
            compareBy<Node> { it.normalizedBounds.run { width * height } }
                .thenBy { (it.normalizedFloatBounds.center - click).getDistanceSquared() }
        )
        .firstOrNull()

private fun PaddingValues.toPixelRect(density: Density, layoutDirection: LayoutDirection): Rect =
    with(density) {
        Rect(
            left = calculateLeftPadding(layoutDirection).toPx(),
            top = calculateTopPadding().toPx(),
            right = calculateRightPadding(layoutDirection).toPx(),
            bottom = calculateBottomPadding().toPx()
        )
    }

private suspend fun ZoomState.focus(target: Rect, obscuredPadding: Rect) {
    val maxAutoScale = (baseScale * MAX_AUTO_ZOOM).coerceIn(minScale, maxScale)

    val visibleAreaSize = Size(
        (containerSize.width - obscuredPadding.left - obscuredPadding.right).coerceAtLeast(0f),
        (containerSize.height - obscuredPadding.top - obscuredPadding.bottom).coerceAtLeast(0f)
    )
    val visibleAreaCenter = Offset(
        obscuredPadding.left + visibleAreaSize.width / 2,
        obscuredPadding.top + visibleAreaSize.height / 2,
    )

    val targetScale = when {
        visibleAreaSize.isEmpty() -> return
        target.isEmpty -> maxAutoScale
        else -> ContentScale.Fit.computeScaleFactor(
            target.size,
            visibleAreaSize * SELECTION_FIT_FRACTION
        ).scaleX.coerceIn(minScale, maxAutoScale)
    }
    val targetOffset = visibleAreaCenter - target.center * targetScale
    animateTo(targetScale, targetOffset)
}
