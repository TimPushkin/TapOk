package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.home.HomePane
import com.github.timpushkin.tapok.ui.activity.home.TutorialPane
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : Pane.Primary(
    order = 1,
    label = R.string.home,
    icon = R.drawable.home_outlined,
    iconSelected = R.drawable.home_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<NavKey>) {
        HomePane(openTutorial = { backStack.addSecondary(TutorialKey) })
    }
}

@Serializable
data object TutorialKey : Pane.Secondary(parent = HomeKey) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<NavKey>) {
        TutorialPane(onDone = { backStack.remove(this) })
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object HomeModule {
    @Provides
    @ElementsIntoSet
    fun provideNavDescriptors(): Set<NavDescriptor<*>> = setOf(
        NavDescriptor.OfInstance(HomeKey),
        NavDescriptor.OfInstance(TutorialKey)
    )
}
