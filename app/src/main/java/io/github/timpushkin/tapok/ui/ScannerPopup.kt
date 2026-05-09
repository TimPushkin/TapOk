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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ScannerPopup(
    scanning: Boolean,
    onScan: () -> Unit,
    onViewScans: () -> Unit,
    onTurnOff: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    Box(Modifier.draggable2D(rememberDraggable2DState(onDrag))) {
        var checked by remember { mutableStateOf(false) }
        SplitButtonLayout(
            leadingButton = { LeadingIcon(scanning, onScan) },
            trailingButton = { TrailingIcon(checked) { checked = it } }
        )
        DropdownMenu(expanded = checked, onDismissRequest = { checked = false }) {
            ViewScansItem(onViewScans)
            TurnOffItem(onTurnOff)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            var state by remember { mutableStateOf(if (scanning) State.SCANNING else State.READY) }
            LaunchedEffect(scanning) {
                if (scanning && state != State.SCANNING) {
                    state = State.SCANNING
                } else if (!scanning && state != State.READY) {
                    state = State.SCANNED_RECENTLY
                    delay(1000)
                    state = State.READY
                }
            }
            AnimatedContent(
                state,
                transitionSpec = {
                    if (targetState == State.READY) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
                    }
                },
                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
            ) { state ->
                when (state) {
                    State.READY -> Icon(
                        painterResource(R.drawable.screenshot_frame),
                        contentDescription = description
                    )

                    State.SCANNING -> LoadingIndicator(
                        color = LocalContentColor.current
                    )

                    State.SCANNED_RECENTLY -> Icon(
                        painterResource(R.drawable.check),
                        contentDescription = stringResource(R.string.done)
                    )
                }
            }

        }
    }
}

private enum class State { READY, SCANNING, SCANNED_RECENTLY }

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
private fun TurnOffItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.turn_off)) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.settings_outlined),
                contentDescription = null
            )
        },
    )
}
