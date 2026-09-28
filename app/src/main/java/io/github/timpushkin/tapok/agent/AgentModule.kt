package io.github.timpushkin.tapok.agent

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.timpushkin.tapok.data.InMemoryRequestBroker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {
    @Provides
    @Singleton
    fun provideShortcutRequestBroker(): ShortcutRequestBroker = InMemoryRequestBroker()
}
