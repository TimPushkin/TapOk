package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface PaneKey : NavKey {
    @Composable
    fun Content(directive: PaneScaffoldDirective, backStack: MutableList<PaneKey>)

    @Serializable
    sealed class Primary(
        /**
         * Order by which primary destinations are sorted in navigation UIs.
         */
        val order: Int,
        @StringRes val label: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconSelected: Int
    ) : PaneKey

    @Serializable
    sealed class Secondary(val parent: Primary, val fullScreen: Boolean = false) : PaneKey
}

sealed interface NavDescriptor<T : NavKey> {
    val metadata: Map<String, Any>

    data class OfInstance<T : NavKey>(
        val key: T,
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavDescriptor<T>

    data class OfClass<T : NavKey>(
        val cls: KClass<out T>,
        override val metadata: Map<String, Any> = emptyMap()
    ) : NavDescriptor<T>
}

fun MutableList<in PaneKey>.addPrimary(primary: PaneKey.Primary) {
    val top = last()
    if (top == primary || (top is PaneKey.Secondary && top.parent == primary)) {
        return // Already in this primary
    }
    removeAll { it == primary || (it is PaneKey.Secondary && it.parent == primary) }
    add(primary)
}

fun MutableList<in PaneKey>.addSecondary(secondary: PaneKey.Secondary) {
    when (val top = last()) {
        secondary.parent -> add(secondary)
        is PaneKey.Secondary if top.parent == secondary.parent -> set(lastIndex, secondary)
        else -> throw IllegalStateException("Cannot add $secondary after $top")
    }
}
