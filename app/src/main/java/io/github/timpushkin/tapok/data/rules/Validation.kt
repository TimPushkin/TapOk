package io.github.timpushkin.tapok.data.rules

data class RuleInputValidity(
    val name: Name,
    val packageName: PackageName,
    val viewId: ViewId,
    val containedText: ContainedText,
    val className: ClassName
) {
    val isValid = name == Name.OK &&
            packageName == PackageName.OK &&
            ((viewId == ViewId.OK && !isError(containedText)) ||
                    (containedText == ContainedText.OK && !isError(viewId))) &&
            !isError(className)

    private fun isError(validity: Enum<*>?) = validity != null && validity.ordinal != 0 // OK is 0

    fun relationAwareCopy(
        name: Name = this.name,
        packageName: PackageName = this.packageName,
        viewId: ViewId = this.viewId,
        containedText: ContainedText = this.containedText,
        className: ClassName = this.className
    ) = copy(
        name = name.takeIfRetainedOnChangeOf(
            packageName = packageName != this.packageName,
            viewId = viewId != this.viewId,
            containedText = containedText != this.containedText,
            className = className != this.className
        ) ?: Name.OK,
        packageName = packageName.takeIfRetainedOnChangeOf(
            name = name != this.name,
            viewId = viewId != this.viewId,
            containedText = containedText != this.containedText,
            className = className != this.className
        ) ?: PackageName.OK,
        viewId = viewId.takeIfRetainedOnChangeOf(
            name = name != this.name,
            packageName = packageName != this.packageName,
            containedText = containedText != this.containedText,
            className = className != this.className
        ) ?: ViewId.OK,
        containedText = containedText.takeIfRetainedOnChangeOf(
            name = name != this.name,
            packageName = packageName != this.packageName,
            viewId = viewId != this.viewId,
            className = className != this.className
        ) ?: ContainedText.OK,
        className = className.takeIfRetainedOnChangeOf(
            name = name != this.name,
            packageName = packageName != this.packageName,
            viewId = viewId != this.viewId,
            containedText = containedText != this.containedText
        ) ?: ClassName.OK
    )

    companion object {
        val AllOK =
            RuleInputValidity(Name.OK, PackageName.OK, ViewId.OK, ContainedText.OK, ClassName.OK)
    }

    enum class Name {
        OK;

        fun takeIfRetainedOnChangeOf(
            packageName: Boolean = false,
            viewId: Boolean = false,
            containedText: Boolean = false,
            className: Boolean = false
        ): Name? = this
    }

    enum class PackageName {
        OK,
        EMPTY,
        EMPTY_SEGMENT,
        ILLEGAL_SEGMENT_START,
        ILLEGAL_SEGMENT_PART,
        NOT_ENOUGH_SEGMENTS;

        fun takeIfRetainedOnChangeOf(
            name: Boolean = false,
            viewId: Boolean = false,
            containedText: Boolean = false,
            className: Boolean = false
        ): PackageName? = this
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
        VIEW_ILLEGAL_PART;

        fun takeIfRetainedOnChangeOf(
            name: Boolean = false,
            packageName: Boolean = false,
            containedText: Boolean = false,
            className: Boolean = false
        ): ViewId? = takeIf { !containedText }
    }

    enum class ContainedText {
        OK,
        EMPTY_WITH_NO_ALTERNATIVE;

        fun takeIfRetainedOnChangeOf(
            name: Boolean = false,
            packageName: Boolean = false,
            viewId: Boolean = false,
            className: Boolean = false
        ): ContainedText? = takeIf { !viewId }
    }

    enum class ClassName {
        OK,
        EMPTY_SEGMENT,
        ILLEGAL_SEGMENT_START,
        ILLEGAL_SEGMENT_PART;

        fun takeIfRetainedOnChangeOf(
            name: Boolean = false,
            packageName: Boolean = false,
            viewId: Boolean = false,
            containedText: Boolean = false
        ): ClassName? = this
    }
}

fun validateRule(
    name: CharSequence,
    packageName: CharSequence,
    viewId: CharSequence,
    containedText: CharSequence,
    className: CharSequence
): RuleInputValidity {
    val nameValidity = validateRuleName(name)
    val packageNameValidity = validateRulePackageName(packageName)
    val viewIdValidity = validateRuleViewId(viewId)
    val containedTextValidity = validateRuleContainedText(containedText)
    val classNameValidity = validateRuleClassName(className)
    val hasSearchCriterion = viewId.isNotEmpty() || containedText.isNotEmpty()
    return RuleInputValidity(
        nameValidity,
        packageNameValidity,
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
        classNameValidity
    )
}

fun validateRuleName(str: CharSequence): RuleInputValidity.Name = RuleInputValidity.Name.OK

fun validateRulePackageName(str: CharSequence): RuleInputValidity.PackageName {
    if (str.isEmpty()) {
        return RuleInputValidity.PackageName.EMPTY
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

fun validateRuleClassName(str: CharSequence): RuleInputValidity.ClassName {
    if (!str.isEmpty()) {
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
