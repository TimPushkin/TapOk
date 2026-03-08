package com.github.timpushkin.tapok.data.scans

import androidx.compose.ui.geometry.Rect
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
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "view_name") val viewName: String?,
    @ColumnInfo(name = "class_name") val className: String,
    @Embedded(prefix = "bound_") val bounds: Bounds,
    @ColumnInfo(name = "is_clickable") val isClickable: Boolean
) {
    data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    val viewId by lazy { viewName?.let { "$packageName:id/$it" } }

    val boundsRect by lazy {
        with(bounds) {
            Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }
}
