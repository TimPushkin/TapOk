package com.github.timpushkin.tapok.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.github.timpushkin.tapok.ui.activity.navigation.TapOkNavKey
import com.github.timpushkin.tapok.ui.activity.navigation.TapOkNavKeyDescriptor
import com.github.timpushkin.tapok.ui.activity.navigation.addPrimary
import com.github.timpushkin.tapok.ui.utils.thenIf

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActivityUi(navKeyDescriptors: Set<TapOkNavKeyDescriptor>) {
    val backStack = rememberNavBackStack(HelpKey)
    val currentPrimary = backStack.findLast { it is TapOkNavKey.Primary } as TapOkNavKey.Primary
    val allPrimaries = navKeyDescriptors.mapNotNull {
        if (it is TapOkNavKeyDescriptor.OfInstance && it.key is TapOkNavKey.Primary) it.key else null
    }.sortedBy { it.order }

    val navSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    // TODO: maybe use https://developer.android.com/develop/ui/compose/animation/shared-elements
    // TODO: in single-pane mode do not show navigation bar when viewing a scan
    NavigationSuiteScaffold(
        navigationItems = {
            for (primary in allPrimaries) {
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
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                for (descriptor in navKeyDescriptors) {
                    when (descriptor) {
                        is TapOkNavKeyDescriptor.OfInstance ->
                            entry(descriptor.key, metadata = descriptor.metadata) {
                                it.Content(listDetailStrategy.directive, backStack)
                            }
                        is TapOkNavKeyDescriptor.OfClass ->
                            addEntryProvider(descriptor.clazz, metadata = descriptor.metadata) {
                                it.Content(listDetailStrategy.directive, backStack)
                            }
                    }
                }
            },
            sceneStrategy = listDetailStrategy,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            modifier =
                Modifier
                    .fillMaxSize()
                    .thenIf(navSuiteType.isHorizontal()) {
                        // Make pane container cover whole screen, must be done before applying
                        // insets
                        background(MaterialTheme.colorScheme.surfaceContainer)
                    }
                    .thenIf(listDetailStrategy.directive.isPartitionedHorizontally()) {
                        // Panes do not apply insets themselves
                        windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
                    }
                    .thenIf(!navSuiteType.isHorizontal()) {
                        // Now that insets are applied, if background has not been colored do that
                        background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = if (listDetailStrategy.directive.isPartitionedHorizontally()) {
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

@Composable
private fun PrimaryIcon(primary: TapOkNavKey.Primary, selected: Boolean) {
    Icon(
        painterResource(if (selected) primary.iconSelected else primary.icon),
        contentDescription = null
    )
}

@Composable
private fun PrimaryLabel(primary: TapOkNavKey.Primary) {
    Text(stringResource(primary.label))
}

private fun NavigationSuiteType.isHorizontal() =
    this == NavigationSuiteType.NavigationBar ||
        this == NavigationSuiteType.ShortNavigationBarCompact ||
        this == NavigationSuiteType.ShortNavigationBarMedium
