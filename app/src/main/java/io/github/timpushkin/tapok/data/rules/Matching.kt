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
