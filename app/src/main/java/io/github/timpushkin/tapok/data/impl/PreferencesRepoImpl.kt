package io.github.timpushkin.tapok.data.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.timpushkin.tapok.data.preferences.Shortcut
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val IS_TUTORIAL_FINISHED_KEY = booleanPreferencesKey("is_tutorial_finished")
private const val IS_TUTORIAL_FINISHED_DEFAULT = false

private val IS_ACCESSIBILITY_USAGE_ACCEPTED_KEY =
    booleanPreferencesKey("is_accessibility_usage_accepted")
private const val IS_ACCESSIBILITY_USAGE_ACCEPTED_DEFAULT = false

private val SHOULD_SHOW_SCANNER_OVERLAY_KEY = booleanPreferencesKey("should_show_scanner_overlay")
private const val SHOULD_SHOW_SCANNER_OVERLAY_DEFAULT = true

private val SCAN_SHORTCUT_KEY = stringPreferencesKey("scan_shortcut")
private val SCAN_SHORTCUT_DEFAULT: String? = null

private val SHOULD_SCAN_ALL_WINDOWS_KEY = booleanPreferencesKey("should_scan_all_windows")
private const val SHOULD_SCAN_ALL_WINDOWS_DEFAULT = false

class PreferencesRepoImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepo {
    private fun <T, D> getValueFlow(key: Preferences.Key<T>, default: D): Flow<D> where T : D =
        dataStore.data.map { prefs -> (prefs[key] ?: default) }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T?) {
        dataStore.edit { prefs ->
            if (value != null) {
                prefs[key] = value
            } else {
                prefs.remove(key)
            }
        }
    }

    override fun isTutorialFinishedFlow() =
        getValueFlow(IS_TUTORIAL_FINISHED_KEY, IS_TUTORIAL_FINISHED_DEFAULT)

    override suspend fun markTutorialFinished() {
        setValue(IS_TUTORIAL_FINISHED_KEY, true)
    }

    override fun isAccessibilityUsageAccepted() =
        getValueFlow(IS_ACCESSIBILITY_USAGE_ACCEPTED_KEY, IS_ACCESSIBILITY_USAGE_ACCEPTED_DEFAULT)

    override suspend fun markAccessibilityUsageAccepted() {
        setValue(IS_ACCESSIBILITY_USAGE_ACCEPTED_KEY, true)
    }

    override fun shouldShowScannerOverlay() =
        getValueFlow(SHOULD_SHOW_SCANNER_OVERLAY_KEY, SHOULD_SHOW_SCANNER_OVERLAY_DEFAULT)

    override suspend fun setShouldShowScannerOverlay(value: Boolean) {
        setValue(SHOULD_SHOW_SCANNER_OVERLAY_KEY, value)
    }

    override fun scanShortcut(): Flow<Shortcut?> =
        getValueFlow(SCAN_SHORTCUT_KEY, SCAN_SHORTCUT_DEFAULT)
            .map { it?.let(Json::decodeFromString) }

    override suspend fun setScanShortcut(value: Shortcut?) {
        setValue(SCAN_SHORTCUT_KEY, value?.let(Json::encodeToString))
    }

    override fun shouldScanAllWindows() =
        getValueFlow(SHOULD_SCAN_ALL_WINDOWS_KEY, SHOULD_SCAN_ALL_WINDOWS_DEFAULT)

    override suspend fun setShouldScanAllWindows(value: Boolean) {
        setValue(SHOULD_SCAN_ALL_WINDOWS_KEY, value)
    }
}
