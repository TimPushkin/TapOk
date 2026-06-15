package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
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
import io.github.timpushkin.tapok.ui.ScannerPopup
import io.github.timpushkin.tapok.ui.activity.MainActivity
import io.github.timpushkin.tapok.ui.theme.TapOkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class AgentService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle = lifecycleDispatcher.lifecycle
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    @Inject
    lateinit var automatorFactory: AutomatorFactory
    private val automator by lazy { automatorFactory.create(this) }

    @Inject
    lateinit var scannerFactory: ScannerFactory
    private val scanner by lazy { scannerFactory.create(this) }

    @Inject
    lateinit var preferencesRepo: PreferencesRepo

    private lateinit var windowManager: WindowManager
    private var scannerView: View? = null

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        windowManager = checkNotNull(getSystemService())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleDispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        // Should be called in onBind but that is final in AccessibilityService
        lifecycleDispatcher.onServicePreSuperOnBind()

        preferencesRepo.shouldShowScannerOverlay()
            .onEach { shouldShow -> if (shouldShow) showScannerOverlay() else hideScannerOverlay() }
            .launchIn(lifecycle.coroutineScope)

        automator.selectedPackagesFlow
            .onEach { serviceInfo = serviceInfo.apply { packageNames = it.toTypedArray() } }
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
                    val scanning by scanner.isScanningFlow.collectAsStateWithLifecycle()
                    val scope = lifecycle.coroutineScope
                    ScannerPopup(
                        scanning = scanning,
                        onScan = { scope.launch(Dispatchers.Default) { scanner.scan() } },
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
        automator.scheduleProcessing(event)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        automator.close()
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
