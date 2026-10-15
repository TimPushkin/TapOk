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

package io.github.timpushkin.tapok.data.rules

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

fun AccessibilityNodeInfoCompat.matches(
    rule: Rule,
    checkViewId: Boolean = true,
    checkText: Boolean = true,
    checkPackageName: Boolean = true,
    checkClassName: Boolean = true
) = (!checkViewId || rule.viewId == null || viewIdResourceName == rule.viewId) &&
        (!checkText || matchesByText(rule)) &&
        (!checkPackageName || rule.packageName == null || packageName == rule.packageName) &&
        (!checkClassName || rule.className == null || className == rule.className)

/**
 * Whether this node would be returned by
 * [AccessibilityNodeInfoCompat.findAccessibilityNodeInfosByText] called on it or its ancestors with
 * [Rule.containedText] of the specified rule as an argument.
 */
fun AccessibilityNodeInfoCompat.matchesByText(rule: Rule) = rule.containedText == null ||
        text?.contains(rule.containedText, true) == true ||
        contentDescription?.contains(rule.containedText, true) == true
