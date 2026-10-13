package io.github.timpushkin.tapok.data.rules

data class RuleInputValidity(
    val name: Name,
    val sourcePackageName: SourcePackageName,
    val viewId: ViewId,
    val containedText: ContainedText,
    val packageName: PackageName,
    val className: ClassName
) {
    val isValid = name == Name.OK && sourcePackageName == SourcePackageName.OK &&
            ((viewId == ViewId.OK && !isError(containedText)) ||
                    (containedText == ContainedText.OK && !isError(viewId))) &&
            !isError(packageName) && !isError(className)

    private fun isError(validity: Enum<*>?) = validity != null && validity.ordinal != 0 // OK is 0

    fun relationAwareCopy(
        name: Name = this.name,
        sourcePackageName: SourcePackageName = this.sourcePackageName,
        viewId: ViewId = this.viewId,
        containedText: ContainedText = this.containedText,
        packageName: PackageName = this.packageName,
        className: ClassName = this.className
    ) = copy(
        name = name,
        sourcePackageName = sourcePackageName,
        viewId = viewId.takeUnless { containedText != this.containedText } ?: ViewId.OK,
        containedText = containedText.takeUnless { viewId != this.viewId } ?: ContainedText.OK,
        packageName = packageName,
        className = className
    )

    companion object {
        val AllOK = RuleInputValidity(
            Name.OK, SourcePackageName.OK, ViewId.OK, ContainedText.OK, PackageName.OK, ClassName.OK
        )
    }

    enum class Name { OK }

    enum class SourcePackageName {
        OK,
        EMPTY,
        EMPTY_SEGMENT,
        ILLEGAL_SEGMENT_START,
        ILLEGAL_SEGMENT_PART,
        NOT_ENOUGH_SEGMENTS
    }

    enum class ViewId {
        OK,

        EMPTY_WITH_NO_ALTERNATIVE,

        NO_DIVIDER,

        PACKAGE_EMPTY,
        PACKAGE_EMPTY_SEGMENT,
        PACKAGE_ILLEGAL_SEGMENT_START,
        PACKAGE_ILLEGAL_SEGMENT_PART,

        VIEW_EMPTY,
        VIEW_ILLEGAL_START,
        VIEW_ILLEGAL_PART
    }

    enum class ContainedText {
        OK,
        EMPTY_WITH_NO_ALTERNATIVE
    }

    enum class PackageName {
        OK,
        EMPTY_SEGMENT,
        ILLEGAL_SEGMENT_START,
        ILLEGAL_SEGMENT_PART,
        NOT_ENOUGH_SEGMENTS
    }

    enum class ClassName {
        OK,
        EMPTY_SEGMENT,
        ILLEGAL_SEGMENT_START,
        ILLEGAL_SEGMENT_PART
    }
}

fun validateRule(
    name: CharSequence,
    sourcePackageName: CharSequence,
    viewId: CharSequence,
    containedText: CharSequence,
    packageName: CharSequence,
    className: CharSequence
): RuleInputValidity {
    val nameValidity = validateRuleName(name)
    val sourcePackageNameValidity = validateRuleSourcePackageName(sourcePackageName)
    val viewIdValidity = validateRuleViewId(viewId)
    val containedTextValidity = validateRuleContainedText(containedText)
    val packageNameValidity = validateRulePackageName(packageName)
    val classNameValidity = validateRuleClassName(className)
    val hasSearchCriterion = viewId.isNotEmpty() || containedText.isNotEmpty()
    return RuleInputValidity(
        nameValidity,
        sourcePackageNameValidity,
        if (hasSearchCriterion) {
            viewIdValidity
        } else {
            RuleInputValidity.ViewId.EMPTY_WITH_NO_ALTERNATIVE
        },
        if (hasSearchCriterion) {
            containedTextValidity
        } else {
            RuleInputValidity.ContainedText.EMPTY_WITH_NO_ALTERNATIVE
        },
        packageNameValidity,
        classNameValidity
    )
}

fun validateRuleName(str: CharSequence): RuleInputValidity.Name = RuleInputValidity.Name.OK

fun validateRuleSourcePackageName(str: CharSequence): RuleInputValidity.SourcePackageName {
    if (str.isEmpty()) {
        return RuleInputValidity.SourcePackageName.EMPTY
    }
    return when (validateRulePackageName(str)) {
        RuleInputValidity.PackageName.OK -> RuleInputValidity.SourcePackageName.OK

        RuleInputValidity.PackageName.EMPTY_SEGMENT ->
            RuleInputValidity.SourcePackageName.EMPTY_SEGMENT

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_START ->
            RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_START

        RuleInputValidity.PackageName.ILLEGAL_SEGMENT_PART ->
            RuleInputValidity.SourcePackageName.ILLEGAL_SEGMENT_PART

        RuleInputValidity.PackageName.NOT_ENOUGH_SEGMENTS ->
            RuleInputValidity.SourcePackageName.NOT_ENOUGH_SEGMENTS
    }
}

