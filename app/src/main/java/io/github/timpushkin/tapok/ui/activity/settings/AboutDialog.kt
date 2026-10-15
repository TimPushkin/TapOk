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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.theme.DefaultInnerSpacing
import io.github.timpushkin.tapok.ui.utils.annotatedStringArrayResource
import io.github.timpushkin.tapok.ui.utils.appendAnnotatedParagraphs
import io.github.timpushkin.tapok.ui.utils.rememberAnnotatedString

private const val SOURCE_URL = "https://github.com/TimPushkin/TapOk"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_app)) },
        text = {
            Column {
                val uriHandler = LocalUriHandler.current

                Text(
                    annotatedStringArrayResource(R.array.about_app_text).let {
                        rememberAnnotatedString(it) { appendAnnotatedParagraphs(it) }
                    }
                )

                Spacer(Modifier.height(DefaultInnerSpacing))

                TextButton(onClick = { uriHandler.openUri(SOURCE_URL) }) {
                    Text(stringResource(R.string.source_code))
                }
                TextButton(onClick = { uriHandler.openUri(LICENSE_URL) }) {
                    Text(stringResource(R.string.license))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
