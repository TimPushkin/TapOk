package io.github.timpushkin.tapok.ui.activity

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.timpushkin.tapok.ui.activity.components.PaneExpansionDragHandle
import io.github.timpushkin.tapok.ui.activity.components.rememberAnchoredPaneExpansionState

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun ActivityUi(navigator: Navigator, entryProviderInstallers: Iterable<EntryProviderInstaller>) {
    val targetNavValue = rememberCurrentNavScaffoldValue(navigator.currentKey)
    val navState = rememberNavigationSuiteScaffoldState(targetNavValue)
    LaunchedEffect(targetNavValue) { if (navState.targetValue != targetNavValue) navState.toggle() }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)

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
        navigationSuiteType = navType,
        state = navState
    ) {
        val directive = calculatePaneScaffoldDirective(adaptiveInfo)
        NavDisplay(
            entries = navigator.toDecoratedEntries(
                entryProvider = entryProvider {
                    for (installer in entryProviderInstallers) {
                        installer(navigator)
                    }
                },
                entryDecoratorsProvider = {
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
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

@Composable
private fun rememberCurrentNavScaffoldValue(currentKey: NavKey) = remember(currentKey) {
    if (currentKey is SecondaryKey && currentKey.fullScreen) {
        NavigationSuiteScaffoldValue.Hidden
    } else {
        NavigationSuiteScaffoldValue.Visible
    }
}
