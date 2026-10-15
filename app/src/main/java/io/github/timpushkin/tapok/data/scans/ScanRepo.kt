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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.timpushkin.tapok.data.Deletion
import kotlinx.coroutines.flow.Flow

interface ScanRepo {
    fun observeAllScans(): Flow<List<Scan>>

    suspend fun loadScan(id: Long, options: BitmapFactory.Options? = null): ScanWithDependencies?

    suspend fun insertScan(scan: Scan, nodes: List<Node>, screenshot: Bitmap?): Long

    suspend fun deleteScan(id: Long, mode: Deletion = Deletion.HARD)

    suspend fun undoDeleteScan(id: Long): Boolean

    fun scheduleCleanupOfDeletedScans()
}

data class ScanWithDependencies(val scan: Scan, val nodes: List<Node>, val screenshot: Bitmap?)
