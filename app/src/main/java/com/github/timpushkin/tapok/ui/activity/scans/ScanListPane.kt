package com.github.timpushkin.tapok.ui.activity.scans

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.data.scans.Scan
import com.github.timpushkin.tapok.ui.activity.components.DeletableSegmentedListItem
import com.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import com.github.timpushkin.tapok.ui.activity.components.SegmentedListPane
import com.github.timpushkin.tapok.ui.theme.DefaultPaneSpacing
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.time.DateTimeException
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Composable
fun ScanListPane(
    selectedScanId: Long?,
    onSelectScan: (Scan) -> Unit,
    onDeleteScan: (Scan) -> Unit,
    vm: ScanListViewModel = hiltViewModel()
) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        if (uiState.scans.isEmpty()) {
            PanePlaceholderText(
                stringResource(R.string.scans_will_be_here),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            )
        } else {
            SegmentedListPane(
                contentPadding = PaddingValues(DefaultPaneSpacing) + padding,
                modifier = Modifier.consumeWindowInsets(padding)
            ) {
                val scans = uiState.scans.sortedByDescending { it.timestamp }
                val count = scans.size
                itemsIndexed(scans, key = { _, scan -> scan.id }) { index, scan ->
                    ScanItem(
                        index,
                        count,
                        scan,
                        selected = scan.id == selectedScanId,
                        onClick = { onSelectScan(scan) },
                        onDelete = {
                            onDeleteScan(scan)
                            vm.deleteScan(scan)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanItem(
    index: Int,
    count: Int,
    scan: Scan,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (appIcon, appLabel) = LocalContext.current.packageManager.getAppIconAndLabel(scan.appPackage)
    DeletableSegmentedListItem(
        index,
        count,
        selected = selected,
        onClick = onClick,
        onDelete = onDelete,
        content = {
            Text(
                text = appLabel ?: scan.appPackage,
                maxLines = 1,
                overflow = if (appLabel != null) {
                    TextOverflow.Ellipsis
                } else {
                    TextOverflow.StartEllipsis
                }
            )
        },
        leadingContent = {
            Image(
                painter = rememberDrawablePainter(appIcon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        supportingContent = {
            Text(
                scan.timestamp.toFormattedString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

private fun PackageManager.getAppIconAndLabel(appPackage: String): Pair<Drawable, String?> {
    val appInfo = try {
        getApplicationInfo(appPackage, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    return if (appInfo != null) {
        getApplicationIcon(appInfo) to getApplicationLabel(appInfo).toString()
    } else {
        defaultActivityIcon to null
    }
}

@Composable
private fun Instant.toFormattedString(): String {
    val timestampZonedMillis = try {
        toJavaInstant().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        return toString()
    } catch (_: ArithmeticException) {
        return toString()
    }
    val locale = LocalConfiguration.current.locales[0]
    val hh = if (DateFormat.is24HourFormat(LocalContext.current)) "HH" else "hh"
    val format = DateFormat.getBestDateTimePattern(locale, "yyyyMMMdd${hh}mmss")
    return DateFormat.format(format, timestampZonedMillis).toString()
}
