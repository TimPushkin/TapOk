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

package io.github.timpushkin.tapok.data.scans

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toRect
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlin.math.max
import kotlin.math.min

@Entity(
    foreignKeys = [
        ForeignKey(
            Scan::class,
            ["id"],
            ["scan_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Node(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "scan_id", index = true) val scanId: Long,
    /**
     * Bounds of the node in scan's screenshot, in pixels, starting from top-left.
     */
    @Embedded(prefix = "bound_") val bounds: IntRect,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getPackageName].
     */
    @ColumnInfo(name = "package_name", index = true) val packageName: String?,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getClassName].
     */
    @ColumnInfo(name = "class_name") val className: String?,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getViewIdResourceName].
     */
    @ColumnInfo(name = "view_id") val viewId: String?,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getContentDescription].
     */
    @ColumnInfo(name = "content_description") val contentDescription: String?,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getText].
     */
    @ColumnInfo(name = "text") val text: String?,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.isClickable].
     */
    @ColumnInfo(name = "is_clickable") val isClickable: Boolean,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.toString].
     */
    @ColumnInfo(name = "raw_info") val rawInfo: String
) {
    val normalizedBounds by lazy {
        with(bounds) {
            IntRect(
                left = min(left, right),
                top = min(top, bottom),
                right = max(left, right),
                bottom = max(top, bottom)
            )
        }
    }
    val normalizedFloatBounds by lazy { normalizedBounds.toRect() }

    val isReferenceableInRules by lazy {
        viewId != null || contentDescription != null || text != null
    }
}
