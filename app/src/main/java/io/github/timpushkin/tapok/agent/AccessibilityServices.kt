package io.github.timpushkin.tapok.agent

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.util.valueIterator

fun AccessibilityService.getRootsOfAllWindows(
    prefetchingStrategy: Int = AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID
): Map<Int, AccessibilityNodeInfo> = windowsOnAllDisplays.valueIterator().asSequence()
    .flatten()
    .mapNotNull { w -> w.getRoot(prefetchingStrategy)?.let { root -> w.id to root } }
    .toMap()
