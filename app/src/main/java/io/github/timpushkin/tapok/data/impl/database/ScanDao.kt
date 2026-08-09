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
