package io.github.timpushkin.tapok.ui.activity.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import io.github.timpushkin.tapok.ui.activity.PrimaryKey
import io.github.timpushkin.tapok.ui.activity.SecondaryKey
import kotlinx.serialization.Serializable

@Serializable
data object SettingsKey : PrimaryKey(
    index = 1,
    label = R.string.settings,
    icon = R.drawable.settings_outlined,
    iconSelected = R.drawable.settings_filled
)

@Serializable
data object TutorialKey : SecondaryKey(fullScreen = true)

@Module
@InstallIn(ActivityRetainedComponent::class)
object SettingsModule {
    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = SettingsKey

    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { navigator ->
        entry<SettingsKey> { SettingsPane(openTutorial = { navigator.navigateTo(TutorialKey) }) }
        entry<TutorialKey> { TutorialScreen(onDone = navigator::navigateBack) }
    }
}
