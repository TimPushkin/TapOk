package io.github.timpushkin.tapok.data.rules

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /**
     * Name of the rule given by the user.
     */
    val name: String,
    /**
     * Used for event filtering via
     * [android.accessibilityservice.AccessibilityServiceInfo.packageNames].
     */
    @ColumnInfo(name = "source_package_name") val sourcePackageName: String,
    /**
     * Used as an argument for
     * [android.view.accessibility.AccessibilityNodeInfo.findAccessibilityNodeInfosByViewId].
     */
    @ColumnInfo(name = "view_id") val viewId: String?,
    /**
     * Used as an argument for
     * [android.view.accessibility.AccessibilityNodeInfo.findAccessibilityNodeInfosByText].
     */
    @ColumnInfo(name = "contained_text") val containedText: String?,
    /**
     * Used for additional filtering by comparison with
     * [android.view.accessibility.AccessibilityNodeInfo.getPackageName].
     */
    @ColumnInfo(name = "package_name") val packageName: String?,
    /**
     * Used for additional filtering by comparison with
     * [android.view.accessibility.AccessibilityNodeInfo.getClassName].
     */
    @ColumnInfo(name = "class_name") val className: String?,
    /**
     * What to do when the rule matches.
     */
    val action: Action,
    /**
     * Whether the automation considers the rule.
     */
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
) {
    init {
        require(viewId != null || containedText != null) { "No search criteria" }
    }

    enum class Action {
        CLICK,
        CLICK_NEAREST_CLICKABLE_ANCESTOR,
        CLICK_NEAREST_CLICKABLE_DESCENDANT,
        MUTE_WHEN_PRESENT
    }
}
