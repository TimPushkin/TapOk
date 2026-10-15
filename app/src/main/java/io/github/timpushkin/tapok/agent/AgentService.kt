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

package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.preferences.Shortcut
import io.github.timpushkin.tapok.ui.ScannerPopup
import io.github.timpushkin.tapok.ui.activity.MainActivity
import io.github.timpushkin.tapok.ui.theme.TapOkTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val TAG = "AgentService"

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

    private val windowManager: WindowManager by lazy { checkNotNull(getSystemService()) }
    private val viewConfiguration: ViewConfiguration by lazy { ViewConfiguration.get(this) }
    private var scannerView: View? = null

    @Inject
    lateinit var scanShortcutRequester: ShortcutRequestBroker
    private val keyTrackingState = MutableStateFlow<KeyTrackingState?>(null)

    // Key tracking must be reset each time a new shortcut or request for it is sent to ensure
    // in-progress gestures are not accepted
    private data class KeyTrackingState(
        val shortcut: Shortcut?,
        val shortcutRequest: CompletableDeferred<Shortcut>?,
        val tracker: KeyTracker = KeyTracker()
    )

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

        preferencesRepo.shouldShowScannerOverlay()
            .onEach { shouldShow -> if (shouldShow) showScannerOverlay() else hideScannerOverlay() }
            .launchIn(lifecycleScope)

        preferencesRepo.scanShortcut()
            .onEach { shortcut ->
                keyTrackingState.update { keyTrackingStateIfNeeded(shortcut, it?.shortcutRequest) }
            }
            .launchIn(lifecycleScope)
        lifecycleScope.launch {
            scanShortcutRequester.serve {
                val request = CompletableDeferred<Shortcut>()
                keyTrackingState.update { keyTrackingStateIfNeeded(it?.shortcut, request) }
                try {
                    request.await()
                } finally {
                    keyTrackingState.update { keyTrackingStateIfNeeded(it?.shortcut, null) }
                }
            }
        }
        keyTrackingState
            .onEach { setupKeyTracking(it != null) }
            .launchIn(lifecycleScope)

        automator.selectedPackagesFlow
            .onEach { serviceInfo = serviceInfo.apply { packageNames = it.toTypedArray() } }
            .launchIn(lifecycleScope)
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
        ).apply {
            verticalMargin = 0.3f
        }
        scannerView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AgentService)
            setViewTreeSavedStateRegistryOwner(this@AgentService)
            setContent {
                TapOkTheme {
                    val scanning by scanner.isScanningFlow.collectAsStateWithLifecycle()
                    val keyTrackingState by keyTrackingState.collectAsStateWithLifecycle()
                    ScannerPopup(
                        scanning = scanning,
                        shortcut = keyTrackingState?.shortcut,
                        onScan = ::launchScan,
                        onViewScans = ::showScans,
                        onHide = {
                            lifecycleScope.launch {
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

    private fun launchScan() {
        lifecycleScope.launch(Dispatchers.Default) { scanner.scan() }
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

    private fun hideScannerOverlay() {
        if (scannerView != null) {
            windowManager.removeView(scannerView)
            scannerView = null
        }
    }

    private fun keyTrackingStateIfNeeded(
        shortcut: Shortcut?,
        request: CompletableDeferred<Shortcut>?
    ) = if (shortcut != null || request != null) KeyTrackingState(shortcut, request) else null

    private fun setupKeyTracking(enable: Boolean) {
        val info = serviceInfo ?: AccessibilityServiceInfo()
        val newFlags = if (enable) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        }
        if (info.flags == newFlags) {
            return
        }
        serviceInfo = info.apply { flags = newFlags }
        Log.i(TAG, "Key tracking ${if (enable) "enabled" else "disabled"}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        automator.scheduleProcessing(event)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        processKeyEvent(event)
        return false // Never consume the event
    }

    private fun processKeyEvent(event: KeyEvent) {
        val state = keyTrackingState.value ?: return
        val shortcut = state.tracker.onKeyEvent(event)?.run {
            Shortcut(key, isLongPress = holdMillis >= longPressTimeoutMillis())
        } ?: return
        if (state.shortcutRequest?.complete(shortcut) == true) {
            return
        }
        if (state.shortcut?.matches(shortcut) == true) {
            launchScan()
        }
    }

    private fun longPressTimeoutMillis(): Long =
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            viewConfiguration.longPressTimeoutMillis
        } else {
            ViewConfiguration.getLongPressTimeout()
        }.toLong()

    override fun onInterrupt() {}

    override fun onDestroy() {
        automator.close()
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }
}
