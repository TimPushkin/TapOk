package com.github.timpushkin.tapok.ui.activity.help

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.TapOkNavKey
import com.github.timpushkin.tapok.ui.activity.TapOkNavKeyDescriptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

@Serializable
data object HelpKey : TapOkNavKey.Primary() {
    override val order = 1

    @Composable
    override fun Icon(selected: Boolean) {
        Icon(
            painterResource(if (selected) R.drawable.help_filled else R.drawable.help_outlined),
            contentDescription = stringResource(R.string.help)
        )
    }

    @Composable
    override fun Label() {
        Text(stringResource(R.string.help))
    }

    @Composable
    override fun Content(backStack: NavBackStack<NavKey>) {
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
