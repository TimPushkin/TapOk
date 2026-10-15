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

package io.github.timpushkin.tapok.ui.activity

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.timpushkin.tapok.ui.activity.components.PaneExpansionDragHandle
import io.github.timpushkin.tapok.ui.activity.components.rememberAnchoredPaneExpansionState

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun MainUi(navigator: Navigator, entryProviderInstallers: Iterable<EntryProviderInstaller>) {
    val navState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(navigator.currentKey) {
        val currentKey = navigator.currentKey
        val targetScaffoldValue = if (currentKey is SecondaryKey && currentKey.fullScreen) {
            NavigationSuiteScaffoldValue.Hidden
        } else {
            NavigationSuiteScaffoldValue.Visible
        }
        if (navState.targetValue != targetScaffoldValue) {
            navState.toggle()
        }
    }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()

    NavigationSuiteScaffold(
        navigationItems = {
            for (primary in navigator.primaries.sortedBy { it.index }) {
                val selected = primary == navigator.currentPrimary
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { navigator.navigateTo(primary) },
                    icon = {
                        Icon(
                            painterResource(if (selected) primary.iconSelected else primary.icon),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(primary.label)) }
                )
            }
        },
        navigationSuiteType = navigationSuiteType(navState, adaptiveInfo),
        state = navState
    ) {
        val directive = calculatePaneScaffoldDirective(adaptiveInfo)
        NavDisplay(
            entries = navigator.toDecoratedEntries(
                entryProvider = entryProvider {
                    for (installer in entryProviderInstallers) {
                        installer(directive, navigator)
                    }
                },
                entryDecoratorsProvider = {
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                        rememberResultEventBusNavEntryDecorator()
                    )
                }
            ),
            onBack = navigator::navigateBack,
            sceneStrategies = listOf(
                rememberListDetailSceneStrategy(
                    directive = directive,
                    paneExpansionDragHandle = { state -> PaneExpansionDragHandle(state) },
                    paneExpansionState = rememberAnchoredPaneExpansionState(directive)
                )
            )
        )
    }
}

private fun navigationSuiteType(
    state: NavigationSuiteScaffoldState,
    adaptiveInfo: WindowAdaptiveInfo
): NavigationSuiteType {
    val invisible = state.currentValue == state.targetValue &&
            state.currentValue == NavigationSuiteScaffoldValue.Hidden
    return if (invisible) {
        // Ensures the navigation is not focusable while it is not visible
        NavigationSuiteType.None
    } else {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    }
}
