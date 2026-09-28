package io.github.timpushkin.tapok.data.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepo {
    fun isTutorialFinishedFlow(): Flow<Boolean>
    suspend fun markTutorialFinished()

    fun isAccessibilityUsageAccepted(): Flow<Boolean>
    suspend fun markAccessibilityUsageAccepted()

    fun shouldShowScannerOverlay(): Flow<Boolean>
    suspend fun setShouldShowScannerOverlay(value: Boolean)

    fun scanShortcut(): Flow<Shortcut?>
    suspend fun setScanShortcut(value: Shortcut?)

    fun shouldScanAllWindows(): Flow<Boolean>
    suspend fun setShouldScanAllWindows(value: Boolean)
}
