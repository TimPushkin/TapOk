package io.github.timpushkin.tapok

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import io.github.timpushkin.tapok.data.scans.screenshotFile
import io.github.timpushkin.tapok.ui.ScannerPopup
import io.github.timpushkin.tapok.ui.theme.TapOkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

private const val TAG = "AgentService"

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class AgentService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle = lifecycleDispatcher.lifecycle
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    @Inject
    lateinit var ruleRepo: RuleRepo
    private lateinit var rulesByPackage: StateFlow<Map<String, List<Rule>>>

    @Inject
    lateinit var scanRepo: ScanRepo

    @Inject
    lateinit var preferencesRepo: PreferencesRepo

    private lateinit var audioManager: AudioManager
    private var mutedAudio = false

    private lateinit var windowManager: WindowManager
    private var scannerView: View? = null
    private var isScanningFlow = MutableStateFlow(false)

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        rulesByPackage = ruleRepo.loadAllRulesByPackage().flowOn(Dispatchers.IO).stateIn(
            lifecycle.coroutineScope, SharingStarted.Eagerly, emptyMap()
        )
        audioManager = checkNotNull(getSystemService())
        windowManager = checkNotNull(getSystemService())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleDispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        // Should be called in onBind but that is final in AccessibilityService
        lifecycleDispatcher.onServicePreSuperOnBind()

        rulesByPackage
            .map { it.keys }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .onEach { serviceInfo = serviceInfo.apply { packageNames = it.toTypedArray() } }
            .launchIn(lifecycle.coroutineScope)

        preferencesRepo.shouldShowScannerOverlay()
            .onEach { shouldShow -> if (shouldShow) showScannerOverlay() else hideScannerOverlay() }
            .launchIn(lifecycle.coroutineScope)
    }

    private fun showScannerOverlay() {
        if (scannerView != null) {
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        scannerView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AgentService)
            setViewTreeSavedStateRegistryOwner(this@AgentService)
            setContent {
                TapOkTheme {
                    val scanning by isScanningFlow.collectAsStateWithLifecycle()
                    val scope = lifecycle.coroutineScope
                    ScannerPopup(
                        scanning = scanning,
                        onScan = { scope.launch(Dispatchers.Default) { scan() } },
                        onViewScans = ::showScans,
                        onHide = {
                            scope.launch(Dispatchers.IO) {
                                preferencesRepo.setShouldShowScannerOverlay(false)
                            }
                        },
                        onDrag = { delta ->
                            params.x += delta.x.roundToInt()
                            params.y += delta.y.roundToInt()
                            windowManager.updateViewLayout(this, params)
                        }
                    )
                }
            }
        }
        windowManager.addView(scannerView, params)
    }

    private suspend fun scan() {
        if (!isScanningFlow.compareAndSet(expect = false, update = true)) {
            return
        }
        coroutineScope { // Need a coroutine scope for async/await
            Log.i(TAG, "Scanning...")
            val timestamp = Clock.System.now()
            val root = rootInActiveWindow
            val nodes = async(Dispatchers.Default) { captureAccessibilityNodes(root) }
            val screenshot = async(Dispatchers.Default) { captureScreenshot(root) }
            val appPackage = root.packageName.toString()
            withContext(Dispatchers.IO) {
                saveScan(appPackage, timestamp, nodes.await(), screenshot.await())
            }
            Log.i(TAG, "Scanned ${nodes.await().size} nodes of $appPackage at $timestamp")
        }
        isScanningFlow.value = false
    }

    private fun captureAccessibilityNodes(
        root: AccessibilityNodeInfo
    ): List<AccessibilityNodeInfo> {
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
            takeScreenshotOfWindow(root.windowId, executor, callback)
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
        appPackage: String,
        timestamp: Instant,
        recordedNodes: List<AccessibilityNodeInfo>,
        screenshot: Bitmap?
    ) {
        val screenshotFile = screenshot?.run {
            "$appPackage-$timestamp.webp".also { filename ->
                screenshotFile(filename).outputStream().use { out ->
                    compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
            }
        }

        val scanId = scanRepo.insertScan(Scan(0, appPackage, timestamp, screenshotFile))

        val offset = screenshot?.let { screenshot ->
            recordedNodes.firstOrNull()?.window?.let { window ->
                windowOffsetInScreenshot(window, screenshot)
            }
        } ?: IntOffset.Zero
        val bounds = Rect()
        val objectStringRegex = """^[\w.]+@\p{XDigit}+""".toRegex()
        val nodes = List(recordedNodes.size) { i ->
            val nodeInfo = recordedNodes[i]
            nodeInfo.getBoundsInWindow(bounds)
            bounds.offset(offset.x, offset.y)
            Node(
                0,
                scanId,
                nodeInfo.viewIdResourceName,
                nodeInfo.className.toString(),
                with(bounds) { IntRect(left, top, right, bottom) },
                nodeInfo.isClickable,
                nodeInfo.toString().replace(objectStringRegex, "").removePrefix("; ")
            )
        }
        scanRepo.insertNodes(nodes)
    }

    /**
     * For some windows, at least for dialogs, screenshots contain transparent padding. This method
     * calculates the offset of the window within the screenshot. The padding is not officially
     * documented so this is just a guess.
     */
    private fun windowOffsetInScreenshot(
        window: AccessibilityWindowInfo,
        screenshot: Bitmap
    ): IntOffset {
        val windowBounds = Rect()
        window.getBoundsInScreen(windowBounds)
        return IntOffset(
            x = (screenshot.width - windowBounds.width()).coerceAtLeast(0) / 2,
            y = (screenshot.height - windowBounds.height()).coerceAtLeast(0) / 2
        )
    }

    private fun hideScannerOverlay() {
        if (scannerView != null) {
            windowManager.removeView(scannerView)
            scannerView = null
        }
    }

    private fun showScans() {
        val uri = Uri.Builder()
            .scheme(MainActivity.DATA_SCHEME)
            .path(MainActivity.DATA_PATH_SCANS)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri, this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.v(TAG, "Received $event")
        require(event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            "Unexpected event type: ${event.eventType}"
        }
        if (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE == 0) {
            return
        }

        val applicableRules = rulesByPackage.value[event.packageName]
        if (applicableRules == null) {
            // Since rules can be changed concurrently this is probably possible
            Log.d(TAG, "Event's package ${event.packageName} is not among the monitored packages")
            return
        }

        var shouldBeMuted: Boolean? = null // null means there are no applicable muting rules
        val root = rootInActiveWindow
        val source = event.source ?: root ?: return
        for (rule in applicableRules) {
            when (rule.action) {
                Rule.Action.CLICK -> source.findAccessibilityNodeInfosByViewId(rule.viewId)
                    .filter { it.isClickable }
                    .forEach {
                        Log.i(TAG, "Applying ${rule.toShortString()}: click $it")
                        if (!it.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.w(TAG, "Failed to click $it")
                        }
                    }

                Rule.Action.MUTE_WHEN_PRESENT -> {
                    val matches = root.findAccessibilityNodeInfosByViewId(rule.viewId)
                    if (matches.isNotEmpty()) {
                        Log.d(TAG, "${rule.toShortString()} matches $matches")
                        shouldBeMuted = true
                    } else if (shouldBeMuted == null) {
                        shouldBeMuted = false
                    }
                }
            }
        }
        if (shouldBeMuted != null && shouldBeMuted != mutedAudio) {
            // Ideally we should only unmute if the audio has been muted by us but since there is no
            // way to synchronize with others we just do what we want
            Log.i(TAG, "Applying muting: ${if (shouldBeMuted) "mute" else "unmute"}")
            toggleMute(shouldBeMuted)
        }
    }

    private fun toggleMute(mute: Boolean) {
        val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        mutedAudio = mute
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (mutedAudio) {
            toggleMute(false)
        }
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
