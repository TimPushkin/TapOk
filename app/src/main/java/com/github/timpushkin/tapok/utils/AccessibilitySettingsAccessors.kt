package com.github.timpushkin.tapok.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import kotlin.reflect.KClass

private const val TAG = "AccessibilitySettingsAccessors"

fun Context.openAccessibilityServiceSettings(serviceClass: KClass<*>) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).run {
        if (this@openAccessibilityServiceSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Undocumented feature of Settings app from AOSP to highlight the particular
        // accessibility service. May be ignored on some vendors.
        val componentName = ComponentName(
            this@openAccessibilityServiceSettings,
            serviceClass.java
        ).flattenToString()
        putExtra(
            ":settings:show_fragment_args",
            Bundle(1).apply { putString(":settings:fragment_args_key", componentName) }
        )
    }
    try {
        startActivity(intent)
    } catch (ex: ActivityNotFoundException) {
        Log.e(TAG, "Failed to open accessibility settings: $ex")
    }
}

fun Context.isAccessibilityServiceEnabled(serviceClass: KClass<*>): Boolean {
    val services = checkNotNull(getSystemService<AccessibilityManager>())
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    val enabled = services.any { it.resolveInfo.serviceInfo.name == serviceClass.java.name }
    Log.d(
        TAG,
        "${if (enabled) "Found" else "Not found"} " +
                "${serviceClass.java.name} in ${services.map { it.resolveInfo.serviceInfo.name }}"
    )
    return enabled
}

@Composable
fun rememberIsAccessibilityServiceEnabled(serviceClass: KClass<*>): Boolean {
    val context = LocalContext.current
    // produceState does a similar thing (remember + DisposableEffect) but it does not play well
    // with evaluated initial value
    var enabled by remember { mutableStateOf(context.isAccessibilityServiceEnabled(serviceClass)) }
    DisposableEffect(context) {
        val a11yListener = AccessibilityManager.AccessibilityServicesStateChangeListener {
            Log.d(TAG, "AccessibilityServicesStateChangeListener triggered")
            enabled = context.isAccessibilityServiceEnabled(serviceClass)
        }
        val a11yManager = checkNotNull(context.getSystemService<AccessibilityManager>())
        a11yManager.addAccessibilityServicesStateChangeListener(a11yListener)
        onDispose { a11yManager.removeAccessibilityServicesStateChangeListener(a11yListener) }
    }
    return enabled
}
