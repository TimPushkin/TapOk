package io.github.timpushkin.tapok.ui.activity.rules

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule

@DrawableRes
fun Rule.Action.drawableRes() = when (this) {
    Rule.Action.CLICK -> R.drawable.touch_app_outlined
    Rule.Action.MUTE_WHEN_PRESENT -> R.drawable.volume_off_outlined
}

@StringRes
fun Rule.Action.stringRes() = when (this) {
    Rule.Action.CLICK -> R.string.click
    Rule.Action.MUTE_WHEN_PRESENT -> R.string.mute_when_present
}
