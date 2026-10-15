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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.geometry.lerp as lerpGeometry

/**
 * At least this fraction of the zoomable container should be occupied by the pannable area (when
 * it is large enough). Must be > 0 and <= 1.
 */
private const val PAN_VISIBLE_FRACTION = 0.5f

/**
 * Sensitivity of double-tap-and-drag zoom gesture.
 */
private const val QUICK_ZOOM_SENSITIVITY = 0.012f

private val DEFAULT_ANIMATION_SPEC = tween<Float>(300)

@Stable
class ZoomState(
    /**
     * Size of the main zoomable content in pixels.
     */
    val contentSize: Size,
    /**
     * Defines how much panning is allowed. It will be possible to pan as much as needed for placing
     * any pixel in the bounds at the center of the zoomable container. Defined in content's
     * coordinates: the content starts at (0, 0) of the bounds, coordinates increase towards
     * bottom-right.
     *
     * Must be large enough to include (0, 0) - (`contentSize.width`, `contentSize.height`).
     *
     * This is separated from [contentSize] to allow adding pannable ares not considered for
     * zoom-related calculations:
     * - if a pixel is inside [contentSize], it is guaranteed to be visible initially (unless
     * [initialZoom] is greater than 1),
     * - if a pixel is outside of [contentSize] but inside [panBounds], it is guaranteed to be
     * possible to center on it,
     * - if a pixel is outside of [panBounds], it may be impossible to center on it or even see it
     * at all.
     */
    val panBounds: Rect,
    /**
     * How much the main content can be zoomed. Same scale as for [initialZoom].
     */
    val zoomRange: ClosedFloatingPointRange<Float>,
    /**
     * How the main content is zoomed initially. 1 means the content is fitted into the zoomable
     * container.
     */
    val initialZoom: Float
) {
    /**
     * Size of the zoomable container in pixels. Must be set via [updateContainerSize] before using
     * the state.
     */
    val containerSize: Size get() = _containerSize
    private var _containerSize = Size.Zero

    /**
     * [initialZoom] rescaled so that [contentSize] × [baseScale] = [containerSize] × [initialZoom].
     */
    val baseScale: Float get() = _baseScale
    private var _baseScale = 1f

    /**
     * [`zoomRange.start`][zoomRange] in the scale of [baseScale].
     */
    val minScale: Float get() = _minScale
    private var _minScale = 1f

    /**
     * [`zoomRange.endInclusive`][zoomRange] in the scale of [baseScale].
     */
    val maxScale: Float get() = _maxScale
    private var _maxScale = 1f

    /**
     * Current zoom level in the scale of [baseScale].
     */
    val scale: Float get() = _scale
    private var _scale by mutableFloatStateOf(Float.NaN)

    /**
     * Current offset of the content's top-level corner from the container's top-level corner,
     * dragging the content towards bottom-right increases this offset.
     */
    val offset: Offset get() = _offset
    private var _offset by mutableStateOf(Offset.Unspecified)

    init {
        require(!contentSize.isEmpty()) { "Content cannot be empty" }
        require(
            panBounds.left <= 0 && panBounds.top <= 0 &&
                    panBounds.right >= contentSize.width && panBounds.bottom >= contentSize.height
        ) { "Pan bounds must be large enough to fit contentSize placed at (0, 0)" }
        require(zoomRange.start >= 0) { "Zoom range cannot include negative values" }
        require(initialZoom in zoomRange) { "Initial zoom must be in allowed zoom range" }
    }

    /**
     * Updates [containerSize]. Must be called before using any other method of the state.
     */
    fun updateContainerSize(newContainerSize: Size) {
        require(!newContainerSize.isEmpty()) { "Container cannot be empty" }
        if (newContainerSize == containerSize) {
            return
        }

        val oldContainerSize = containerSize.also { _containerSize = newContainerSize }

        _baseScale = ContentScale.Fit.computeScaleFactor(contentSize, containerSize).scaleX
        _minScale = baseScale * zoomRange.start
        _maxScale = baseScale * zoomRange.endInclusive

        if (oldContainerSize == Size.Zero) {
            val initialScale = baseScale * initialZoom
            snapTo(
                initialScale,
                Offset(
                    (containerSize.width - contentSize.width * initialScale) / 2,
                    (containerSize.height - contentSize.height * initialScale) / 2
                )
            )
        } else {
            val oldContainerCenterInContent = containerToContent(oldContainerSize.center)
            snapTo(scale, containerSize.center - oldContainerCenterInContent * scale)
        }
    }

    /**
     * Converts container's coordinates into content's coordinates.
     */
    fun containerToContent(containerPos: Offset): Offset = (containerPos - offset) / scale

    /**
     * Sets [scale][ZoomState.scale] and [offset][ZoomState.offset] to the specified values.
     */
    fun snapTo(scale: Float, offset: Offset) {
        _scale = scale.coerceIn(minScale, maxScale)
        _offset = boundOffset(offset, this.scale)
    }

    private fun boundOffset(offset: Offset, scale: Float = this.scale): Offset {
        val xRange = boundOffsetAxis(panBounds.left, panBounds.right, containerSize.width, scale)
        val yRange = boundOffsetAxis(panBounds.top, panBounds.bottom, containerSize.height, scale)
        return Offset(offset.x.coerceIn(xRange), offset.y.coerceIn(yRange))
    }

    private fun boundOffsetAxis(
        panBoundsStart: Float,
        panBoundsEnd: Float,
        containerLength: Float,
        scale: Float
    ): ClosedFloatingPointRange<Float> {
        val scaledPannableLength = (panBoundsEnd - panBoundsStart) * scale
        val minVisiblePixels = containerLength * PAN_VISIBLE_FRACTION
        assert(minVisiblePixels > 0 && minVisiblePixels <= containerLength)

        val minPanLimit: Float
        val maxPanLimit: Float
        if (scaledPannableLength >= minVisiblePixels) {
            // Main case: pannable area should occupy at least minVisiblePixels of the container
            minPanLimit = minVisiblePixels - scaledPannableLength
            maxPanLimit = containerLength - minVisiblePixels
        } else {
            // Pannable area is not large enough to cover minVisiblePixels, so just don't allow
            // it outside the container
            minPanLimit = 0f
            maxPanLimit = containerLength - scaledPannableLength
        }

        // Offset places the (0,0) of the content, but panBounds can start earlier, so must
        // shift the constraints accordingly
        val boundsShift = panBoundsStart * scale
        val lowerBound = minPanLimit - boundsShift
        val upperBound = maxPanLimit - boundsShift

        return lowerBound..upperBound
    }

    /**
     * Sets [scale] to the specified value and pans so that [centroid] stays in place.
     *
     * [centroid] is to be specified in container coordinates.
     */
    fun zoomTo(scale: Float, centroid: Offset) {
        val boundedScale = scale.coerceIn(minScale, maxScale)
        val centroidInContent = containerToContent(centroid)
        val newOffset = centroid - centroidInContent * boundedScale
        snapTo(boundedScale, newOffset)
    }

    /**
     * Animates [scale] and [offset] to the specified values.
     */
    suspend fun animateTo(
        targetScale: Float,
        targetOffset: Offset,
        animationSpec: AnimationSpec<Float> = DEFAULT_ANIMATION_SPEC
    ) {
        val startScale = scale
        val boundedTargetScale = targetScale.coerceIn(minScale, maxScale)

        val startOffset = offset
        val boundedTargetOffset = boundOffset(targetOffset, boundedTargetScale)

        animate(0f, 1f, animationSpec = animationSpec) { fraction, _ ->
            val currentScale = lerp(startScale, boundedTargetScale, fraction)
            val currentOffset = lerpGeometry(startOffset, boundedTargetOffset, fraction)
            snapTo(currentScale, currentOffset)
        }
    }

    /**
     * Animates [scale] to the specified value so that [centroid] stays in place.
     *
     * [centroid] is to be specified in container coordinates.
     */
    suspend fun animateZoomTo(
        targetScale: Float,
        centroid: Offset,
        animationSpec: AnimationSpec<Float> = DEFAULT_ANIMATION_SPEC
    ) {
        val boundedTargetScale = targetScale.coerceIn(minScale, maxScale)
        val centroidInContent = containerToContent(centroid)
        animate(scale, boundedTargetScale, animationSpec = animationSpec) { currentScale, _ ->
            val currentTargetOffset = centroid - centroidInContent * currentScale
            snapTo(currentScale, currentTargetOffset)
        }
    }

    companion object {
        val Saver = Saver<ZoomState, Map<String, Float>>(
            save = { state ->
                mapOf(
                    "contentWidth" to state.contentSize.width,
                    "contentHeight" to state.contentSize.height,
                    "panBoundsLeft" to state.panBounds.left,
                    "panBoundsTop" to state.panBounds.top,
                    "panBoundsRight" to state.panBounds.right,
                    "panBoundsBottom" to state.panBounds.bottom,
                    "zoomRangeStart" to state.zoomRange.start,
                    "zoomRangeEnd" to state.zoomRange.endInclusive,
                    "initialZoom" to state.initialZoom,
                    "containerWidth" to state.containerSize.width,
                    "containerHeight" to state.containerSize.height,
                    "scale" to state.scale,
                    "offsetX" to state.offset.x,
                    "offsetY" to state.offset.y
                )
            },
            restore = { map ->
                ZoomState(
                    Size(map.getValue("contentWidth"), map.getValue("contentHeight")),
                    Rect(
                        map.getValue("panBoundsLeft"),
                        map.getValue("panBoundsTop"),
                        map.getValue("panBoundsRight"),
                        map.getValue("panBoundsBottom")
                    ),
                    map.getValue("zoomRangeStart")..map.getValue("zoomRangeEnd"),
                    map.getValue("initialZoom")
                ).apply {
                    val containerSize = Size(
                        map.getValue("containerWidth"),
                        map.getValue("containerHeight")
                    )
                    if (!containerSize.isEmpty()) {
                        updateContainerSize(containerSize)
                        snapTo(
                            map.getValue("scale"),
                            Offset(map.getValue("offsetX"), map.getValue("offsetY"))
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun rememberZoomState(
    contentSize: Size,
    panBounds: Rect,
    zoomRange: ClosedFloatingPointRange<Float> = 1f..2f,
    initialZoom: Float = 1f
) = rememberSaveable(contentSize, panBounds, zoomRange, initialZoom, saver = ZoomState.Saver) {
    ZoomState(contentSize, panBounds, zoomRange, initialZoom)
}

fun Modifier.zoomable(state: ZoomState, onClick: (Offset) -> Unit = {}): Modifier = this
    .onSizeChanged { state.updateContainerSize(it.toSize()) }
    .pointerInput(state) {
        val decay = splineBasedDecay<Offset>(this)
        var animationJob: Job = Job().apply { complete() }
        var clickJob = animationJob
        var pendingDown: PointerInputChange? = null
        coroutineScope{
            awaitEachGesture {
                // Take consumed events too: they may start a gesture that itself won't be consumed
                val down = awaitFirstDown(requireUnconsumed = false)
                animationJob.cancel() // New gesture stops residual animation of previous one

                val doubleClick = viewConfiguration.isDoubleClick(pendingDown, down)
                pendingDown = null

                if (doubleClick) {
                    clickJob.cancel() // That click was a part of this double-click
                    if (!tryHandleDoubleClickAndDrag(state, down)) {
                        animationJob = this@coroutineScope.launch {
                            performDoubleClickToZoom(state, down.position)
                        }
                    }
                } else {
                    val panVelocity = tryHandlePanAndZoom(state)
                    if (panVelocity != null) {
                        animationJob = this@coroutineScope.launch {
                            performFling(state, panVelocity, decay)
                        }
                    } else {
                        val downPositionInContent = state.containerToContent(down.position)
                        pendingDown = down
                        clickJob = this@coroutineScope.launch {
                            // To be canceled if another down event arrives before the timeout
                            delay(viewConfiguration.doubleTapTimeoutMillis.milliseconds)
                            onClick(downPositionInContent)
                        }
                    }
                }
            }
        }
    }

private fun ViewConfiguration.isDoubleClick(
    prevDown: PointerInputChange?,
    currDown: PointerInputChange
) = prevDown != null &&
        currDown.uptimeMillis - prevDown.uptimeMillis <= doubleTapTimeoutMillis &&
        (currDown.position - prevDown.position).getDistance() <= 3 * touchSlop

/**
 * Handles double-click-and-drag and double-click-to-zoom.
 */
private suspend fun AwaitPointerEventScope.tryHandleDoubleClickAndDrag(
    state: ZoomState,
    down: PointerInputChange
): Boolean {
    val touchSlop = viewConfiguration.touchSlop
    var passedTouchSlop = false
    var dyTotal = 0f // Only accumulated until passedTouchSlop becomes true
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) {
            throw GestureCancellationException("Gesture consumed by someone else")
        }

        val change = event.changes.find { it.id == down.id } ?: break
        if (!change.pressed) {
            break
        }

        val dy = change.positionChange().y
        if (!passedTouchSlop) {
            dyTotal += dy
            passedTouchSlop = abs(dyTotal) > touchSlop
        }
        if (passedTouchSlop && dy != 0f) {
            change.consume()
            state.zoomTo(state.scale * exp(dy.toDp().value * QUICK_ZOOM_SENSITIVITY), down.position)
        }
    }
    return passedTouchSlop
}

private suspend fun performDoubleClickToZoom(state: ZoomState, offset: Offset) {
    val startScale = state.scale
    if (startScale < state.maxScale - 0.01f) {
        // Zoom in
        val targetScale = (2 * startScale).coerceAtMost(state.maxScale)
        state.animateZoomTo(targetScale, offset)
    } else {
        // Loop back to the default
        val initialScale = state.baseScale * state.initialZoom
        state.animateTo(
            initialScale,
            Offset(
                (state.containerSize.width - state.contentSize.width * initialScale) / 2,
                (state.containerSize.height - state.contentSize.height * initialScale) / 2
            )
        )
    }
}

/**
 * Handles panning and pinch-to-zoom.
 */
private suspend fun AwaitPointerEventScope.tryHandlePanAndZoom(state: ZoomState): Velocity? {
    val touchSlop = viewConfiguration.touchSlop
    var passedTouchSlop = false
    var zoomMotionTotal = 0f // Only accumulated until passedTouchSlop becomes true
    var panTotal = Offset.Zero
    val panTracker = VelocityTracker()
    var panTrackerHasData = false
    do {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) {
            if (!passedTouchSlop) {
                // Consumed before we started reacting to it
                throw GestureCancellationException("Gesture consumed by someone else")
            }
            break
        }

        val zoomChange = event.calculateZoom()
        val panChange = event.calculatePan()
        panTotal += panChange
        if (!passedTouchSlop) {
            zoomMotionTotal += (zoomChange - 1) * event.calculateCentroidSize(useCurrent = false)
            passedTouchSlop = abs(zoomMotionTotal) > touchSlop || panTotal.getDistance() > touchSlop
        }
        if (passedTouchSlop) {
            if (zoomChange != 1f) {
                state.zoomTo(
                    state.scale * zoomChange,
                    event.calculateCentroid(useCurrent = false).takeIf { it.isSpecified }
                        ?: Offset.Zero
                )
            }
            if (panChange != Offset.Zero) {
                state.snapTo(state.scale, state.offset + panChange)
            }

            for (change in event.changes) {
                if (change.positionChanged()) {
                    change.consume()
                }
            }

            // Filter events where a pointer changed pressed state to avoid sudden jumps
            if (event.changes.all { it.pressed == it.previousPressed }) {
                panTracker.addPosition(event.changes.first().uptimeMillis, panTotal)
                panTrackerHasData = true
            }
        }
    } while (event.changes.any { it.pressed })
    return when {
        passedTouchSlop && panTrackerHasData -> panTracker.calculateVelocity()
        passedTouchSlop -> Velocity.Zero
        else -> null
    }
}

private suspend fun performFling(
    state: ZoomState,
    velocity: Velocity,
    animationSpec: DecayAnimationSpec<Offset>
) {
    var prevValue = Offset.Zero
    AnimationState(
        typeConverter = Offset.VectorConverter,
        initialValue = Offset.Zero,
        initialVelocity = Offset(velocity.x, velocity.y)
    ).animateDecay(animationSpec) {
        val delta = value - prevValue
        prevValue = value

        val requestedOffset = state.offset + delta
        state.snapTo(state.scale, requestedOffset)
    }
}
