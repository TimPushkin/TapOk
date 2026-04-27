package com.github.timpushkin.tapok.data.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepo {
    fun isTutorialFinishedFlow(): Flow<Boolean>

    suspend fun markTutorialFinished()
}
