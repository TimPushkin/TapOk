package io.github.timpushkin.tapok.data.scans

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(indices = [Index("screenshot_file", unique = true)])
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
    val timestamp: Instant,
    @ColumnInfo(name = "screenshot_file") val screenshotFile: String
)
