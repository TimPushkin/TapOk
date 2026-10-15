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
    fun provideEntryProviderInstaller(): EntryProviderInstaller = {  _, navigator ->
        entry<SettingsKey> { SettingsPane(openTutorial = { navigator.navigateTo(TutorialKey) }) }
        entry<TutorialKey> { TutorialScreen(onDone = navigator::navigateBack) }
    }
}
