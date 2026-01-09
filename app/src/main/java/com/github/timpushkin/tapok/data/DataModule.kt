package com.github.timpushkin.tapok.data

import android.content.Context
import androidx.room.Room
import com.github.timpushkin.tapok.data.rules.RuleRepo
import com.github.timpushkin.tapok.data.scans.ScanRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context, AppDatabase::class.java, "tapok.db"
    ).build()

    @Provides
    fun provideRuleRepo(db: AppDatabase): RuleRepo = db.ruleDao()

    @Provides
    fun provideScanRepo(db: AppDatabase): ScanRepo = db.scanDao()
}
