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
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    /**
     * Whether the rule has been marked for deletion.
     */
    @ColumnInfo(name = "is_pending_delete") val isPendingDelete: Boolean = false
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
