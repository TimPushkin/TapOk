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
