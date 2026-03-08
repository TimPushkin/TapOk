package com.github.timpushkin.tapok.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.timpushkin.tapok.ui.activity.navigation.HelpKey
import com.github.timpushkin.tapok.ui.activity.navigation.NavDescriptor
import com.github.timpushkin.tapok.ui.activity.navigation.Pane
import com.github.timpushkin.tapok.ui.activity.navigation.PaneScreenKey
import com.github.timpushkin.tapok.ui.activity.navigation.Screen
import com.github.timpushkin.tapok.ui.activity.navigation.addPaneEntry
import com.github.timpushkin.tapok.ui.activity.navigation.addPrimary
import com.github.timpushkin.tapok.ui.activity.navigation.addScreenEntry
import com.github.timpushkin.tapok.ui.activity.navigation.partitionByDescribedKeyType
import com.github.timpushkin.tapok.ui.utils.thenIf

@Composable
fun ActivityUi(navDescriptors: Iterable<NavDescriptor<*>>) {
    val backStack = rememberNavBackStack(PaneScreenKey, HelpKey)
    val screenBackStack by remember { derivedStateOf { backStack.filterIsInstance<Screen>() } }
    NavDisplay(
        backStack = screenBackStack as List<NavKey>,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            val (screenDescriptors, paneDescriptors) =
                navDescriptors.partitionByDescribedKeyType<Screen, Pane>()
            for (descriptor in screenDescriptors) addScreenEntry(descriptor, backStack)
            entry(PaneScreenKey) { PaneScreen(paneDescriptors, backStack) }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        )
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun PaneScreen(
    paneDescriptors: Iterable<NavDescriptor<Pane>>,
    backStack: MutableList<NavKey>
) {
    val navSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    // TODO: maybe use https://developer.android.com/develop/ui/compose/animation/shared-elements
    NavigationSuiteScaffold(
        navigationItems = {
            val currentPrimary = backStack.filterIsInstance<Pane.Primary>().last()
            for (primary in paneDescriptors.primaries().sortedBy { it.order }) {
                val selected = primary == currentPrimary
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { backStack.addPrimary(primary) },
                    icon = { PrimaryIcon(primary, selected) },
                    label = { PrimaryLabel(primary) }
                )
            }
        },
        navigationSuiteType = navSuiteType,
        navigationSuiteColors = with(MaterialTheme.colorScheme) {
            NavigationSuiteDefaults.colors(
                shortNavigationBarContainerColor = surfaceContainerHighest,
                shortNavigationBarContentColor = contentColorFor(surfaceContainerHighest),
                navigationBarContainerColor = surfaceContainerHighest,
                navigationBarContentColor = contentColorFor(surfaceContainerHighest),
            )
        }
    ) {
        val paneBackStack by remember { derivedStateOf { backStack.filterIsInstance<Pane>() } }
        val strategy = rememberListDetailSceneStrategy<NavKey>()
        NavDisplay(
            backStack = paneBackStack as List<NavKey>,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                for (descriptor in paneDescriptors) {
                    addPaneEntry(descriptor, strategy.directive, backStack)
                }
            },
            sceneStrategy = strategy,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            modifier = Modifier
                .fillMaxSize()
                .thenIf(navSuiteType.isHorizontal()) {
                    // Make pane container cover whole screen, must be done before applying
                    // insets
                    background(MaterialTheme.colorScheme.surfaceContainer)
                }
                .thenIf(strategy.directive.isPartitionedHorizontally()) {
                    // Panes do not apply insets themselves
                    windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
                }
                .thenIf(!navSuiteType.isHorizontal()) {
                    // Now that insets are applied, if background has not been colored do that
                    background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = if (strategy.directive.isPartitionedHorizontally()) {
                            // Make panes look like they are on a surface above the navigation
                            MaterialTheme.shapes.extraLarge.copy(
                                topEnd = CornerSize(0), bottomEnd = CornerSize(0)
                            )
                        } else {
                            RectangleShape
                        }
                    )
                }
        )
    }
}

private fun Iterable<NavDescriptor<*>>.primaries(): List<Pane.Primary> = mapNotNull {
    if (it is NavDescriptor.OfInstance && it.key is Pane.Primary) it.key else null
}

private fun NavigationSuiteType.isHorizontal() =
    this == NavigationSuiteType.NavigationBar ||
        this == NavigationSuiteType.ShortNavigationBarCompact ||
        this == NavigationSuiteType.ShortNavigationBarMedium

@Composable
private fun PrimaryIcon(primary: Pane.Primary, selected: Boolean) {
    Icon(
        painterResource(if (selected) primary.iconSelected else primary.icon),
        contentDescription = null
    )
}

@Composable
private fun PrimaryLabel(primary: Pane.Primary) {
    Text(stringResource(primary.label))
}
