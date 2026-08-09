package io.github.timpushkin.tapok.ui.activity.scans

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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.scans.Scan
import io.github.timpushkin.tapok.ui.activity.components.DeletableSegmentedListItem
import io.github.timpushkin.tapok.ui.activity.components.PanePlaceholderText
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPane
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPaneDefaults
import io.github.timpushkin.tapok.ui.activity.components.rememberDeletableItemEpochs
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import java.time.DateTimeException
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Composable
fun ScanListPane(
    selectedScanId: Long?,
    onSelectScan: (Scan) -> Unit,
    vm: ScanListViewModel = hiltViewModel()
) {
    val snackbarHostState = retain { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        if (uiState.scans.isEmpty()) {
            PanePlaceholderText(
                stringResource(R.string.scans_will_be_here),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            )
        } else {
            val epochs = rememberDeletableItemEpochs(uiState.scans.map { it.id })
            SegmentedListPane(
                contentPadding = PaddingValues(DefaultOuterSpacing) + padding,
                modifier = Modifier.consumeWindowInsets(padding)
            ) {
                val scans = uiState.scans.sortedByDescending { it.timestamp }
                val count = scans.size
                itemsIndexed(
                    scans,
                    key = { _, scan -> "${scan.id}-${epochs[scan.id]}" }
                ) { index, scan ->
                    val deletionMessage = stringResource(
                        R.string.deleted_scan,
                        scan.rootPackageName
                            ?: stringResource(R.string.unidentifiable_app).lowercase()
                    )
                    val undoString = stringResource(R.string.undo)
                    ScanItem(
                        index,
                        count,
                        scan,
                        selected = scan.id == selectedScanId,
                        onClick = { onSelectScan(scan) },
                        onDelete = {
                            vm.launchDeleteWithUndo(scan) {
                                snackbarHostState.showSnackbar(
                                    deletionMessage,
                                    undoString,
                                    duration = SnackbarDuration.Short
                                ) == SnackbarResult.ActionPerformed
                            }
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
    val (appIcon, appLabel) = LocalContext.current.packageManager.getAppIconAndLabel(
        scan.rootPackageName
    )
    val colors = SegmentedListPaneDefaults.colors()
    DeletableSegmentedListItem(
        index,
        count,
        selected = selected,
        onClick = onClick,
        onDelete = onDelete,
        content = {
            Text(
                text = appLabel
                    ?: scan.rootPackageName
                    ?: stringResource(R.string.unidentifiable_app),
                maxLines = 1,
                overflow = if (appLabel != null || scan.rootPackageName == null) {
                    TextOverflow.Ellipsis // General text
                } else {
                    TextOverflow.StartEllipsis // Package
                },
                color = colors.run {
                    if (scan.rootPackageName != null) contentColor else disabledContentColor
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
        },
        colors = colors
    )
}

private fun PackageManager.getAppIconAndLabel(appPackage: String?): Pair<Drawable, String?> {
    if (appPackage == null) {
        return defaultActivityIcon to null
    }
    val appInfo = try {
        getApplicationInfo(appPackage, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        return defaultActivityIcon to null
    }
    return getApplicationIcon(appInfo) to getApplicationLabel(appInfo).toString()
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
