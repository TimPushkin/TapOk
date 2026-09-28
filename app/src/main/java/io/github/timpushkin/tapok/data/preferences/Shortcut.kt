package io.github.timpushkin.tapok.data.preferences

import android.os.Parcelable
import io.github.timpushkin.tapok.data.KeyId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Shortcut(val key: KeyId, val isLongPress: Boolean) : Parcelable {
    fun matches(other: Shortcut) =
        key.hasSameMeaning(other.key) && isLongPress == other.isLongPress
}
