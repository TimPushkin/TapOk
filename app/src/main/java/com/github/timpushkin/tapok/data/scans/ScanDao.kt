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

    @Query("SELECT * FROM node WHERE node.scan_id = :scanId")
    protected abstract suspend fun loadNodesOfScan(scanId: Long): List<Node>

    override suspend fun loadNodesOfScan(scan: Scan): List<Node> = loadNodesOfScan(scan.id)

    @Insert
    abstract override suspend fun insertScan(scan: Scan): Long

    @Insert
    abstract override suspend fun insertNodes(nodes: List<Node>)

    @Delete
    abstract override suspend fun deleteScan(scan: Scan)
}
