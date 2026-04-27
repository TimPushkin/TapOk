package com.github.timpushkin.tapok.data.impl

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.github.timpushkin.tapok.data.impl.database.AppDatabase
import com.github.timpushkin.tapok.data.preferences.PreferencesRepo
import com.github.timpushkin.tapok.data.rules.RuleRepo
import com.github.timpushkin.tapok.data.scans.ScanRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
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
        fun providePreferencesDataStore(@ApplicationContext context: Context) =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("preferences") },
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
            )

        @Provides
        fun provideRuleRepo(db: AppDatabase): RuleRepo = db.ruleDao()

        @Provides
        fun provideScanRepo(db: AppDatabase): ScanRepo = db.scanDao()
    }
}