package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface Screen : NavKey {
    @Composable
    fun Content(backStack: MutableList<NavKey>)
}

@Serializable
sealed interface Pane : NavKey {
    @Composable
    fun Content(directive: PaneScaffoldDirective, backStack: MutableList<NavKey>)

    @Serializable
    sealed class Primary(
        /** Order by which primary destinations are sorted in navigation UIs. */
        val order: Int,
        @StringRes val label: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconSelected: Int
    ) : Pane

    @Serializable
    sealed class Secondary(val parent: Primary) : Pane
}

/**
 * Artificial key representing the screen with [Pane]s.
 */
@Serializable
data object PaneScreenKey : Screen {
    @Composable
    override fun Content(backStack: MutableList<NavKey>) {
        throw RuntimeException("${this::class.simpleName} should not be displayed")
    }
}

sealed interface NavDescriptor<T: NavKey> {
    val metadata: Map<String, Any>

    data class OfInstance<T: NavKey>(
        val key: T,
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavDescriptor<T>

    data class OfClass<T: NavKey>(
        val cls: KClass<out T>,
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavDescriptor<T>
}

fun MutableList<NavKey>.addScreen(screen: Screen) {
    check(last() !is Screen) { "Cannot add $screen after screen ${last()}" }
    add(screen)
}

fun MutableList<NavKey>.addPrimary(primary: Pane.Primary) {
    val top = last()
    if (top == primary || (top is Pane.Secondary && top.parent == primary)) {
        return // Already in this primary
    }
    check(top !is Screen) { "Cannot add $primary after screen $top" }
    removeAll { it == primary || (it is Pane.Secondary && it.parent == primary) }
    add(primary)
}

fun MutableList<NavKey>.addSecondary(secondary: Pane.Secondary) {
    when (val top = last()) {
        secondary.parent -> add(secondary)
        is Pane.Secondary if top.parent == secondary.parent -> set(lastIndex, secondary)
        else -> throw IllegalStateException("Cannot add $secondary after $top")
    }
}

fun EntryProviderScope<NavKey>.addScreenEntry(
    descriptor: NavDescriptor<Screen>,
    backStack: MutableList<NavKey>
) {
    when (descriptor) {
        is NavDescriptor.OfInstance ->
            entry(descriptor.key, metadata = descriptor.metadata) {
                it.Content(backStack)
            }
        is NavDescriptor.OfClass ->
            addEntryProvider(descriptor.cls, metadata = descriptor.metadata) {
                it.Content(backStack)
            }
    }
}

fun EntryProviderScope<NavKey>.addPaneEntry(
    descriptor: NavDescriptor<Pane>,
    directive: PaneScaffoldDirective,
    backStack: MutableList<NavKey>
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

@Suppress("UNCHECKED_CAST")
inline fun <reified L : NavKey, reified R : NavKey>
    Iterable<NavDescriptor<*>>.partitionByDescribedKeyType():
    Pair<List<NavDescriptor<L>>, List<NavDescriptor<R>>> {
    val ls = mutableListOf<NavDescriptor<L>>()
    val rs = mutableListOf<NavDescriptor<R>>()
    for (descriptor in this) {
        when (descriptor) {
            is NavDescriptor.OfInstance -> {
                when (descriptor.key) {
                    is L -> ls.add(descriptor as NavDescriptor<L>)
                    is R -> rs.add(descriptor as NavDescriptor<R>)
                }
            }
            is NavDescriptor.OfClass -> {
                val cls = descriptor.cls.java
                when {
                    L::class.java.isAssignableFrom(cls) -> ls.add(descriptor as NavDescriptor<L>)
                    R::class.java.isAssignableFrom(cls) -> rs.add(descriptor as NavDescriptor<R>)
                }
            }
        }
    }
    return ls to rs
}
