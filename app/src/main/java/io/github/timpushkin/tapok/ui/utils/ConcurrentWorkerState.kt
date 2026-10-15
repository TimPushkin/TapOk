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

package io.github.timpushkin.tapok.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class ConcurrentWorkerState {
    IDLE,
    BUSY,
    FINISHED_RECENTLY
}

@Composable
fun rememberConcurrentWorkerState(
    working: Boolean,
    finishedStateDuration: Duration = 1.seconds
): ConcurrentWorkerState {
    var state by remember {
        mutableStateOf(if (working) ConcurrentWorkerState.BUSY else ConcurrentWorkerState.IDLE)
    }
    LaunchedEffect(working) {
        if (working) {
            state = ConcurrentWorkerState.BUSY
        } else if (state != ConcurrentWorkerState.IDLE) {
            state = ConcurrentWorkerState.FINISHED_RECENTLY
            delay(finishedStateDuration)
            state = ConcurrentWorkerState.IDLE
        }
    }
    return state
}