fun validateRuleViewId(str: CharSequence): RuleInputValidity.ViewId {
    if (str.isEmpty()) {
        return RuleInputValidity.ViewId.OK // Depends on other fields
    }

    val (packageName, viewName) = str.split(":id/", limit = 2).apply {
        if (size < 2) {
            return RuleInputValidity.ViewId.NO_DIVIDER
        }
    }

    if (packageName.isEmpty()) {
        return RuleInputValidity.ViewId.PACKAGE_EMPTY
    }
    for (segment in packageName.splitToSequence('.')) {
        return when (validateJavaIdentifier(segment)) {
            JavaIdentifierInputValidity.OK -> continue
            JavaIdentifierInputValidity.EMPTY ->
                RuleInputValidity.ViewId.PACKAGE_EMPTY_SEGMENT

            JavaIdentifierInputValidity.ILLEGAL_START ->
                RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_START

            JavaIdentifierInputValidity.ILLEGAL_PART ->
                RuleInputValidity.ViewId.PACKAGE_ILLEGAL_SEGMENT_PART
        }
    }

    return when (validateJavaIdentifier(viewName)) {
        JavaIdentifierInputValidity.OK -> RuleInputValidity.ViewId.OK
        JavaIdentifierInputValidity.EMPTY -> RuleInputValidity.ViewId.VIEW_EMPTY
        JavaIdentifierInputValidity.ILLEGAL_START -> RuleInputValidity.ViewId.VIEW_ILLEGAL_START
        JavaIdentifierInputValidity.ILLEGAL_PART -> RuleInputValidity.ViewId.VIEW_ILLEGAL_PART
    }
}

fun validateRuleContainedText(str: CharSequence): RuleInputValidity.ContainedText =
    RuleInputValidity.ContainedText.OK // Depends on other fields

fun validateRulePackageName(str: CharSequence): RuleInputValidity.PackageName {
    if (str.isEmpty()) {
        return RuleInputValidity.PackageName.OK
    }
    // https://developer.android.com/build/configure-app-module#set-application-id
    var numSegments = 0
    for (segment in str.splitToSequence('.')) {
        numSegments++
        if (segment.isEmpty()) {
            return RuleInputValidity.PackageName.EMPTY_SEGMENT
        }
        segment.forEachIndexed { i, c ->
            if (i == 0 && !c.isLetter()) {
                return RuleInputValidity.PackageName.ILLEGAL_SEGMENT_START
            }
            if (i > 0 && !c.isLetterOrDigit() && c != '_') {
                return RuleInputValidity.PackageName.ILLEGAL_SEGMENT_PART
            }
        }
    }
    return if (numSegments < 2) {
        RuleInputValidity.PackageName.NOT_ENOUGH_SEGMENTS
    } else {
        RuleInputValidity.PackageName.OK
    }
}

fun validateRuleClassName(str: CharSequence): RuleInputValidity.ClassName {
    if (str.isNotEmpty()) {
        for (segment in str.splitToSequence('.')) {
            return when (validateJavaIdentifier(segment)) {
                JavaIdentifierInputValidity.OK -> continue
                JavaIdentifierInputValidity.EMPTY ->
                    RuleInputValidity.ClassName.EMPTY_SEGMENT

                JavaIdentifierInputValidity.ILLEGAL_START ->
                    RuleInputValidity.ClassName.ILLEGAL_SEGMENT_START

                JavaIdentifierInputValidity.ILLEGAL_PART ->
                    RuleInputValidity.ClassName.ILLEGAL_SEGMENT_PART
            }
        }
    }
    return RuleInputValidity.ClassName.OK
}

private enum class JavaIdentifierInputValidity { OK, EMPTY, ILLEGAL_START, ILLEGAL_PART }

private fun validateJavaIdentifier(str: CharSequence): JavaIdentifierInputValidity {
    if (str.isEmpty()) {
        return JavaIdentifierInputValidity.EMPTY
    }
    if (!str.first().isJavaIdentifierStart()) {
        return JavaIdentifierInputValidity.ILLEGAL_START
    }
    if (str.subSequence(1..<str.length).any { !it.isJavaIdentifierPart() }) {
        return JavaIdentifierInputValidity.ILLEGAL_PART
    }
    return JavaIdentifierInputValidity.OK
}
