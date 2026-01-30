package com.github.timpushkin.tapok.ui.activity

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.github.timpushkin.tapok.ui.activity.help.HelpKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ActivityUi(navKeyDescriptors: Set<TapOkNavKeyDescriptor>) {
    val primaryKeys = navKeyDescriptors.mapNotNull {
        if (it is TapOkNavKeyDescriptor.OfInstance && it.key is TapOkNavKey.Primary) it.key else null
    }.sortedBy { it.order }

    val backStack = rememberNavBackStack(HelpKey)
    val currentPrimary = backStack.findLast { it is TapOkNavKey.Primary } as TapOkNavKey.Primary

    // TODO: replace NavigationBar with NavigationRail on large screens
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = when (val current = backStack.last()) {
                    is TapOkNavKey.Secondary if current.hasLabel -> current::Label
                    else -> currentPrimary::Label
                }
            )
        },
        // TODO: hide navigation bar on content scroll
        // TODO: in single-pane mode do not show navigation bar when viewing a scan
        bottomBar = {
            NavigationBar {
                for (primary in primaryKeys.sortedBy { it.order }) {
                    val selected = primary == currentPrimary
                    NavigationBarItem(
                        selected = selected,
                        onClick = { backStack.addPrimary(primary) },
                        icon = { primary.Icon(selected) },
                        label = { primary.Label() }
                    )
                }
            }
        },
        // TODO: hide or minimize FAB on content scroll
        // TODO: compare to "Moving across tabs" in https://m3.material.io/components/floating-action-button/guidelines
        floatingActionButton = { currentPrimary.Fab(backStack) }
    ) { paddingValues ->
        // Note: not using rememberViewModelStoreNavEntryDecorator to scope view models to entries
        // because then FAB and other content outside of NavDisplay will be out of the scope
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(paddingValues),
            entryProvider = entryProvider {
                for (descriptor in navKeyDescriptors) {
                    when (descriptor) {
                        is TapOkNavKeyDescriptor.OfInstance ->
                            entry(descriptor.key, metadata = descriptor.metadata) {
                                it.Content(backStack)
                            }
                        is TapOkNavKeyDescriptor.OfClass ->
                            addEntryProvider(descriptor.clazz, metadata = descriptor.metadata) {
                                it.Content(backStack)
                            }
                    }
                }
            },
            sceneStrategy = rememberListDetailSceneStrategy()
        )
    }
}
