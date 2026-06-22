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
