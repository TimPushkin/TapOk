package io.github.timpushkin.tapok.data.scans

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
     * Fully-qualified view's ID's resource name, e.g. com.example.app:id/view_name.
     *
     * See [android.view.accessibility.AccessibilityNodeInfo.getViewIdResourceName].
     */
    @ColumnInfo(name = "view_id") val viewId: String?,
    /**
     * Unqualified view's class' name, e.g. ViewClass.
     *
     * See [android.view.accessibility.AccessibilityNodeInfo.getClassName].
     */
    @ColumnInfo(name = "class_name") val className: String,
    /**
     * Bounds of the node in scan's screenshot.
     *
     * See [android.view.accessibility.AccessibilityNodeInfo.getBoundsInWindow], its output may be
     * offset here if screenshot's bounds do not match window's bounds.
     */
    @Embedded(prefix = "bound_") val bounds: IntRect,
    /**
     * See [android.view.accessibility.AccessibilityNodeInfo.isClickable].
     */
    @ColumnInfo(name = "is_clickable") val isClickable: Boolean,
    /**
     * Barely processed output of [android.view.accessibility.AccessibilityNodeInfo.toString].
     */
    @ColumnInfo(name = "raw_info") val rawInfo: String
) {
    val fractionalBounds by lazy {
        with(bounds) {
            Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }
}
