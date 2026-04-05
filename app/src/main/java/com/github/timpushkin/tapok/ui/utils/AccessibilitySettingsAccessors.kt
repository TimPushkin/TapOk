package com.github.timpushkin.tapok.ui.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    // The format of ENABLED_ACCESSIBILITY_SERVICES string is not documented so relying on internals
    // here. Using AccessibilityManager.getEnabledAccessibilityServiceList() would be better in this
    // sense, but it returns stale values sometimes.
    val names = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: ""
    val (nameFull, nameShort) = ComponentName(this, serviceClass.java).run {
        flattenToString() to flattenToShortString()
    }
    return names.split(':').any { it == nameFull || it == nameShort }.also {
        Log.d(TAG, "${if (it) "Found" else "Not found"} '$nameFull' / '$nameShort' in '$names'")
    }
}

@Composable
fun rememberIsAccessibilityServiceEnabled(serviceClass: KClass<*>): Boolean {
    val context = LocalContext.current
    // produceState does a similar thing (remember + DisposableEffect) but it does not play well
    // with evaluated initial value
    var enabled by remember { mutableStateOf(context.isAccessibilityServiceEnabled(serviceClass)) }
    DisposableEffect(context) {
        // Known issue: sometimes the notification gets delayed for some reason
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(TAG, "List of enabled accessibility services changed")
                enabled = context.isAccessibilityServiceEnabled(serviceClass)
            }
        }
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}
