package io.github.timpushkin.tapok.ui.activity.settings

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.preferences.Shortcut

private val NAVIGATION_KEY_CODES = setOf(
    // KeyEvent.isConfirmKey
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_SPACE,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    // ViewRootImpl.performFocusNavigation
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_TAB
)

@Composable
fun ShortcutConfigurationDialog(
    title: String,
    saved: Shortcut?,
    onRecord: suspend () -> Shortcut,
    onSave: (Shortcut?) -> Unit,
    onHide: () -> Unit
) {
    var recorded by rememberSaveable(saved) { mutableStateOf(saved) }
    var recording by rememberSaveable { mutableStateOf(false) }
    if (recording) {
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(Unit) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recorded = onRecord()
                recording = false
            }
        }
    }

    AlertDialog(
        title = { Text(title) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (recording) {
                            stringResource(R.string.press_key)
                        } else {
                            recorded?.run { key.label() } ?: stringResource(R.string.none)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { recorded = null },
                        enabled = recorded != null,
                    ) {
                        Icon(
                            painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                    IconButton(onClick = { recording = !recording }) {
                        AnimatedContent(recording) { recording ->
                            if (recording) {
                                Icon(
                                    painterResource(R.drawable.edit_off_outlined),
                                    contentDescription =
                                        stringResource(R.string.stop_recording_key_presses)
                                )
                            } else {
                                Icon(
                                    painterResource(R.drawable.edit_outlined),
                                    contentDescription =
                                        stringResource(R.string.start_recording_key_presses)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val enabled = recorded != null
                    Checkbox(
                        checked = recorded?.isLongPress == true,
                        onCheckedChange = { recorded = recorded?.copy(isLongPress = it) },
                        enabled = enabled
                    )
                    Text(
                        stringResource(R.string.long_press),
                        color = LocalContentColor.current.let {
                            if (enabled) it else it.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(recorded)
                    onHide()
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onHide) { Text(stringResource(R.string.cancel)) } },
        onDismissRequest = onHide,
        // Swallow all navigational key events while recording
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .onPreviewKeyEvent { recording && it.nativeKeyEvent.keyCode in NAVIGATION_KEY_CODES }
    )
}
