package io.github.timpushkin.tapok.agent

import android.view.KeyEvent
import io.github.timpushkin.tapok.data.KeyId

data class KeyPress(val key: KeyId, val holdMillis: Long)

class KeyTracker {
    // KeyId -> eventTime of its most recent first (repeatCount == 0) DOWN event that has not yet
    // had a matching UP event
    private val pendingDownTimes = mutableMapOf<KeyId, Long>()

    fun onKeyEvent(event: KeyEvent): KeyPress? = when (event.action) {
        KeyEvent.ACTION_DOWN -> {
            handleDown(event)
            null
        }

        KeyEvent.ACTION_UP -> handleUp(event)
        else -> null
    }

    private fun handleDown(event: KeyEvent) {
        // Ignore repeated DOWNs: they are sent while the key is being held (but not on all devices)
        if (event.repeatCount == 0) {
            pendingDownTimes[KeyId(event)] = event.eventTime
        }
    }

    private fun handleUp(event: KeyEvent): KeyPress? {
        val key = KeyId(event)
        val downTime = pendingDownTimes.remove(key) ?: return null // Ignore stray UPs

        if ((event.flags and KeyEvent.FLAG_CANCELED) != 0) {
            return null
        }

        val holdMillis = event.eventTime - downTime
        return KeyPress(key, holdMillis)
    }

    fun reset() {
        pendingDownTimes.clear()
    }
}
