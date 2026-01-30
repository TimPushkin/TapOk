package com.github.timpushkin.tapok.ui.activity

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.KClass

@Serializable
sealed class TapOkNavKey : NavKey {
    @Composable
    abstract fun Label()

    @Composable
    abstract fun Content(backStack: NavBackStack<NavKey>)

    @Serializable
    abstract class Primary : TapOkNavKey() {
        /**
         * Order by which primary destinations are sorted in navigation UIs.
         */
        abstract val order: Int

        @Composable
        abstract fun Icon(selected: Boolean)

        @Composable
        open fun Fab(backStack: NavBackStack<NavKey>) {
        }
    }

    @Serializable
    abstract class Secondary : TapOkNavKey() {
        abstract val parent: Primary

        /**
         * Whether [Label] composable has content.
         */
        @Transient
        open val hasLabel = false

        @Composable
        override fun Label() {
        }
    }
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
