package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.info.TutorialPane
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object InfoKey : Pane.Primary(
    order = 1,
    label = R.string.info,
    icon = R.drawable.info_outlined,
    iconSelected = R.drawable.info_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<NavKey>) {
        TutorialPane(onDone = { backStack.remove(this) }, directive)
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object InfoModule {
    @Provides
    @IntoSet
    fun provideNavDescriptors(): NavDescriptor<*> = NavDescriptor.OfInstance(InfoKey)
}
