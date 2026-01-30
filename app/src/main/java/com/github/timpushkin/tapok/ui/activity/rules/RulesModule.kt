package com.github.timpushkin.tapok.ui.activity.rules

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
data object RulesKey : TapOkNavKey.Primary() {
    override val order = 2

    @Composable
    override fun Icon(selected: Boolean) {
        Icon(
            painterResource(
                if (selected) R.drawable.settings_filled else R.drawable.settings_outlined
            ),
            contentDescription = stringResource(R.string.rules)
        )
    }

    @Composable
    override fun Label() {
        Text(stringResource(R.string.rules))
    }

    @Composable
    override fun Fab(backStack: NavBackStack<NavKey>) {
        // TODO: use the backstack to open add-rule pane
        AddRuleFab()
    }

    @Composable
    override fun Content(backStack: NavBackStack<NavKey>) {
        RulesScreen()
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object RulesModule {
    @Provides
    @IntoSet
    fun providePrimaryNavKey(): TapOkNavKeyDescriptor = TapOkNavKeyDescriptor.OfInstance(RulesKey)
}
