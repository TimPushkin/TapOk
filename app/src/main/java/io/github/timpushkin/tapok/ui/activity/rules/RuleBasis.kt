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

package io.github.timpushkin.tapok.ui.activity.rules

import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import kotlinx.serialization.Serializable

@Serializable
data class RuleBasis(
    val sourcePackageName: String?,
    val viewId: String?,
    val containedText: String?,
    val packageName: String?,
    val className: String?,
    val isTargetClickable: Boolean
)

fun ruleBasisFrom(scan: Scan, node: Node) = RuleBasis(
    sourcePackageName = scan.rootPackageName ?: node.packageName,
    viewId = node.viewId,
    containedText = node.contentDescription ?: node.text,
    packageName = node.packageName,
    className = node.className,
    isTargetClickable = node.isClickable
)
