package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.help.HelpScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object HelpKey : TapOkNavKey.Primary(
    order = 1,
    label = R.string.help,
    icon = R.drawable.help_outlined,
    iconSelected = R.drawable.help_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>) {
        HelpScreen()
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object HelpModule {
    @Provides
    @IntoSet
    fun provideNavKeyDescriptors(): TapOkNavKeyDescriptor =
        TapOkNavKeyDescriptor.OfInstance(HelpKey)
}
