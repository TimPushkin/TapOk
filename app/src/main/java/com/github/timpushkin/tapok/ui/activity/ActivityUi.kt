package com.github.timpushkin.tapok.ui.activity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.rules.RulesScreen
import com.github.timpushkin.tapok.ui.activity.scans.ScanInspector
import com.github.timpushkin.tapok.ui.activity.scans.ScanList
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ActivityUi() {
    val backStack = rememberNavBackStack(NavRoute.Help)
    val currentPrimary = backStack.findLast { it is NavRoute.Primary } as NavRoute.Primary
    Scaffold(
        topBar = {
            // TODO: when viewing a scan show the name of the scan
            CenterAlignedTopAppBar(title = { Text(stringResource(currentPrimary.titleRes)) })
        },
        bottomBar = {
            // TODO: in single-pane mode do not show the navigation bar when viewing a scan
            val currentPrimary = backStack.findLast { it is NavRoute.Primary }
            NavigationBar {
                for (primary in NavRoute.Primary.all) {
                    val title = stringResource(primary.titleRes)
                    val selected = primary == currentPrimary
                    NavigationBarItem(
                        selected = selected,
                        onClick = { backStack.addPrimary(primary) },
                        icon = {
                            Icon(
                                painterResource(primary.iconRes(selected)),
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
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
                entry<NavRoute.Scans>(metadata = ListDetailSceneStrategy.listPane()) {
                    ScanList(
                        onSelectScan = { scan ->
                            backStack.addSecondary(NavRoute.ScanInspector(scan.id))
                        },
                        onDeleteScan = { scan ->
                            backStack.removeAll {
                                it is NavRoute.ScanInspector && it.scanId == scan.id
                            }
                        }
                    )
                }
                entry<NavRoute.ScanInspector>(metadata = ListDetailSceneStrategy.detailPane()) {
                    ScanInspector(it.scanId)
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            sceneStrategy = rememberListDetailSceneStrategy()
        )
    }
}

private fun NavBackStack<NavKey>.addPrimary(primary: NavRoute.Primary) {
    val current = last()
    if (current == primary || (current is NavRoute.Secondary && current.parent == primary)) {
        return // Already in this primary
    }
    removeAll { it == primary || (it is NavRoute.Secondary && it.parent == primary) }
    add(primary)
}

private fun NavBackStack<NavKey>.addSecondary(secondary: NavRoute.Secondary) {
    when (val current = last()) {
        secondary.parent -> add(secondary)
        is NavRoute.Secondary if current.parent == secondary.parent -> set(lastIndex, secondary)
        else -> throw IllegalStateException("Cannot add $secondary after $current")
    }
}

private sealed interface NavRoute : NavKey {
    @Serializable
    sealed class Primary(
        @StringRes val titleRes: Int,
        @DrawableRes private val iconFilledRes: Int,
        @DrawableRes private val iconOutlinedRes: Int
    ) : NavRoute {
        companion object {
            // Without lazy, if an object from the list is accessed before Primary itself, that
            // object will be replaced by null in the list:
            // https://medium.com/livefront/kotlin-a-tale-of-static-cyclical-initialization-3aea530d2053
            val all by lazy { listOf<Primary>(Help, Rules, Scans) }
        }

        @DrawableRes
        fun iconRes(isSelected: Boolean): Int = if (isSelected) iconFilledRes else iconOutlinedRes
    }

    @Serializable
    sealed class Secondary(val parent: Primary) : NavRoute

    @Serializable
    data object Help :
        Primary(R.string.help, R.drawable.help_filled, R.drawable.help_outlined)

    @Serializable
    data object Rules :
        Primary(R.string.rules, R.drawable.settings_filled, R.drawable.settings_outlined)

    @Serializable
    data object Scans :
        Primary(R.string.scans, R.drawable.data_table_filled, R.drawable.data_table_outlined)

    @Serializable
    data class ScanInspector(val scanId: Long) : Secondary(Scans)
}
