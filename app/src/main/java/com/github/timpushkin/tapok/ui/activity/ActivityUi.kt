package com.github.timpushkin.tapok.ui.activity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.rules.RulesScreen
import com.github.timpushkin.tapok.ui.activity.scans.ScansScreen
import kotlinx.serialization.Serializable

// TODO: organize navigation to scan viewer, https://developer.android.com/guide/navigation/navigation-3/custom-layouts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityUi() {
    val backStack = rememberNavBackStack(NavRoute.Help)
    val currentDestination = backStack.last() // nav3 requires backstack to never be empty
    Scaffold(
        topBar = {
            if (currentDestination is NavRoute.TopLevel) {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
            }
        },
        bottomBar = {
            if (currentDestination is NavRoute.TopLevel) {
                NavigationBar {
                    for (screen in NavRoute.TopLevel.all) {
                        val screenTitle = stringResource(screen.titleRes)
                        val selected = screen == currentDestination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (currentDestination != screen) backStack.add(screen) },
                            icon = {
                                Icon(
                                    painterResource(screen.iconRes(selected)),
                                    contentDescription = screenTitle
                                )
                            },
                            label = { Text(screenTitle) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(paddingValues),
            entryProvider = entryProvider {
                entry<NavRoute.Help> { HelpScreen() }
                entry<NavRoute.Rules> { RulesScreen() }
                entry<NavRoute.Scans> { ScansScreen() }
                entry<NavRoute.ScanViewer> { TODO() /* ScanViewer() */ }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
        )
    }
}

sealed interface NavRoute : NavKey {
    @Serializable
    sealed class TopLevel(
        @StringRes val titleRes: Int,
        @DrawableRes private val iconFilledRes: Int,
        @DrawableRes private val iconOutlinedRes: Int
    ) : NavRoute {
        companion object {
            // Without lazy, if an object from the list is accessed before TopLevel itself, that
            // object will be replaced by null in the list:
            // https://medium.com/livefront/kotlin-a-tale-of-static-cyclical-initialization-3aea530d2053
            val all by lazy { listOf<TopLevel>(Help, Rules, Scans) }
        }

        @DrawableRes
        fun iconRes(isSelected: Boolean): Int = if (isSelected) iconFilledRes else iconOutlinedRes
    }

    @Serializable
    data object Help :
        TopLevel(R.string.help, R.drawable.help_filled, R.drawable.help_outlined)

    @Serializable
    data object Rules :
        TopLevel(R.string.rules, R.drawable.settings_filled, R.drawable.settings_outlined)

    @Serializable
    data object Scans :
        TopLevel(R.string.scans, R.drawable.data_table_filled, R.drawable.data_table_outlined)

    @Serializable
    data object ScanViewer : NavRoute
}
