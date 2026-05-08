package com.github.timpushkin.tapok.ui.activity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import kotlinx.serialization.Serializable

// Based on official Nav3 recipes:
// - https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/multiplestacks
// - https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/modular/hilt

@Serializable
open class PrimaryKey(
    val index: Int,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconSelected: Int
) : NavKey

@Serializable
open class SecondaryKey(val fullScreen: Boolean = false) : NavKey

typealias EntryProviderInstaller = EntryProviderScope<NavKey>.(Navigator) -> Unit

data class NavRoute(val primary: PrimaryKey, val secondaries: Iterable<SecondaryKey>)

@Composable
fun rememberNavigator(
    primaries: Set<PrimaryKey>,
    mainPrimary: PrimaryKey,
    initialRoute: NavRoute = NavRoute(mainPrimary, emptyList()),
    onNavigateTo: (NavKey) -> Unit = {},
    onNavigateBack: (NavKey) -> Unit = {}
): Navigator {
    require(mainPrimary in primaries) { "Main primary must be among all primaries" }
    val currentPrimary = rememberSerializable(
        primaries, serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(initialRoute.primary)
    }
    // Sort to ensure the back stacks are always remembered in the same order
    val backStacks = primaries.sortedBy { it.index }.associateWith { primary ->
        val secondaries =
            if (primary == initialRoute.primary) initialRoute.secondaries else emptyList()
        rememberNavBackStack(primary, *secondaries.toList().toTypedArray())
    }
    return remember(primaries, mainPrimary, onNavigateTo, onNavigateBack) {
        Navigator(mainPrimary, currentPrimary, backStacks, onNavigateTo, onNavigateBack)
    }
}

class Navigator(
    private val mainPrimary: PrimaryKey,
    currentPrimary: MutableState<PrimaryKey>,
    private val backStacks: Map<PrimaryKey, NavBackStack<NavKey>>,
    private val onNavigateTo: (NavKey) -> Unit = {},
    private val onNavigateBack: (NavKey) -> Unit = {}
) {
    val primaries: Set<PrimaryKey>
        get() = backStacks.keys
    var currentPrimary: PrimaryKey by currentPrimary
        private set
    val currentKey: NavKey
        get() = currentBackStack.last()

    private val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(currentPrimary)

    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
        entryDecoratorsProvider: @Composable () -> List<NavEntryDecorator<NavKey>> =
            { listOf(rememberSaveableStateHolderNavEntryDecorator()) }
    ): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, backStack) ->
            rememberDecoratedNavEntries(backStack, entryDecoratorsProvider(), entryProvider)
        }
        // Only return the entries for the stacks that are currently in use
        return linkedSetOf(mainPrimary, currentPrimary).flatMap {
            decoratedEntries.getValue(it)
        }
    }

    fun navigateTo(to: NavKey) {
        onNavigateTo(to)
        if (to is PrimaryKey) {
            require(to in backStacks.keys) { "Unregistered primary key $to" }
            currentPrimary = to
        } else {
            backStacks.getValue(currentPrimary).add(to)
        }
    }

    fun navigateBack() {
        val from = currentKey
        onNavigateBack(from)
        if (from == currentPrimary) {
            currentPrimary = mainPrimary
        } else {
            currentBackStack.removeLastOrNull()
        }
    }
}
