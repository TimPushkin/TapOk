package io.github.timpushkin.tapok.ui.activity.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.utils.appendParagraphSpacer

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
            Text(
                buildAnnotatedString {
                    appendLine(stringResource(R.string.accessibility_api_usage_disclosure_body_1))
                    appendLine(
                        "• ${stringResource(R.string.accessibility_api_usage_disclosure_body_2)}"
                    )
                    append(
                        "• ${stringResource(R.string.accessibility_api_usage_disclosure_body_3)}"
                    )
                    appendParagraphSpacer()

                    append(stringResource(R.string.accessibility_api_usage_disclosure_body_4))
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
