package com.github.timpushkin.tapok.data.scans

import kotlinx.coroutines.flow.Flow

interface ScanRepo {
    fun loadAllScans(): Flow<List<Scan>>

    suspend fun loadScanWithNodes(scanId: Long): Pair<Scan, List<Node>>?

    suspend fun insertScan(scan: Scan): Long

    suspend fun insertNodes(nodes: List<Node>)

    suspend fun deleteScan(scan: Scan)
}