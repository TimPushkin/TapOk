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

package io.github.timpushkin.tapok.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.preferences.Shortcut
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.ConcurrentWorkerState
import io.github.timpushkin.tapok.ui.utils.rememberConcurrentWorkerState
import io.github.timpushkin.tapok.ui.utils.rememberHasPointerInput

@Composable
fun ScannerPopup(
    scanning: Boolean,
    shortcut: Shortcut?,
    onScan: () -> Unit,
    onViewScans: () -> Unit,
    onHide: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    Box(Modifier.draggable2D(rememberDraggable2DState(onDrag))) {
        val hasPointerInput = rememberHasPointerInput()
        AnimatedContent(hasPointerInput) { interactive ->
            if (interactive) {
                InteractivePopup(scanning, onScan, onViewScans, onHide)
            } else {
                NoninteractivePopup(scanning, shortcut)
            }
        }
    }
}

@Composable
private fun InteractivePopup(
    scanning: Boolean,
    onScan: () -> Unit,
    onViewScans: () -> Unit,
    onHide: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    SplitButtonLayout(
        leadingButton = { LeadingIcon(scanning, onScan) },
        trailingButton = { TrailingIcon(checked) { checked = it } }
    )
    DropdownMenu(expanded = checked, onDismissRequest = { checked = false }) {
        ViewScansItem(onViewScans)
        HideItem(onHide)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LeadingIcon(scanning: Boolean, onScan: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(stringResource(R.string.scan_screen)) } },
        state = rememberTooltipState()
    ) {
        SplitButtonDefaults.LeadingButton(
            onClick = onScan,
            enabled = !scanning,
            // Disabled state is usually so brief that changing the colors looks like a glitch
            colors = ButtonDefaults.buttonColors().run {
                ButtonDefaults.buttonColors(
                    disabledContainerColor = containerColor,
                    disabledContentColor = contentColor
                )
            }
        ) {
            ScanIndicator(scanning, modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ScanIndicator(
    scanning: Boolean,
    modifier: Modifier = Modifier
) {
    val state = rememberConcurrentWorkerState(scanning)
    AnimatedContent(
        state,
        transitionSpec = {
            if (targetState == ConcurrentWorkerState.IDLE) {
                fadeIn() togetherWith fadeOut()
            } else {
                (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
            }
        },
        modifier = modifier
    ) { state ->
        when (state) {
            ConcurrentWorkerState.IDLE -> Icon(
                painterResource(R.drawable.screenshot_frame),
                contentDescription = stringResource(R.string.scan_screen)
            )

            ConcurrentWorkerState.BUSY -> {
                val description = stringResource(R.string.scanning)
                LoadingIndicator(
                    color = LocalContentColor.current,
                    modifier = Modifier
                        .semantics {
                            contentDescription = description
                            role = Role.Image
                        }
                        .size(24.dp)
                )
            }

            ConcurrentWorkerState.FINISHED_RECENTLY -> Icon(
                painterResource(R.drawable.check),
                contentDescription = stringResource(R.string.done)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TrailingIcon(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.toggle_button)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        val stateDescription = stringResource(
            if (checked) R.string.expanded else R.string.collapsed
        )
        SplitButtonDefaults.TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics {
                this.stateDescription = stateDescription
                contentDescription = description
            }
        ) {
            val rotation by animateFloatAsState(if (checked) 180f else 0f)
            Icon(
                painterResource(R.drawable.keyboard_arrow_down),
                modifier = Modifier
                    .size(SplitButtonDefaults.TrailingIconSize)
                    .graphicsLayer { rotationZ = rotation },
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ViewScansItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.view_scans)) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.home_storage_outlined),
                contentDescription = null
            )
        }
    )
}

@Composable
private fun HideItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.hide_overlay)) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.visibility_off_outlined),
                contentDescription = null
            )
        }
    )
}

@Composable
private fun NoninteractivePopup(scanning: Boolean, shortcut: Shortcut?) {
    Card {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DefaultInnerSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(DefaultOuterSpacing)
        ) {
            ScanIndicator(scanning)
            AnimatedContent(shortcut, modifier = Modifier.weight(1f, fill = false)) { shortcut ->
                Text(
                    if (shortcut != null) {
                        "Press ${shortcut.key.label()} to scan"
                    } else {
                        "No pointer input (touchscreen, mouse…) found. Configure a shortcut key instead."
                    }
                )
            }
        }
    }
}
