package com.github.timpushkin.tapok.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.io.File

const val SCREENSHOTS_DIR = "screenshots"

fun Context.screenshotFile(name: String) =
    File(getDir(SCREENSHOTS_DIR, MODE_PRIVATE), name)
