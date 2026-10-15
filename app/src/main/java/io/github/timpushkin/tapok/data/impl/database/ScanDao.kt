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

package io.github.timpushkin.tapok.data.impl.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScanDao {
    @Query("SELECT * FROM scan WHERE liveness = 'ALIVE'")
    abstract fun observeAllScans(): Flow<List<Scan>>

    @Query("SELECT * FROM scan WHERE liveness <> 'ALIVE'")
    abstract fun loadSoftDeletedScans(): List<Scan>

    suspend fun loadScan(id: Long): Scan =
        requireNotNull(loadScanOrNull(id)) { "Scan with ID $id does not exist" }

    @Query("SELECT * FROM scan WHERE scan.id = :id")
    abstract suspend fun loadScanOrNull(id: Long): Scan?

    suspend fun loadScanWithNodes(id: Long): Pair<Scan, List<Node>>? =
        loadScanWithNodesImpl(id).entries.firstOrNull()?.toPair()

    @Query(
        """
        SELECT * FROM scan
        JOIN node ON node.scan_id = scan.id
        WHERE scan.id = :id AND scan.liveness = 'ALIVE'
        """
    )
    protected abstract suspend fun loadScanWithNodesImpl(id: Long): Map<Scan, List<Node>>

    @Transaction
    open suspend fun insertScanWithNodes(scan: Scan, nodes: List<Node>): Long {
        val id = insertScan(scan)
        insertNodes(nodes.map { it.copy(scanId = id) })
        return id
    }

    @Insert
    protected abstract suspend fun insertScan(scan: Scan): Long

    @Insert
    protected abstract suspend fun insertNodes(nodes: List<Node>)

    @Query("UPDATE scan SET liveness = 'SOFT_DELETED' WHERE id = :id AND liveness <> 'BEING_HARD_DELETED'")
    abstract suspend fun softDeleteScan(id: Long)

    @Query("UPDATE scan SET liveness = 'ALIVE' WHERE id = :id AND liveness <> 'BEING_HARD_DELETED'")
    abstract suspend fun undoSoftDeleteScan(id: Long): Int

    @Query("UPDATE scan SET liveness = 'BEING_HARD_DELETED' WHERE id = :id")
    abstract suspend fun startDeleteScan(id: Long)

    @Query("UPDATE scan SET liveness = 'BEING_HARD_DELETED' WHERE id = :id AND liveness <> 'ALIVE'")
    abstract suspend fun startDeleteScanIfSoftDeleted(id: Long): Int

    @Query("DELETE FROM scan WHERE id = :id")
    abstract suspend fun deleteScan(id: Long)
}
