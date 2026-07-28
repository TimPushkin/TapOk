package io.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleInputValidity

@Composable
fun RuleInputValidity.Name.errorText() = when (this) {
    RuleInputValidity.Name.OK -> null
}

@Composable
fun RuleInputValidity.SourcePackageName.errorText(): String? {
    val resId = when (this) {
        RuleInputValidity.SourcePackageName.OK -> return null

        RuleInputValidity.SourcePackageName.EMPTY -> R.string.error_this_field_is_required

        RuleInputValidity.SourcePackageName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_START ->
            R.string.error_segments_can_only_start_with

        RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_PART ->
            R.string.error_segments_can_only_contain

        RuleInputValidity.SourcePackageName.NOT_ENOUGH_SEGMENTS ->
            R.string.error_at_least_two_segments_required
    }
    return stringResource(resId)
}

@Composable
fun RuleInputValidity.ViewId.errorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ViewId.OK -> return null

        RuleInputValidity.ViewId.EMPTY_WITH_NO_ALTERNATIVE -> R.string.error_no_search_criteria

        RuleInputValidity.ViewId.NO_DIVIDER -> R.string.error_no_view_id_divider

        RuleInputValidity.ViewId.PACKAGE_EMPTY -> R.string.error_empty_package

        RuleInputValidity.ViewId.PACKAGE_EMPTY_SEGMENT -> R.string.error_empty_package_segment

        RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_START ->
            R.string.error_illegal_package_segment_start

        RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_PART ->
            R.string.error_illegal_package_segment_part

        RuleInputValidity.ViewId.VIEW_EMPTY -> R.string.error_empty_view_name

        RuleInputValidity.ViewId.VIEW_ILLEGAL_START -> R.string.error_illegal_view_name_start

        RuleInputValidity.ViewId.VIEW_ILLEGAL_PART -> R.string.error_illegal_view_name_part
    }
    return stringResource(resId)
}

@Composable
fun RuleInputValidity.ContainedText.errorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ContainedText.OK -> return null
        RuleInputValidity.ContainedText.EMPTY_WITH_NO_ALTERNATIVE ->
            R.string.error_no_search_criteria
    }
    return stringResource(resId)
}

@Composable
fun RuleInputValidity.PackageName.errorText(): String? {
    val resId = when (this) {
        RuleInputValidity.PackageName.OK -> return null

        RuleInputValidity.PackageName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_START ->
            R.string.error_segments_can_only_start_with

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_PART ->
            R.string.error_segments_can_only_contain

        RuleInputValidity.PackageName.NOT_ENOUGH_SEGMENTS ->
            R.string.error_at_least_two_segments_required
    }
    return stringResource(resId)
}

@Composable
fun RuleInputValidity.ClassName.errorText(): String? {
    val resId = when (this) {
        RuleInputValidity.ClassName.OK -> return null

        RuleInputValidity.ClassName.EMPTY_SEGMENT -> R.string.error_empty_segments

        RuleInputValidity.ClassName.ILLEGAL_SEGMENT_START ->
            R.string.error_illegal_package_segment_or_class_start

        RuleInputValidity.ClassName.ILLEGAL_SEGMENT_PART ->
            R.string.error_illegal_package_segment_or_class_part
    }
    return stringResource(resId)
}

@Composable
fun Rule.Action.icon(): Painter {
    val resId = when (this) {
        Rule.Action.CLICK,
        Rule.Action.CLICK_NEAREST_CLICKABLE_ANCESTOR,
        Rule.Action.CLICK_NEAREST_CLICKABLE_DESCENDANT -> R.drawable.touch_app_outlined

        Rule.Action.MUTE_WHEN_PRESENT -> R.drawable.volume_off_outlined
    }
    return painterResource(resId)
}

@Composable
fun Rule.Action.name(): String {
    val resId = when (this) {
        Rule.Action.CLICK -> R.string.click

        Rule.Action.CLICK_NEAREST_CLICKABLE_ANCESTOR -> R.string.click_nearest_clickable_ancestor

        Rule.Action.CLICK_NEAREST_CLICKABLE_DESCENDANT ->
            R.string.click_nearest_clickable_descendant

        Rule.Action.MUTE_WHEN_PRESENT -> R.string.mute_when_present
    }
    return stringResource(resId)
}

@Composable
fun Rule.Action.explanation(): String {
    val resId = when (this) {
        Rule.Action.CLICK -> R.string.click_explanation

        Rule.Action.CLICK_NEAREST_CLICKABLE_ANCESTOR ->
            R.string.click_nearest_clickable_ancestor_explanation

        Rule.Action.CLICK_NEAREST_CLICKABLE_DESCENDANT ->
            R.string.click_nearest_clickable_descendant_explanation

        Rule.Action.MUTE_WHEN_PRESENT -> R.string.mute_when_present_explanation
    }
    return stringResource(resId)
}
