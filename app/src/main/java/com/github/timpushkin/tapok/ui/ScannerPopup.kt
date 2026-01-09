package com.github.timpushkin.tapok.ui

import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.timpushkin.tapok.R

// From FabPrimaryTokens
private const val SCANNER_BUTTON_ELEVATION_DP = 8

@Composable
fun ScannerPopup(onClick: () -> Unit, onDrag: (Offset) -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .padding(SCANNER_BUTTON_ELEVATION_DP.dp) // Ensure the shade is not clipped
            .draggable2D(rememberDraggable2DState(onDrag))
    ) {
        Icon(
            painterResource(R.drawable.screenshot_frame),
            contentDescription = stringResource(R.string.scan_screen)
        )
    }
}
