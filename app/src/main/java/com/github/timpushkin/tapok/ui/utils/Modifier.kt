package com.github.timpushkin.tapok.ui.utils

import androidx.compose.ui.Modifier

inline fun Modifier.thenIf(
    condition: Boolean,
    modificator: Modifier.() -> Modifier
): Modifier = if (condition) modificator() else this
