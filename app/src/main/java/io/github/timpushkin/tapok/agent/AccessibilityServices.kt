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

package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.MagnificationConfig
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.geometry.Offset
import androidx.core.util.size
import androidx.core.util.valueIterator
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlin.math.roundToInt

// Notes:
// - There is deliberately no recycling-related code, even though it was mandated by the docs before
//   API 33. AndroidX devs "found performance changes to be negligible in even the oldest supported
//   versions" and made it a no-op in compat classes for all API levels in
//   https://github.com/androidx/androidx/commit/d3f14454c63023fc85ea857e9992b8363c1156b8.
// - Using AccessibilityWindowInfo instead of AccessibilityWindowInfoCompat because the latter
//   cannot be obtained from the former (there's no wrapping method exposed), but it is always
//   possible to unwrap the latter to get the former.

fun AccessibilityService.getRootsOfAllWindows(
    prefetchingStrategy: Int = AccessibilityNodeInfoCompat.FLAG_PREFETCH_DESCENDANTS_HYBRID
): Map<Int, AccessibilityNodeInfoCompat> = windowsOnAllDisplays.valueIterator().asSequence()
    .flatten()
    .mapNotNull { w -> w.getRootCompat(prefetchingStrategy)?.let { root -> w.id to root } }
    .toMap()

fun AccessibilityService.getRootInActiveWindowCompat(
    prefetchingStrategy: Int
): AccessibilityNodeInfoCompat? {
    val root = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getRootInActiveWindow(prefetchingStrategy)
    } else {
        rootInActiveWindow
    }
    return root?.let(AccessibilityNodeInfoCompat::wrap)
}

fun AccessibilityService.getWindowDisplayIdCompat(window: AccessibilityWindowInfo): Int? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return window.displayId
    }

    val windows = windowsOnAllDisplays
    for (i in 0 until windows.size) {
        if (windows.valueAt(i).any { it.id == window.id }) {
            return windows.keyAt(i)
        }
    }

    return null
}

val AccessibilityService.magnification: Magnification
    get() {
        // Would be better to use getMagnificationController(displayId) but it is hidden
        val controller = magnificationController

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val region = controller.currentMagnificationRegion
            val config = controller.magnificationConfig
            if (region.isEmpty || config == null) return Magnification.None
            // Window magnification uses an overlay which does not impact the window itself. Not
            // sure how to handle MAGNIFICATION_MODE_DEFAULT, assuming it is fullscreen.
            return if (config.mode != MagnificationConfig.MAGNIFICATION_MODE_WINDOW) {
                Magnification(
                    config.scale,
                    Offset(config.centerX, config.centerY),
                    with(region.bounds) { Offset(exactCenterX(), exactCenterY()) }
                )
            } else {
                Magnification.None
            }
        }

        @Suppress("DEPRECATION") // Not deprecated on this API level
        val region = controller.magnificationRegion
        if (region.isEmpty) return Magnification.None
        @Suppress("DEPRECATION") // Not deprecated on this API level
        return Magnification(
            controller.scale,
            Offset(controller.centerX, controller.centerY),
            with(region.bounds) { Offset(exactCenterX(), exactCenterY()) }
        )
    }

data class Magnification(val scale: Float, val center: Offset, val regionCenter: Offset) {
    fun magnifyX(x: Int) = ((x - center.x) * scale + regionCenter.x).roundToInt()

    fun magnifyY(y: Int) = ((y - center.y) * scale + regionCenter.y).roundToInt()

    fun demagnifyX(x: Int) = ((x - regionCenter.x) / scale + center.x).roundToInt()

    fun demagnifyY(y: Int) = ((y - regionCenter.y) / scale + center.y).roundToInt()

    companion object {
        val None = Magnification(1f, Offset.Zero, Offset.Zero)
    }
}

fun AccessibilityWindowInfo.getRootCompat(prefetchingStrategy: Int): AccessibilityNodeInfoCompat? {
    val root = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getRoot(prefetchingStrategy)
    } else {
        root
    }
    return root?.let(AccessibilityNodeInfoCompat::wrap)
}

val AccessibilityEvent.sourceCompat: AccessibilityNodeInfoCompat?
    get() = source?.let(AccessibilityNodeInfoCompat::wrap)
