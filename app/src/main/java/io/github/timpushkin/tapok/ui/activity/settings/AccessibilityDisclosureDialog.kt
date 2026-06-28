package io.github.timpushkin.tapok.ui.activity.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccessibilityDisclosureDialog(onAccept: () -> Unit, onHide: () -> Unit) {
    AlertDialog(
        icon = {
            Icon(painterResource(R.drawable.accessibility_new), contentDescription = null)
        },
        title = {
            Text(stringResource(R.string.accessibility_api_usage))
        },
        text = {
            val paragraphs =
                annotatedStringArrayResource(R.array.accessibility_api_usage_disclosure)
            Text(
                rememberAnnotatedString(paragraphs) {
                    appendAnnotatedParagraphs(paragraphs)
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAccept()
                    onHide()
                }
            ) {
                Text(stringResource(R.string.accept_and_proceed))
            }
        },
        dismissButton = {
            TextButton(onHide) {
                Text(stringResource(R.string.cancel))
            }
        },
        onDismissRequest = onHide
    )
}
