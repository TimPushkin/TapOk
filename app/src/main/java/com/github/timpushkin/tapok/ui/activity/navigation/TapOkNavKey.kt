package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface TapOkNavKey : NavKey {
    @Composable
    fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>)

    @Serializable
    sealed class Primary(
        /** Order by which primary destinations are sorted in navigation UIs. */
        val order: Int,
        @StringRes val label: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconSelected: Int
    ) : TapOkNavKey

    @Serializable
    sealed class Secondary(val parent: Primary) : TapOkNavKey
}

sealed interface TapOkNavKeyDescriptor {
    val metadata: Map<String, Any>

    data class OfInstance(
        val key: TapOkNavKey,
        override val metadata: Map<String, Any> = emptyMap()
    ) : TapOkNavKeyDescriptor

    data class OfClass(
        val clazz: KClass<out TapOkNavKey.Secondary>,
        override val metadata: Map<String, Any> = emptyMap()
    ) : TapOkNavKeyDescriptor
}

fun NavBackStack<NavKey>.addPrimary(primary: TapOkNavKey.Primary) {
    val current = last()
    if (current == primary || (current is TapOkNavKey.Secondary && current.parent == primary)) {
        return // Already in this primary
    }
    removeAll { it == primary || (it is TapOkNavKey.Secondary && it.parent == primary) }
    add(primary)
}

fun NavBackStack<NavKey>.addSecondary(secondary: TapOkNavKey.Secondary) {
    when (val current = last()) {
        secondary.parent -> add(secondary)
        is TapOkNavKey.Secondary if current.parent == secondary.parent -> set(lastIndex, secondary)
        else -> throw IllegalStateException("Cannot add $secondary after $current")
    }
}
