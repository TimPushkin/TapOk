package com.github.timpushkin.tapok.ui.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.github.timpushkin.tapok.AgentService
import com.github.timpushkin.tapok.ScannerService
import kotlin.reflect.KClass

private const val TAG = "AccessibilitySettingsAccessors"

private fun Context.openServiceSettings(serviceClass: KClass<*>) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).run {
        if (this@openServiceSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Undocumented feature of Settings app from AOSP to highlight the particular
        // accessibility service. May be ignored on some vendors.
        val componentName = ComponentName(
            this@openServiceSettings,
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

fun Context.openScannerServiceSettings() {
    openServiceSettings(ScannerService::class)
}

fun Context.openAgentServiceSettings() {
    openServiceSettings(AgentService::class)
}
