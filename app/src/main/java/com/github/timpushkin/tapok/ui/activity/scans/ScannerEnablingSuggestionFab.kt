package com.github.timpushkin.tapok.ui.activity.scans

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import com.github.timpushkin.tapok.MainActivity
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ScannerService

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ScannerEnablingSuggestionFab() {
    val context = LocalContext.current

    if (!context.isScannerEnabled()) {
        return
    }

    MediumExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.open_system_settings)) },
        icon = { Icon(painterResource(R.drawable.settings_filled), contentDescription = null) },
        onClick = { context.openAccessibilitySettings() }
    )
}

private fun Context.isScannerEnabled(): Boolean {
    val accessibilityManager = checkNotNull(getSystemService<AccessibilityManager>())
    for (service in accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)) {
        if (service.settingsActivityName != MainActivity::class.qualifiedName) {
            continue
        }
        val packageName = checkNotNull(service.resolveInfo.serviceInfo).packageName
        if (packageName == ScannerService::class.java.packageName) {
            return true
        }
    }
    return false
}

private fun Context.openAccessibilitySettings() {
    // TODO: review other accessibility-related actions
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (ex: ActivityNotFoundException) {
        Log.e("UI", "Failed to open accessibility settings: $ex")
    }
}
