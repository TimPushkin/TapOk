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
