package com.github.timpushkin.tapok.ui.activity.home

import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import com.github.timpushkin.tapok.ui.activity.PrimaryKey
import com.github.timpushkin.tapok.ui.activity.SecondaryKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : PrimaryKey(
    index = 1,
    label = R.string.home,
    icon = R.drawable.home_outlined,
    iconSelected = R.drawable.home_filled
)

@Serializable
data object TutorialKey : SecondaryKey()

@Module
@InstallIn(ActivityRetainedComponent::class)
object HomeModule {
    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = HomeKey

    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { navigator ->
        entry<HomeKey> { HomePane(openTutorial = { navigator.navigateTo(TutorialKey) }) }
        entry<TutorialKey> { TutorialPane(onDone = navigator::navigateBack) }
    }
}
