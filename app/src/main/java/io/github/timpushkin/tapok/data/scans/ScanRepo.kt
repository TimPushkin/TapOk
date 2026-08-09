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
