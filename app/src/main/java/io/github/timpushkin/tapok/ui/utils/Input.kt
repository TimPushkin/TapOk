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
