package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.getSystemService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

private const val TAG = "Automator"

class Automator @AssistedInject constructor(
    @Assisted private val accessibilityService: AccessibilityService,
    ruleRepo: RuleRepo
) : AutoCloseable {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val audioManager = checkNotNull(accessibilityService.getSystemService<AudioManager>())
    private var mutedAudio = false

    private val rulesByPackage = ruleRepo.loadAllRulesByPackage().flowOn(Dispatchers.IO).stateIn(
        coroutineScope, SharingStarted.Eagerly, emptyMap()
    )

    init {
        rulesByPackage
            .map { it.keys }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .onEach {
                accessibilityService.serviceInfo = accessibilityService.serviceInfo.apply {
                    packageNames = it.toTypedArray()
                }
            }
            .launchIn(coroutineScope)
    }

    fun process(event: AccessibilityEvent) {
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
        val root = accessibilityService.rootInActiveWindow
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

    override fun close() {
        if (mutedAudio) {
            toggleMute(false)
        }
        coroutineScope.cancel()
    }
}

@AssistedFactory
interface AutomatorFactory {
    fun create(accessibilityService: AccessibilityService): Automator
}
