package io.github.timpushkin.tapok.ui.activity.rules

import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.scans.Node
import io.github.timpushkin.tapok.data.scans.Scan
import kotlinx.serialization.Serializable

@Serializable
data class IncompleteRule(
    val sourcePackageName: String?,
    val viewId: String?,
    val containedText: String?,
    val packageName: String?,
    val className: String?,
    val action: Rule.Action
)

fun prefilledRuleFrom(scan: Scan, node: Node) = IncompleteRule(
    sourcePackageName = scan.rootPackageName ?: node.packageName,
    viewId = node.viewId,
    containedText = node.contentDescription ?: node.text,
    packageName = node.packageName,
    className = node.className,
    action = if (node.isClickable) Rule.Action.CLICK else Rule.Action.MUTE_WHEN_PRESENT
)
