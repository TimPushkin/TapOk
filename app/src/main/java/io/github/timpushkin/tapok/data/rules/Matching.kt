package io.github.timpushkin.tapok.data.rules

import android.view.accessibility.AccessibilityNodeInfo

fun AccessibilityNodeInfo.matches(
    rule: Rule,
    checkViewId: Boolean = true,
    checkText: Boolean = true,
    checkPackageName: Boolean = true,
    checkClassName: Boolean = true
) = !((checkViewId && viewIdResourceName != rule.viewId) ||
        (checkText && !matchesByText(rule)) ||
        (checkPackageName && packageName != rule.packageName) ||
        (checkClassName && className != rule.className))

/**
 * Whether this node would be returned by
 * [AccessibilityNodeInfo.findAccessibilityNodeInfosByText] called on it or its ancestors with
 * [Rule.containedText] of the specified rule as an argument.
 */
fun AccessibilityNodeInfo.matchesByText(rule: Rule) = rule.containedText == null ||
        text?.contains(rule.containedText, true) == true ||
        contentDescription?.contains(rule.containedText, true) == true
