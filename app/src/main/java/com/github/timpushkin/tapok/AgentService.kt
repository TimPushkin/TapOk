package com.github.timpushkin.tapok

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleRepo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val TAG = "AgentService"

@SuppressLint("AccessibilityPolicy")
@AndroidEntryPoint
class AgentService : AccessibilityService() {
    private val scopeSupervisorJob = SupervisorJob()
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + scopeSupervisorJob)
    private val ioScope = CoroutineScope(Dispatchers.IO + scopeSupervisorJob)

    @Inject
    /* private */ lateinit var ruleRepo: RuleRepo
    private lateinit var rulesByPackage: StateFlow<Map<String, List<Rule>>>

    private lateinit var audioManager: AudioManager

    /**
     * Whether the audio has been muted by us.
     */
    private var muted = false

    override fun onCreate() {
        super.onCreate()
        rulesByPackage = ruleRepo.loadAllRulesByPackage().stateIn(
            ioScope, SharingStarted.Eagerly, emptyMap()
        )
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    override fun onServiceConnected() {
        // Start listening to updates of the selected packages set
        rulesByPackage
            .map { it.keys }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .onEach { serviceInfo = serviceInfo.apply { packageNames = it.toTypedArray() } }
            .launchIn(mainScope)
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
        if (shouldBeMuted != null && shouldBeMuted != muted) {
            // Ideally we should only unmute if the audio has been muted by us but since there is no
            // way to synchronize with others we just do what we want
            Log.i(TAG, "Applying muting: ${if (shouldBeMuted) "mute" else "unmute"}")
            toggleMute(shouldBeMuted)
        }
    }

    private fun toggleMute(mute: Boolean) {
        val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        muted = mute
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        if (muted) {
            toggleMute(false)
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        scopeSupervisorJob.cancel()
        super.onDestroy()
    }
}
