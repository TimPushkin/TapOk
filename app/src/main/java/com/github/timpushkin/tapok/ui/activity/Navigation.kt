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

@Composable
fun rememberNavigator(initial: PrimaryKey, primaries: Iterable<PrimaryKey>): Navigator {
    require(initial in primaries) { "Initial key must be among all registered primary keys" }
    val primary = rememberSerializable(
        initial, primaries, serializer = MutableStateSerializer(NavKeySerializer())
    ) { mutableStateOf(initial) }
    val backStacks = primaries.associateWith { key -> rememberNavBackStack(key) }
    return remember(initial, primaries) { Navigator(initial, primary, backStacks) }
}

class Navigator(
    private val initialPrimary: PrimaryKey,
    currentPrimary: MutableState<PrimaryKey>,
    private val backStacks: Map<PrimaryKey, NavBackStack<NavKey>>
) {
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
        return linkedSetOf(initialPrimary, currentPrimary).flatMap {
            decoratedEntries.getValue(it)
        }
    }

    fun navigateTo(key: NavKey){
        if (key is PrimaryKey){
            require(key in backStacks.keys) { "Unregistered primary key $key" }
            currentPrimary = key
        } else {
            backStacks.getValue(currentPrimary).add(key)
        }
    }

    fun navigateBack(){
        if (currentKey == currentPrimary){
            currentPrimary = initialPrimary
        } else {
            currentBackStack.removeLastOrNull()
        }
    }
}
