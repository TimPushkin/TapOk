package com.github.timpushkin.tapok.ui.activity.scans

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ScannerService

private const val TAG = "EnableScannerTextButton"

@Composable
fun EnableScannerTextButton(modifier: Modifier = Modifier) {
    val isScannerRunning by isScannerRunningAsState()
    if (isScannerRunning) {
        return
    }

    val context = LocalContext.current
    TextButton(onClick = { context.openScannerServiceSettings() }, modifier = modifier) {
        Text(stringResource(R.string.enable_scanner))
    }
}

@Composable
private fun isScannerRunningAsState(): State<Boolean> {
    val context = LocalContext.current
    // produceState does a similar thing (remember + DisposableEffect) but it does not play well
    // with evaluated initial value
    val state = remember { mutableStateOf(context.isScannerServiceEnabled()) }
    DisposableEffect(context) {
        // Known issue: sometimes the notification gets delayed for some reason
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(TAG, "List of enabled accessibility services changed")
                state.value = context.isScannerServiceEnabled()
            }
        }
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return state
}

private fun Context.isScannerServiceEnabled(): Boolean {
    // The format of ENABLED_ACCESSIBILITY_SERVICES string is not documented so relying on internals
    // here. Using AccessibilityManager.getEnabledAccessibilityServiceList() would be better in this
    // sense but it returns stale values sometimes.
    val names = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: ""
    val (nameFull, nameShort) = ComponentName(this, ScannerService::class.java).run {
        flattenToString() to flattenToShortString()
    }
    return names.split(':').any { it == nameFull || it == nameShort }.also {
        Log.d(TAG, "${if (it) "Found" else "Not found"} '$nameFull' / '$nameShort' in '$names'")
    }
}

private fun Context.openScannerServiceSettings() {
    // TODO: review other accessibility-related actions
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).run {
        if (this@openScannerServiceSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Undocumented feature of Settings app from AOSP to highlight the particular
        // accessibility service. May be ignored on some vendors.
        val componentName = ComponentName(
            this@openScannerServiceSettings,
            ScannerService::class.java
        ).flattenToString()
        putExtra(
            ":settings:show_fragment_args",
            bundleOf(":settings:fragment_args_key" to componentName)
        )
    }
    try {
        startActivity(intent)
    } catch (ex: ActivityNotFoundException) {
        // TODO: toast?
        Log.e(TAG, "Failed to open accessibility settings: $ex")
    }
}
