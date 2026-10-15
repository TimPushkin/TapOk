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

package io.github.timpushkin.tapok.ui.activity.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.timpushkin.tapok.R

@Composable
fun NavigateBackButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            painterResource(R.drawable.arrow_back),
            contentDescription = stringResource(R.string.navigate_back)
        )
    }
}

@Composable
fun CloseButton(onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(
            painterResource(R.drawable.close),
            contentDescription = stringResource(R.string.close)
        )
    }
}
