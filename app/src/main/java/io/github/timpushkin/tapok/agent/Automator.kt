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
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.getSystemService
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
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

    private val activeRulesFlow = ruleRepo.observeAllRules()
        .map { rules -> rules.filter { it.isActive } }
        .flowOn(Dispatchers.IO)
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())
    val selectedPackagesFlow = activeRulesFlow
        // Empty list would mean all packages are selected, so using an empty package instead
        .map { rules -> rules.mapTo(mutableSetOf()) { it.sourcePackageName }.ifEmpty { setOf("") } }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private var queuedEvents = Channel<AccessibilityEvent>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { event -> Log.v(TAG, "Dropped event ${event.id()}") }
    )

    init {
        coroutineScope.launch {
            run {
                Log.d(TAG, "Processing initial state")
                process(null)
                Log.d(TAG, "Processing initial state - done")
            }

            // TODO: dynamically adjust AccessibilityService's event timeout based on processing
            //  time
            for (event in queuedEvents) {
                Log.d(TAG, "Processing event ${event.id()}")
                process(event)
                Log.d(TAG, "Processing event ${event.id()} - done")
            }
        }
    }

    private fun process(event: AccessibilityEvent?) {
        val action = EventProcessor(
            event,
            activeRulesFlow.value,
            accessibilityService::getRootsOfAllWindows
        ).run()

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
            if (!node.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)) {
                Log.w(TAG, "Failed to click $node")
            }
        }
    }

    fun scheduleProcessing(event: AccessibilityEvent) {
        Log.v(TAG, "Received event ${event.id()}: $event")
        val queued = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            event
        } else {
            AccessibilityEvent(event) // OS may recycle the original by the time we get to it
        }
        queuedEvents.trySend(queued).exceptionOrNull()?.let { exception ->
            Log.e(TAG, "Failed to record event ${queued.id()}: $exception") // Should not happen
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    /**
     * Processes the [event] deciding what actions should be taken according to the [rules].
     */
    private class EventProcessor(
        /**
         * null means perform full search.
         */
        private val event: AccessibilityEvent?,
        private val rules: Iterable<Rule>,
        /**
         * Returns map of window's ID to its root.
         */
        getRootsOfAllWindows: () -> Map<Int, AccessibilityNodeInfoCompat>
    ) {
        private val windowRoots by lazy(LazyThreadSafetyMode.NONE, getRootsOfAllWindows)
        private val eventSource by lazy(LazyThreadSafetyMode.NONE) { event?.sourceCompat }

        fun run(): Action {
            val nodesToClick = mutableSetOf<AccessibilityNodeInfoCompat>()
            var muteDirection = Action.AudioChange.NONE
            for (rule in rules) {
                when (rule.action) {
                    Rule.Action.CLICK -> nodesToClick.addAll(findMatchingNodes(rule))

                    Rule.Action.CLICK_NEAREST_CLICKABLE_ANCESTOR -> nodesToClick.addAll(
                        findMatchingNodes(rule).mapNotNull { findNearestClickableAncestor(it) }
                    )

                    Rule.Action.CLICK_NEAREST_CLICKABLE_DESCENDANT -> nodesToClick.addAll(
                        findMatchingNodes(rule).mapNotNull { findNearestClickableDescendant(it) }
                    )

                    // No point in searching if we have already decided to mute
                    Rule.Action.MUTE_WHEN_PRESENT if muteDirection != Action.AudioChange.MUTE ->
                        muteDirection = if (findMatchingNodes(rule).isNotEmpty()) {
                            Action.AudioChange.MUTE
                        } else {
                            Action.AudioChange.UNMUTE
                        }

                    Rule.Action.MUTE_WHEN_PRESENT -> {}
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
        private fun findMatchingNodes(rule: Rule): List<AccessibilityNodeInfoCompat> {
            Log.d(TAG, "Looking for nodes matching $rule")

            if (event?.packageName == rule.sourcePackageName) {
                val source = eventSource
                if (source != null) {
                    Log.d(TAG, "Inspecting event's source")
                    val nodes = findMatchingNodes(rule, source)
                    Log.d(TAG, "Matching nodes: ${nodes.toShortString()}")
                    if (nodes.isNotEmpty()) {
                        return nodes
                    }
                } else {
                    Log.d(TAG, "Not inspecting event's source: not available")
                }
            } else {
                Log.d(
                    TAG,
                    "Not inspecting event's source: " +
                            if (event == null) "no event" else "package mismatch"
                )
            }

            val root = event?.run { windowRoots[windowId] }
            if (root != null) {
                Log.d(TAG, "Inspecting event's window")
                val nodes = findMatchingNodes(rule, root)
                Log.d(TAG, "Matching nodes: ${nodes.toShortString()}")
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            } else {
                Log.d(
                    TAG,
                    "Not inspecting event's window: " +
                            if (event == null) "no event" else "window not interactable"
                )
            }

            Log.d(TAG, "Inspecting other interactable windows")
            val nodes = findMatchingNodes(
                rule, root?.let { windowRoots.values - it } ?: windowRoots.values
            )
            Log.d(TAG, "Matching nodes: ${nodes.toShortString()}")
            return nodes
        }

        private fun findMatchingNodes(
            rule: Rule,
            source: AccessibilityNodeInfoCompat
        ): List<AccessibilityNodeInfoCompat> {
            Log.d(TAG, "Inspecting subtree of $source")
            if (source.packageName != rule.sourcePackageName) {
                Log.d(TAG, "Skipped: source's package name does not match")
                return emptyList()
            }
            if (source.matches(rule)) {
                Log.d(TAG, "Source matches, taking just it")
                return listOf(source)
            }
            if (rule.viewId != null) {
                val nodes = source.findAccessibilityNodeInfosByViewId(rule.viewId)
                    .also { Log.d(TAG, "Found by view ID (to be filtered): $it") }
                    .filter { it.matches(rule, checkViewId = false) }
                    .also { Log.d(TAG, "After filtering: ${it.toShortString()}") }
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            }
            if (rule.containedText != null) {
                val nodes = source.findAccessibilityNodeInfosByText(rule.containedText)
                    .also { Log.d(TAG, "Found by contained text (to be filtered): $it") }
                    .filter { it.matches(rule, checkText = false) }
                    .also { Log.d(TAG, "After filtering: ${it.toShortString()}") }
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            }
            return emptyList()
        }

        private fun findMatchingNodes(
            rule: Rule,
            sources: Iterable<AccessibilityNodeInfoCompat>
        ): List<AccessibilityNodeInfoCompat> {
            for (source in sources) {
                val nodes = findMatchingNodes(rule, source)
                if (nodes.isNotEmpty()) {
                    return nodes
                }
            }
            return emptyList()
        }

        private fun findNearestClickableAncestor(
            node: AccessibilityNodeInfoCompat
        ): AccessibilityNodeInfoCompat? {
            var parent = node.getParent(AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS)
            while (parent != null) {
                if (parent.isClickable) {
                    return parent
                }
                parent = parent.getParent(AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS)
            }
            return null
        }

        private fun findNearestClickableDescendant(
            node: AccessibilityNodeInfoCompat
        ): AccessibilityNodeInfoCompat? {
            val queue = ArrayDeque<AccessibilityNodeInfoCompat>().apply { addLast(node) }
            while (queue.isNotEmpty()) {
                val dequeued = queue.removeFirst()
                for (i in 0 until dequeued.childCount) {
                    val child = dequeued.getChild(
                        i,
                        AccessibilityNodeInfoCompat.FLAG_PREFETCH_SIBLINGS or
                                AccessibilityNodeInfoCompat.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST
                    ) ?: continue
                    if (child.isClickable) {
                        return child
                    } else {
                        queue.addLast(child)
                    }
                }
            }
            return null
        }
    }

    private data class Action(
        val nodesToClick: Set<AccessibilityNodeInfoCompat>,
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

private fun Iterable<AccessibilityNodeInfoCompat>.toShortString() =
    joinToString(prefix = "[", postfix = "]") { it.toShortString() }

private fun AccessibilityNodeInfoCompat.toShortString() =
    "${this::class.simpleName}@${Integer.toHexString(hashCode())}"

@AssistedFactory
interface AutomatorFactory {
    fun create(accessibilityService: AccessibilityService): Automator
}
