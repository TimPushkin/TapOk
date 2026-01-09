package com.github.timpushkin.tapok.data.rules

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index("package_name", "view_name", "action", unique = true)]
)
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /**
     * Name of the rule given by the user.
     */
    val name: String,
    /**
     * Targeted view's package name.
     */
    @ColumnInfo(name = "package_name") val packageName: String,
    /**
     * Targeted view's ID's resource name.
     */
    @ColumnInfo(name = "view_name") val viewName: String,
    /**
     * What to do when the rule matches.
     */
    val action: Action
) {
    enum class Action {
        CLICK,
        MUTE_WHEN_PRESENT
    }

    /**
     * Fully-qualified targeted view's ID's resource name.
     */
    val viewId by lazy { "$packageName:id/$viewName" }

    fun toShortString() = "Rule(action: $action, id: $viewName)"
}
