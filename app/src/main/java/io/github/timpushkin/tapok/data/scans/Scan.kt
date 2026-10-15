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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity
data class Scan(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /**
     * Result of [android.view.accessibility.AccessibilityNodeInfo.getPackageName] from the root
     * of the scanned window. This is the closest the scanner can get to the ID of the scanned app.
     */
    @ColumnInfo(name = "root_package_name") val rootPackageName: String?,
    val timestamp: Instant,
    /**
     * Whether the scan has been marked for deletion.
     */
    @ColumnInfo(name = "liveness") val liveness: Liveness = Liveness.ALIVE
) {
    // Database queries reference these by name and must be kept in sync manually.
    enum class Liveness {
        /**
         * Not deleted.
         */
        ALIVE,

        /**
         * Deleted but can be brought back.
         */
        SOFT_DELETED,

        /**
         * In process of final deleting, cannot be brought back.
         */
        BEING_HARD_DELETED
    }
}
