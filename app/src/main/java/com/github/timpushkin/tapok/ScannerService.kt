package com.github.timpushkin.tapok

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.timpushkin.tapok.data.scans.Node
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.data.scans.ScanRepo
import com.github.timpushkin.tapok.data.scans.screenshotFile
import com.github.timpushkin.tapok.ui.ScannerPopup
import com.github.timpushkin.tapok.ui.theme.TapOkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Instant

private const val TAG = "ScannerService"

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class ScannerService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle = lifecycleDispatcher.lifecycle

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Inject
    /* private */ lateinit var scanRepo: ScanRepo

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleDispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        // Should be called in onBind but that is final in AccessibilityService
        lifecycleDispatcher.onServicePreSuperOnBind()

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setContent {
            TapOkTheme {
                ScannerPopup(
                    onClick = ::scan, // TODO: make not clickable while in progress
                    onDrag = { delta ->
                        params.x += delta.x.toInt()
                        params.y += delta.y.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                )
            }
        }

        windowManager.addView(view, params)
    }

    private fun scan() {
        scope.launch {
            Log.i(TAG, "Scanning...")

            val root = rootInActiveWindow ?: run {
                Log.e(TAG, "Scan failed: cannot get accessibility root in active window")
                return@launch
            }

            val appName = root.packageName.toString()
            val timestamp = Clock.System.now()

            val nodes: List<AccessibilityNodeInfo>
            val screenshot: String?
            coroutineScope {
                nodes = captureAccessibilityNodes(root)
                screenshot = captureScreenshot(root, "${appName}-${timestamp}")
            }
            if (screenshot == null) {
                Log.e(TAG, "Scan failed: cannot take a screenshot")
                return@launch
            }

            withContext(Dispatchers.IO) { saveScan(appName, timestamp, nodes, screenshot) }

            Log.i(TAG, "Scanned ${nodes.size} nodes, screenshot saved as $screenshot")
        }
    }

    private fun captureAccessibilityNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf(root)
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { addLast(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    nodes.add(child)
                    queue.addLast(child)
                }
            }
        }
        return nodes
    }

    private suspend fun captureScreenshot(root: AccessibilityNodeInfo, name: String): String? {
        val bitmap: Bitmap? = suspendCancellableCoroutine { continuation ->
            val executor = Dispatchers.Default.asExecutor()
            val callback = object : TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) {
                    Log.e(TAG, "Failed to take a screenshot: error $errorCode")
                    continuation.resume(null)
                }

                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = screenshot.hardwareBuffer.use { buf ->
                        Bitmap.wrapHardwareBuffer(buf, screenshot.colorSpace)
                    }
                    continuation.resume(bitmap)
                }
            }
            takeScreenshotOfWindow(root.windowId, executor, callback)
        }
        if (bitmap == null) {
            return null
        }

        val filename = "$name.webp"
        withContext(Dispatchers.IO) {
            screenshotFile(filename).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
            }
        }
        return filename
    }

    private suspend fun saveScan(
        appName: String,
        timestamp: Instant,
        recordedNodes: List<AccessibilityNodeInfo>,
        screenshot: String
    ) {
        val scanId = scanRepo.insertScan(Scan(0, appName, timestamp, screenshot))

        val bounds = Rect()
        val nodes = List(recordedNodes.size) { i ->
            val nodeInfo = recordedNodes[i]
            nodeInfo.getBoundsInWindow(bounds)
            Node(
                0,
                scanId,
                nodeInfo.viewIdResourceName,
                nodeInfo.className.toString(),
                with(bounds) { Node.Bounds(left, top, right, bottom) },
                nodeInfo.isClickable
            )
        }
        scanRepo.insertNodes(nodes)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}