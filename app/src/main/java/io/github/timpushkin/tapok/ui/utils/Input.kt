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

package io.github.timpushkin.tapok.ui.utils

import android.content.res.Configuration
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

@Composable
fun rememberHasPointerInput(): Boolean {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    return remember(configuration, context) {
        configuration.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH ||
                context.getSystemService<InputManager>()?.let { inputManager ->
                    inputManager.inputDeviceIds.any { id ->
                        inputManager.getInputDevice(id)?.run {
                            supportsSource(InputDevice.SOURCE_CLASS_POINTER) &&
                                    // We already know this particular screen is not a touchscreen,
                                    // ignore other touchscreens
                                    !supportsSource(InputDevice.SOURCE_TOUCHSCREEN)
                        } == true
                    }
                } == true
    }
}
