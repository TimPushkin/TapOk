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
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Log
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Instant

private const val TAG = "Scanner"

private const val PREFETCH_FLAGS = AccessibilityNodeInfoCompat.FLAG_PREFETCH_UNINTERRUPTIBLE or
        AccessibilityNodeInfoCompat.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST

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
                    getRootInActiveWindowCompat(PREFETCH_FLAGS)?.let { listOf(it) } ?: emptyList()
                }
            }
            // TODO: on API levels before 34 we take whole-display screenshot for every window on
            //  the display which is redundant and is likely to hit screenshot rate limit.
            for (root in roots) {
                launch {
                    val magnification = accessibilityService.magnification
                    val nodes = async { captureNodes(root) }
                    val screenshot = async { captureScreenshot(root, magnification) }
                    withContext(Dispatchers.IO) {
                        saveScan(time, nodes.await(), screenshot.await(), magnification)
                    }
                    Log.i(TAG, "Scanned ${nodes.await().size} nodes of ${root.packageName}")
                }
            }
        }

        isScanningFlow.value = false
    }

    suspend fun shouldScanAllWindows() =
        withContext(Dispatchers.IO) { preferencesRepo.shouldScanAllWindows().first() }

    private fun captureNodes(root: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
        val nodes = mutableListOf(root)
        val queue = ArrayDeque<AccessibilityNodeInfoCompat>().apply { addLast(root) }
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
        return nodes
    }

    private suspend fun captureScreenshot(
        root: AccessibilityNodeInfoCompat,
        magnification: Magnification
    ): Screenshot? {
        val bitmap = takeScreenshot(root) ?: return null

        val windowBounds = Rect()
        root.window.getBoundsInScreen(windowBounds)

        val normalized = normalizeScreenshot(bitmap, windowBounds, magnification)
        if (normalized !== bitmap) {
            bitmap.recycle()
        }
        if (normalized == null) {
            return null
        }

        return Screenshot(
            bitmap = normalized,
            originOnScreen = IntOffset(windowBounds.left, windowBounds.top)
        )
    }

    private suspend fun takeScreenshot(root: AccessibilityNodeInfoCompat): Bitmap? =
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                accessibilityService.takeScreenshotOfWindow(root.windowId, executor, callback)
            } else {
                val window = checkNotNull(root.window.unwrap())
                val displayId = accessibilityService.getWindowDisplayIdCompat(window)
                if (displayId == null) {
                    Log.e(TAG, "Cannot take screenshot: could not determine display of $window")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                accessibilityService.takeScreenshot(displayId, executor, callback)
            }
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

    /**
     * Crops [bitmap] to [windowBounds] and brings it to the demagnified scale, so that its pixels
     * match screen coordinates.
     */
    private fun normalizeScreenshot(
        bitmap: Bitmap,
        windowBounds: Rect,
        magnification: Magnification
    ): Bitmap? {
        val bitmapBounds: Rect // Part of the screen shown by the bitmap, in screen coordinates
        val windowBoundsInBitmap: Rect // Window within the bitmap, in bitmap coordinates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // - Window screenshots are not affected by magnification
            // - At least for dialogs, screenshots contain transparent padding which we strip
            //   assuming the window is centered.
            val bitmapOrigin = IntOffset(
                x = windowBounds.left - (bitmap.width - windowBounds.width()) / 2,
                y = windowBounds.top - (bitmap.height - windowBounds.height()) / 2
            )
            bitmapBounds = with(bitmapOrigin) { Rect(x, y, x + bitmap.width, y + bitmap.height) }
            windowBoundsInBitmap = Rect(windowBounds).apply {
                offset(-bitmapOrigin.x, -bitmapOrigin.y)
            }
        } else {
            // - Display screenshots are affected by magnification
            // - Need to strip the parts of the screenshot around the window
            bitmapBounds = Rect(
                magnification.demagnifyX(0),
                magnification.demagnifyY(0),
                magnification.demagnifyX(bitmap.width),
                magnification.demagnifyY(bitmap.height)
            )
            windowBoundsInBitmap = Rect(
                magnification.magnifyX(windowBounds.left),
                magnification.magnifyY(windowBounds.top),
                magnification.magnifyX(windowBounds.right),
                magnification.magnifyY(windowBounds.bottom)
            )
        }

        if (!Rect(0, 0, bitmap.width, bitmap.height).contains(windowBoundsInBitmap)) {
            Log.e(
                TAG,
                "Not saving screenshot ${bitmapBounds.toShortString()}: it " +
                        "does not cover the whole window ${windowBounds.toShortString()}"
            )
            return null
        }

        val demagnify = Matrix().apply {
            setScale(
                windowBounds.width().toFloat() / windowBoundsInBitmap.width(),
                windowBounds.height().toFloat() / windowBoundsInBitmap.height()
            )
        }
        return with(windowBoundsInBitmap) {
            Bitmap.createBitmap(bitmap, left, top, width(), height(), demagnify, true)
        }
    }

    private class Screenshot(
        val bitmap: Bitmap,
        /**
         * Screen coordinates of the top-left corner of the screenshot.
         */
        val originOnScreen: IntOffset
    )

    private suspend fun saveScan(
        timestamp: Instant,
        nodes: List<AccessibilityNodeInfoCompat>,
        screenshot: Screenshot?,
        magnification: Magnification
    ) {
        val scan = Scan(0, nodes.first().packageName?.toString(), timestamp)

        val screenshotOrigin = screenshot?.originOnScreen ?: IntOffset.Zero
        val nodeBounds = Rect()
        val nodes = nodes.map { node ->
            node.getBoundsInScreenshot(screenshotOrigin, magnification, nodeBounds)
            Node(
                0,
                0,
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

        scanRepo.insertScan(scan, nodes, screenshot?.bitmap)
    }

    private fun AccessibilityNodeInfoCompat.getBoundsInScreenshot(
        screenshotOrigin: IntOffset,
        magnification: Magnification,
        outBounds: Rect
    ) {
        // In some cases, e.g. Compose UIs, getBoundsInWindow reports zeroes even when the node is
        // not empty. getBoundsInScreen does not seem to have this issue so we use it.
        getBoundsInScreen(outBounds)
        // getBoundsInScreen is affected by magnification, so de-magnify
        outBounds.set(
            magnification.demagnifyX(outBounds.left),
            magnification.demagnifyY(outBounds.top),
            magnification.demagnifyX(outBounds.right),
            magnification.demagnifyY(outBounds.bottom)
        )
        outBounds.offset(-screenshotOrigin.x, -screenshotOrigin.y)
    }
}

@AssistedFactory
interface ScannerFactory {
    fun create(accessibilityService: AccessibilityService): Scanner
}
