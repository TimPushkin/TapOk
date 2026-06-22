package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.getSystemService
import androidx.core.util.valueIterator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import io.github.timpushkin.tapok.data.rules.matches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "Automator"

// TODO: this is optimized for positive notificationTimeout, it is possible to have a separate
//  implementation optimized for zero notificationTimeout by assuming no events are lost.
// TODO: support considering only the active window to reduce full-search scope.
class Automator @AssistedInject constructor(
    @Assisted private val accessibilityService: AccessibilityService,
    ruleRepo: RuleRepo
) : AutoCloseable {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val audioManager = checkNotNull(accessibilityService.getSystemService<AudioManager>())

    // Used by one coroutine without locking, add a lock if start using this concurrently
    private var lastAudioChange = Action.AudioChange.NONE

    private val rulesFlow = ruleRepo.loadAllRules().flowOn(Dispatchers.IO).stateIn(
        coroutineScope, SharingStarted.Eagerly, emptyList()
    )
    val selectedPackagesFlow = rulesFlow
        // Empty list would mean all packages are selected, so using an empty package instead
        .map { rules -> rules.mapTo(mutableSetOf()) { it.packageName }.ifEmpty { setOf("") } }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private var queuedEvents = Channel<AccessibilityEvent>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { event -> Log.v(TAG, "Dropped event ${event.id()}") }
    )

    init {
        coroutineScope.launch {
            // TODO: dynamically adjust AccessibilityService's event timeout based on processing
            //  time
            for (event in queuedEvents) {
                Log.d(TAG, "Processing event ${event.id()}")
                val decision = EventProcessor(
                    event,
                    rulesFlow.value,
                    accessibilityService::getRootsOfAllWindows
                ).run()
                execute(decision)
                Log.d(TAG, "Processing event ${event.id()} - done")
            }
        }

        // Process the current state
        scheduleProcessing(AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
    }

    fun scheduleProcessing(event: AccessibilityEvent) {
        Log.v(TAG, "Received event ${event.id()}: $event")
        queuedEvents.trySend(event).exceptionOrNull()?.let { exception ->
            Log.e(TAG, "Failed to record event ${event.id()}: $exception") // Should not happen
        }
    }

    private fun execute(action: Action) {
        if (
            action.audioChange != Action.AudioChange.NONE &&
            action.audioChange != lastAudioChange
        ) {
            Log.i(TAG, "Adjusting music stream: ${action.audioChange}")
            val direction = action.audioChange.adjustmentDirection
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
            lastAudioChange = action.audioChange
        }
        for (node in action.nodesToClick) {
            Log.i(TAG, "Clicking $node")
            if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.w(TAG, "Failed to click $node")
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    /**
     * Processes the [event] deciding what actions should be taken according to the [rules].
     */
    private class EventProcessor(
        private val event: AccessibilityEvent,
        private val rules: Iterable<Rule>,
        getRootsOfAllWindows: () -> Map<Int, AccessibilityNodeInfo> // Window's ID to its root
    ) {
        private val windowRoots by lazy(LazyThreadSafetyMode.NONE, getRootsOfAllWindows)
        private val eventSource by lazy(LazyThreadSafetyMode.NONE) { event.source }

        fun run(): Action {
            val nodesToClick = mutableSetOf<AccessibilityNodeInfo>()
            var muteDirection = Action.AudioChange.NONE
            for (rule in rules) {
                when (rule.action) {
                    Rule.Action.CLICK ->
                        nodesToClick.addAll(findMatchingNodes(rule).filter { it.isClickable })

                    // No point in searching if we have already decided to mute
                    Rule.Action.MUTE_WHEN_PRESENT if muteDirection != Action.AudioChange.MUTE ->
                        muteDirection = if (findMatchingNodes(rule).isNotEmpty()) {
                            Action.AudioChange.MUTE
                        } else {
                            Action.AudioChange.UNMUTE
                        }

                    else -> {}
                }
            }
            return Action(nodesToClick, muteDirection)
        }

        /**
         * Finds nodes that match the rule's description.
         *
         * There is no guarantee that a non-empty result contains all matches, some may be missed.
         * But if the result is empty then it is guaranteed that no matches were missed.
         *
         * This is acceptable for optimization's sake. It is a user's responsibility to ensure the
         * rule's node description is specific enough to not match more than one node. Otherwise, we
         * would have to either process each event without skipping or always search the whole
         * window - either worsens the performance.
         */
        private fun findMatchingNodes(rule: Rule): List<AccessibilityNodeInfo> {
            val source = eventSource
            if (source != null) {
                val nodes = findMatchingNodes(rule, listOf(source))
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            }

            val root = windowRoots[event.windowId]
            if (root != null) {
                val nodes = findMatchingNodes(rule, listOf(root))
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            }

            return findMatchingNodes(
                rule,
                root?.let { windowRoots.values - it } ?: windowRoots.values
            )
        }

        private fun findMatchingNodes(
            rule: Rule,
            sources: Iterable<AccessibilityNodeInfo>
        ): List<AccessibilityNodeInfo> {
            for (source in sources) {
                if (source.packageName != rule.packageName) {
                    continue
                }
                if (source.matches(rule, checkPackageName = false)) {
                    return listOf(source)
                }
                if (rule.viewId != null) {
                    val nodes = source.findAccessibilityNodeInfosByViewId(rule.viewId)
                        .filter { it.matches(rule, checkPackageName = false, checkViewId = false) }
                    if (nodes.isNotEmpty()) {
                        return nodes
                    }
                }
                if (rule.containedText != null) {
                    val nodes = source.findAccessibilityNodeInfosByText(rule.containedText)
                        .filter { it.matches(rule, checkPackageName = false, checkText = false) }
                    if (nodes.isNotEmpty()) {
                        return nodes
                    }
                }
            }
            return emptyList()
        }
    }

    private data class Action(
        val nodesToClick: Set<AccessibilityNodeInfo>,
        val audioChange: AudioChange
    ) {
        enum class AudioChange(val adjustmentDirection: Int) {
            NONE(AudioManager.ADJUST_SAME),
            MUTE(AudioManager.ADJUST_MUTE),
            UNMUTE(AudioManager.ADJUST_UNMUTE);
        }
    }
}

private fun AccessibilityEvent.id() = hashCode().toHexString()

private fun AccessibilityService.getRootsOfAllWindows(): Map<Int, AccessibilityNodeInfo> =
    windowsOnAllDisplays.valueIterator().asSequence()
        .flatten()
        .mapNotNull { window -> window.root?.let { root -> window.id to root } }
        .toMap()

@AssistedFactory
interface AutomatorFactory {
    fun create(accessibilityService: AccessibilityService): Automator
}
