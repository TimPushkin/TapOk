package io.github.timpushkin.tapok.data.impl

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.timpushkin.tapok.data.Deletion
import io.github.timpushkin.tapok.data.impl.database.ScanDao
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.data.scans.ScanRepo
import io.github.timpushkin.tapok.data.scans.ScanWithDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val SCREENSHOTS_DIR = "screenshots"

class ScanRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScanDao
) : ScanRepo {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cleanupRequestChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        scheduleCleanupOfDeletedScans() // Cleanup in case something was left because of a crash
        coroutineScope.launch {
            for (request in cleanupRequestChannel) {
                for ((id) in dao.loadSoftDeletedScans()) {
                    hardDeleteScan(id, onlyIfSoftDeleted = true)
                }
            }
        }
    }

    private suspend fun hardDeleteScan(id: Long, onlyIfSoftDeleted: Boolean) {
        if (!onlyIfSoftDeleted) {
            dao.startDeleteScan(id)
        } else if (dao.startDeleteScanIfSoftDeleted(id) == 0) {
            return
        }
        val scan = dao.loadScanOrNull(id) ?: return
        withContext(Dispatchers.IO) { context.screenshotFile(scan).delete() }
        dao.deleteScan(id)
    }

    override fun observeAllScans() = dao.observeAllScans()

    override suspend fun loadScan(
        id: Long,
        options: BitmapFactory.Options?
    ): ScanWithDependencies? {
        val (scan, nodes) = dao.loadScanWithNodes(id) ?: return null
        val screenshot = withContext(Dispatchers.IO) {
            loadScreenshotIfExists(context.screenshotFile(scan), options)
        }
        return ScanWithDependencies(scan, nodes, screenshot)
    }

    private fun loadScreenshotIfExists(file: File, options: BitmapFactory.Options?): Bitmap? {
        if (!file.exists()) {
            return null // BitmapFactory.decodeFile handles this as well, but can print a warning
        }
        return BitmapFactory.decodeFile(file.toString(), options)
    }

    override suspend fun insertScan(scan: Scan, nodes: List<Node>, screenshot: Bitmap?): Long {
        val id = dao.insertScanWithNodes(scan, nodes)
        // Save screenshot only after successfully saving scan to DB to ensure file is accounted for
        if (screenshot != null) {
            withContext(Dispatchers.IO) {
                context.screenshotFile(scan.copy(id = id)).outputStream().use { out ->
                    screenshot.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                }
            }
        }
        return id
    }

    override suspend fun deleteScan(id: Long, mode: Deletion) {
        when (mode) {
            Deletion.SOFT -> dao.softDeleteScan(id)
            Deletion.HARD_AFTER_SOFT -> hardDeleteScan(id, onlyIfSoftDeleted = true)
            Deletion.HARD -> hardDeleteScan(id, onlyIfSoftDeleted = false)
        }
    }

    override suspend fun undoDeleteScan(id: Long) = dao.undoSoftDeleteScan(id) != 0

    override fun scheduleCleanupOfDeletedScans() {
        cleanupRequestChannel.trySend(Unit)
    }
}

private fun Context.screenshotFile(scan: Scan) = File(
    getDir(SCREENSHOTS_DIR, MODE_PRIVATE),
    with(scan) { "$rootPackageName-$timestamp-$id.webp" }
)
