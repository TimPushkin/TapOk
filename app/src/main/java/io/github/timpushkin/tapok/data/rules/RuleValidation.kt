package io.github.timpushkin.tapok.data.rules

enum class RuleNameValidity {
    /** Is valid. */
    OK,

    /** Is empty or blank. */
    EMPTY,
}

fun validateRuleName(str: CharSequence): RuleNameValidity =
    if (str.isEmpty()) RuleNameValidity.EMPTY else RuleNameValidity.OK

enum class ViewIdValidity {
    /** Is valid. */
    OK,

    /** No :id/ part. */
    NO_DIVIDER,

    /**
     * Has a segment that is either empty, contains non-alphanumeric characters, or starts with a
     * non-letter character.
     */
    PACKAGE_ILLEGAL_SEGMENT,

    /** Has less than two segments. */
    PACKAGE_NOT_ENOUGH_SEGMENTS,

    /** First character is not a "Java letter", including when there is no first character. */
    VIEW_ILLEGAL_START,

    /** Character other than first is not a "Java letter-or-digit". */
    VIEW_ILLEGAL_PART,
}

fun validateViewId(str: CharSequence): ViewIdValidity {
    val (packageName, viewName) = str.split(":id/", limit = 2).apply {
        if (size < 2) return ViewIdValidity.NO_DIVIDER
    }
    val packageValidity = validatePackageName(packageName)
    if (packageValidity != ViewIdValidity.OK) {
        return packageValidity
    }
    val viewValidity = validateViewName(viewName)
    if (viewValidity != ViewIdValidity.OK) {
        return viewValidity
    }
    return ViewIdValidity.OK
}

private fun validatePackageName(str: CharSequence): ViewIdValidity {
    var numSegments = 0
    for (segment in str.splitToSequence('.')) {
        numSegments++
        if (segment.isEmpty()) {
            return ViewIdValidity.PACKAGE_ILLEGAL_SEGMENT
        }
        segment.forEachIndexed { i, c ->
            if ((i == 0 && !c.isLetter()) || (i > 0 && !c.isLetterOrDigit() && c != '_')) {
                return ViewIdValidity.PACKAGE_ILLEGAL_SEGMENT
            }
        }
    }
    if (numSegments < 2) {
        return ViewIdValidity.PACKAGE_NOT_ENOUGH_SEGMENTS
    }
    return ViewIdValidity.OK
}

private fun validateViewName(str: CharSequence): ViewIdValidity {
    if (str.isEmpty() || !str.first().isJavaIdentifierStart()) {
        return ViewIdValidity.VIEW_ILLEGAL_START
    }
    for (c in str.subSequence(1..<str.length)) {
        if (!c.isJavaIdentifierPart()) {
            return ViewIdValidity.VIEW_ILLEGAL_PART
        }
    }
    return ViewIdValidity.OK
}
