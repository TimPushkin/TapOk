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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButton
import androidx.compose.material3.SplitButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.utils.ConcurrentWorkerState
import io.github.timpushkin.tapok.ui.utils.rememberConcurrentWorkerState

@Composable
fun ScannerPopup(
    scanning: Boolean,
    onScan: () -> Unit,
    onViewScans: () -> Unit,
    onHide: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    Box(Modifier.draggable2D(rememberDraggable2DState(onDrag))) {
        var checked by remember { mutableStateOf(false) }
        SplitButton(
            leadingButton = { LeadingIcon(scanning, onScan) },
            trailingButton = { TrailingIcon(checked) { checked = it } }
        )
        DropdownMenu(expanded = checked, onDismissRequest = { checked = false }) {
            ViewScansItem(onViewScans)
            HideItem(onHide)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
private fun LeadingIcon(scanning: Boolean, onScan: () -> Unit) {
    val description = stringResource(R.string.scan_screen)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(description) } },
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
                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
            ) { state ->
                when (state) {
                    ConcurrentWorkerState.IDLE -> Icon(
                        painterResource(R.drawable.screenshot_frame),
                        contentDescription = description
                    )

                    ConcurrentWorkerState.BUSY -> LoadingIndicator(
                        color = LocalContentColor.current
                    )

                    ConcurrentWorkerState.FINISHED_RECENTLY -> Icon(
                        painterResource(R.drawable.check),
                        contentDescription = stringResource(R.string.done)
                    )
                }
            }
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
        },
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
        },
    )
}
