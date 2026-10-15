// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.timpushkin.tapok.ui.utils

import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

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

fun PaneScaffoldDirective.isHorizontallyPartitioned() = maxHorizontalPartitions > 1
