package com.github.timpushkin.tapok.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.thenIf(
    condition: Boolean,
    modificator: @Composable (Modifier.() -> Modifier)
): Modifier = if (condition) modificator() else this
