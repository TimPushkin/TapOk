package io.github.timpushkin.tapok.data.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val IS_TUTORIAL_FINISHED_KEY = booleanPreferencesKey("is_tutorial_finished")
private const val IS_TUTORIAL_FINISHED_DEFAULT = false

private val SHOULD_SHOW_SCANNER_OVERLAY_KEY = booleanPreferencesKey("should_show_scanner_overlay")
private const val SHOULD_SHOW_SCANNER_OVERLAY_DEFAULT = true

class PreferencesRepoImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepo {
    private fun <T> getValueFlow(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { prefs -> prefs[key] ?: default }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    override fun isTutorialFinishedFlow() =
        getValueFlow(IS_TUTORIAL_FINISHED_KEY, IS_TUTORIAL_FINISHED_DEFAULT)

    override suspend fun markTutorialFinished() {
        setValue(IS_TUTORIAL_FINISHED_KEY, true)
    }

    override fun shouldShowScannerOverlay() =
        getValueFlow(SHOULD_SHOW_SCANNER_OVERLAY_KEY, SHOULD_SHOW_SCANNER_OVERLAY_DEFAULT)

    override suspend fun setShouldShowScannerOverlay(value: Boolean) {
        setValue(SHOULD_SHOW_SCANNER_OVERLAY_KEY, value)
    }
}
