// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.timpushkin.tapok.data

import android.os.Parcelable
import android.view.KeyEvent
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class KeyId(val keyCode: Int, val scanCode: Int, val deviceId: Int) : Parcelable {
    constructor(event: KeyEvent) : this(event.keyCode, event.scanCode, event.deviceId)

    // Not transitive, but it would be weird to get such case in practice. For example:
    // A hasSameMeaning B by keyCode, B hasSameMeaning C by scanCode+deviceId, but
    // A !hasSameMeaning C is possible, but it means the same scanCode+deviceId has a different
    // keyCode in B and C.
    fun hasSameMeaning(other: KeyId) =
        (keyCode != KeyEvent.KEYCODE_UNKNOWN && keyCode == other.keyCode) ||
                (scanCode == other.scanCode && deviceId == other.deviceId)

    fun label() = if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
        KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    } else {
        "code$scanCode:device$deviceId"
    }
}
