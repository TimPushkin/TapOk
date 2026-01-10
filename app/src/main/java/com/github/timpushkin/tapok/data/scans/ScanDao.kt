package com.github.timpushkin.tapok.data.scans

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScanDao : ScanRepo {
    @Query("SELECT * FROM scan")
    abstract override fun loadAllScans(): Flow<List<Scan>>

    @Query("SELECT * FROM scan JOIN node ON node.scan_id = scan.id WHERE scan.id = :scanId")
    protected abstract suspend fun loadScanWithNodesAsMap(scanId: Long): Map<Scan, List<Node>>

    override suspend fun loadScanWithNodes(scanId: Long): Pair<Scan, List<Node>>? =
        loadScanWithNodesAsMap(scanId).entries.firstOrNull()?.toPair()

    @Insert
    abstract override suspend fun insertScan(scan: Scan): Long

    @Insert
    abstract override suspend fun insertNodes(nodes: List<Node>)

    @Delete
    abstract override suspend fun deleteScan(scan: Scan)
}
