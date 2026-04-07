package com.github.timpushkin.tapok.ui.theme

import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Default spacing of pane-level elements.
 */
val DefaultPaneSpacing = 8.dp

/**
 * Default spacing of elements inside a pane.
 */
val DefaultSpacing = 8.dp

/**
 * Sets a minimal required width suitable for a pane.
 */
@Composable
fun Modifier.requiredMinPaneWidth(): Modifier =
    if (
        LocalWindowInfo.current.containerDpSize.width >=
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp
    ) {
        // Should be not less than PaneScaffoldDirective.DefaultPreferredWidth
        requiredWidthIn(min = 360.dp)
    } else {
        // This is used to restrict panes from being squeezed more than necessary in a horizontal
        // multi-pane layout. In a small window we don't want the pane to clip, and there should be
        // no multi-pane layout there anyway.
        this
    }
