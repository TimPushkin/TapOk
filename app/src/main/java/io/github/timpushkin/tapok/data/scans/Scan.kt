package io.github.timpushkin.tapok.data.scans

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(indices = [Index("screenshot_file", unique = true)])
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getPackageName] from the root
     * of the scanned window. This is the closest the scanner can get to the ID of the scanned app.
     */
    @ColumnInfo(name = "root_package_name") val rootPackageName: String?,
    val timestamp: Instant,
    @ColumnInfo(name = "screenshot_file") val screenshotFile: String?
)
