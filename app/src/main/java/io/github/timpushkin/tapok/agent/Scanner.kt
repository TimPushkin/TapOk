package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS
import android.accessibilityservice.AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.accessibilityservice.MagnificationConfig
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.Size
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import io.github.timpushkin.tapok.data.scans.screenshotFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

private const val TAG = "Scanner"

private const val PREFETCH_FLAGS = AccessibilityNodeInfo.FLAG_PREFETCH_UNINTERRUPTIBLE or
        AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST

class Scanner @AssistedInject constructor(
    @Assisted private val accessibilityService: AccessibilityService,
    private val scanRepo: ScanRepo,
    private val preferencesRepo: PreferencesRepo
) {
    val isScanningFlow = MutableStateFlow(false)

    suspend fun scan() {
        if (!isScanningFlow.compareAndSet(expect = false, update = true)) {
            return
        }

        withContext(Dispatchers.Default) {
            val shouldScanAllWindows = shouldScanAllWindows()
            val time = Clock.System.now()
            val roots = with(accessibilityService) {
                if (shouldScanAllWindows) {
                    Log.i(TAG, "Scanning all windows...")
                    getRootsOfAllWindows(PREFETCH_FLAGS).values
                } else {
                    Log.i(TAG, "Scanning active window...")
                    getRootInActiveWindow(PREFETCH_FLAGS)?.let { listOf(it) } ?: emptyList()
                }
            }
            for (root in roots) {
                launch {
                    val tree = async { captureAccessibilityState(root) }
                    val screenshot = async { captureScreenshot(root) }
                    withContext(Dispatchers.IO) {
                        saveScan(time, tree.await(), screenshot.await())
                    }
                    Log.i(TAG, "Scanned ${tree.await().nodes.size} nodes of ${root.packageName}")
                }
            }
        }

        isScanningFlow.value = false
    }

    suspend fun shouldScanAllWindows() =
        withContext(Dispatchers.IO) { preferencesRepo.shouldScanAllWindows().first() }

    private fun captureAccessibilityState(root: AccessibilityNodeInfo): AccessibilityState {
        val windowBounds = Rect()
        root.window.getBoundsInScreen(windowBounds)

        // Would be better to use getMagnificationController(root.window.displayId) but it is hidden
        val magnification = accessibilityService.magnificationController
            .magnificationConfig?.toMagnification() ?: Magnification(1f, Offset.Zero)

        val nodes = mutableListOf(root)
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { addLast(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i, PREFETCH_FLAGS)
                if (child != null) {
                    nodes.add(child)
                    queue.addLast(child)
                }
            }
        }

        return AccessibilityState(nodes, windowBounds, magnification)
    }

    private data class AccessibilityState(
        val nodes: List<AccessibilityNodeInfo>,
        val windowBounds: Rect,
        val magnification: Magnification
    )

    private data class Magnification(val scale: Float, val center: Offset)

    private fun MagnificationConfig.toMagnification() =
        Magnification(scale, Offset(centerX, centerY))

    private suspend fun captureScreenshot(root: AccessibilityNodeInfo): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            val executor = Dispatchers.Default.asExecutor()
            val callback = object : TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) {
                    Log.e(TAG, "Cannot take screenshot: ${screenshotErrorReason(errorCode)}")
                    continuation.resume(null)
                }

                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = screenshot.hardwareBuffer.use { buf ->
                        Bitmap.wrapHardwareBuffer(buf, screenshot.colorSpace)
                    }
                    if (bitmap == null) {
                        Log.e(TAG, "Cannot save screenshot: null from Bitmap.wrapHardwareBuffer")
                    }
                    continuation.resume(bitmap)
                }
            }
            accessibilityService.takeScreenshotOfWindow(root.windowId, executor, callback)
        }

    private fun screenshotErrorReason(code: Int) = when (code) {
        ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR -> "internal Android error"
        ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "no accessibility access"
        ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "too little time since previous screenshot"
        ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "invalid display ID"
        ERROR_TAKE_SCREENSHOT_INVALID_WINDOW -> "invalid window ID"
        ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> "window has secure content"
        else -> "unknown error (code $code)"
    }

    private suspend fun saveScan(
        timestamp: Instant,
        a11yState: AccessibilityState,
        screenshot: Bitmap?
    ) {
        val scan = Scan(0, a11yState.nodes.first().packageName?.toString(), timestamp).let {
            it.copy(id = scanRepo.insertScan(it))
        }

        val nodeOffset = screenshot?.run {
            calculateScreenToScreenshotOffset(a11yState.windowBounds, Size(width, height))
        } ?: IntOffset.Zero
        val nodeBounds = Rect()
        val nodes = a11yState.nodes.map { node ->
            node.calculateBoundsInScreenshot(nodeOffset, a11yState.magnification, nodeBounds)
            Node(
                0,
                scan.id,
                with(nodeBounds) { IntRect(left, top, right, bottom) },
                node.packageName?.toString(),
                node.className?.toString(),
                node.viewIdResourceName,
                node.contentDescription?.toString(),
                node.text?.toString(),
                node.isClickable,
                node.toString()
            )
        }
        scanRepo.insertNodes(nodes)

        screenshot?.run {
            accessibilityService.screenshotFile(scan).outputStream().use { out ->
                compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
            }
        }
    }

    private fun calculateScreenToScreenshotOffset(
        windowBounds: Rect,
        screenshotSize: Size
    ): IntOffset {
        val screenToWindow = IntOffset(
            x = -windowBounds.left,
            y = -windowBounds.top
        )
        // For some windows, at least for dialogs, screenshots contain transparent padding
        val windowToScreenshot = IntOffset(
            x = (screenshotSize.width - windowBounds.width()) / 2,
            y = (screenshotSize.height - windowBounds.height()) / 2
        )
        return screenToWindow + windowToScreenshot
    }

    private fun AccessibilityNodeInfo.calculateBoundsInScreenshot(
        screenToScreenshotOffset: IntOffset,
        magnification: Magnification,
        outBounds: Rect
    ) {
        // In some cases, e.g. Compose UIs, getBoundsInWindow reports zeroes even when the node is
        // not empty. getBoundsInScreen does not seem to have this issue so we use it.
        getBoundsInScreen(outBounds)
        // getBoundsInScreen is affected by magnification, so de-magnify
        if ((magnification.scale - 1f).absoluteValue > 1e-7f) {
            outBounds.apply {
                magnification.let { m ->
                    left = ((left - m.center.x) / m.scale + m.center.x).roundToInt()
                    top = ((top - m.center.y) / m.scale + m.center.y).roundToInt()
                    right = ((right - m.center.x) / m.scale + m.center.x).roundToInt()
                    bottom = ((bottom - m.center.y) / m.scale + m.center.y).roundToInt()
                }
            }
        }
        outBounds.offset(screenToScreenshotOffset.x, screenToScreenshotOffset.y)
    }
}

@AssistedFactory
interface ScannerFactory {
    fun create(accessibilityService: AccessibilityService): Scanner
}
