package io.github.timpushkin.tapok.data.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val IS_TUTORIAL_FINISHED = booleanPreferencesKey("is_tutorial_finished")

class PreferencesRepoImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepo {
    override fun isTutorialFinishedFlow() = dataStore.data.map { prefs ->
        prefs[IS_TUTORIAL_FINISHED] ?: false
    }

    override suspend fun markTutorialFinished() {
        dataStore.edit { prefs ->
            prefs[IS_TUTORIAL_FINISHED] = true
        }
    }
}
