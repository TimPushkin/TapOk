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

package io.github.timpushkin.tapok.data.impl

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.timpushkin.tapok.data.impl.database.AppDatabase
import io.github.timpushkin.tapok.data.impl.database.RuleDao
import io.github.timpushkin.tapok.data.impl.database.ScanDao
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.data.rules.RuleRepo
import io.github.timpushkin.tapok.data.scans.ScanRepo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindRuleRepo(impl: RuleRepoImpl): RuleRepo

    @Binds
    abstract fun bindScanRepo(impl: ScanRepoImpl): ScanRepo

    @Binds
    abstract fun bindPreferencesRepo(impl: PreferencesRepoImpl): PreferencesRepo

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
            context, AppDatabase::class.java, "tapok.db"
        ).build()

        @Provides
        @Singleton
        fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

        @Provides
        @Singleton
        fun provideScanDao(db: AppDatabase): ScanDao = db.scanDao()

        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context) =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("preferences") },
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
            )
    }
}
