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
