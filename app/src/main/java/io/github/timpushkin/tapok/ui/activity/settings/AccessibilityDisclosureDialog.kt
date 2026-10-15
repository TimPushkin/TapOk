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

package io.github.timpushkin.tapok.ui.activity.settings

import androidx.compose.material3.AlertDialog
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
