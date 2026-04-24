package com.github.timpushkin.tapok.ui.activity

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.timpushkin.tapok.ui.activity.components.PaneExpansionDragHandle
import com.github.timpushkin.tapok.ui.activity.components.rememberAnchoredPaneExpansionState
import com.github.timpushkin.tapok.ui.activity.navigation.HomeKey
import com.github.timpushkin.tapok.ui.activity.navigation.NavDescriptor
import com.github.timpushkin.tapok.ui.activity.navigation.PaneKey
import com.github.timpushkin.tapok.ui.activity.navigation.addPrimary

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun ActivityUi(navDescriptors: Iterable<NavDescriptor<PaneKey>>) {
    val backStack = rememberNavBackStack<PaneKey>(HomeKey)

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)

    val targetNavValue = rememberCurrentNavScaffoldValue(backStack.last())
    val navState = rememberNavigationSuiteScaffoldState(targetNavValue)
    LaunchedEffect(targetNavValue) { if (navState.targetValue != targetNavValue) navState.toggle() }

    // TODO: maybe use https://developer.android.com/develop/ui/compose/animation/shared-elements
    NavigationSuiteScaffold(
        navigationItems = {
            val currentPrimary = backStack.filterIsInstance<PaneKey.Primary>().last()
            for (primary in navDescriptors.primaries().sortedBy { it.order }) {
                val selected = primary == currentPrimary
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { backStack.addPrimary(primary) },
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
        navigationSuiteColors = with(MaterialTheme.colorScheme) {
            NavigationSuiteDefaults.colors(
                shortNavigationBarContainerColor = surfaceContainerHighest,
                shortNavigationBarContentColor = contentColorFor(surfaceContainerHighest),
                navigationBarContainerColor = surfaceContainerHighest,
                navigationBarContentColor = contentColorFor(surfaceContainerHighest),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
        state = navState
    ) {
        val directive = calculatePaneScaffoldDirective(adaptiveInfo)
        NavDisplay(
            backStack = backStack as List<NavKey>,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                for (descriptor in navDescriptors) {
                    addDescribedEntry(descriptor, directive, backStack)
                }
            },
            sceneStrategies = listOf(
                rememberListDetailSceneStrategy(
                    directive = directive,
                    paneExpansionDragHandle = { state -> PaneExpansionDragHandle(state) },
                    paneExpansionState = rememberAnchoredPaneExpansionState(directive)
                )
            ),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
        )
    }
}

@Composable
@Suppress("UNCHECKED_CAST")
private fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> =
    rememberNavBackStack(*elements) as NavBackStack<T>

@Composable
private fun rememberCurrentNavScaffoldValue(currentKey: PaneKey) = remember(currentKey) {
    if (currentKey is PaneKey.Secondary && currentKey.fullScreen) {
        NavigationSuiteScaffoldValue.Hidden
    } else {
        NavigationSuiteScaffoldValue.Visible
    }
}

private fun Iterable<NavDescriptor<*>>.primaries(): List<PaneKey.Primary> = mapNotNull {
    if (it is NavDescriptor.OfInstance && it.key is PaneKey.Primary) it.key else null
}

private fun EntryProviderScope<NavKey>.addDescribedEntry(
    descriptor: NavDescriptor<PaneKey>,
    directive: PaneScaffoldDirective,
    backStack: MutableList<PaneKey>
) {
    when (descriptor) {
        is NavDescriptor.OfInstance ->
            entry(descriptor.key, metadata = descriptor.metadata) {
                it.Content(directive, backStack)
            }

        is NavDescriptor.OfClass ->
            addEntryProvider(descriptor.cls, metadata = descriptor.metadata) {
                it.Content(directive, backStack)
            }
    }
}
