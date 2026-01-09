package com.github.timpushkin.tapok.data.rules

enum class RuleNameValidity {
    /** Is valid. */
    OK,

    /** Is empty or blank. */
    EMPTY,
}

fun validateRuleName(str: CharSequence): RuleNameValidity =
    if (str.isEmpty()) RuleNameValidity.EMPTY else RuleNameValidity.OK

enum class PackageNameValidity {
    /** Is valid. */
    OK,

    /**
     * has a segment that is either empty, contains non-alphanumeric characters, or starts with a
     * non-letter character.
     */
    ILLEGAL_SEGMENT,

    /** Has less than two segments. */
    NOT_ENOUGH_SEGMENTS,
}

fun validatePackageName(str: CharSequence): PackageNameValidity {
    var numSegments = 0
    for (segment in str.splitToSequence('.')) {
        numSegments++
        if (segment.isEmpty()) {
            return PackageNameValidity.ILLEGAL_SEGMENT
        }
        segment.forEachIndexed { i, c ->
            if ((i == 0 && !c.isLetter()) || (i > 0 && !c.isLetterOrDigit() && c != '_')) {
                return PackageNameValidity.ILLEGAL_SEGMENT
            }
        }
    }
    if (numSegments < 2) {
        return PackageNameValidity.NOT_ENOUGH_SEGMENTS
    }
    return PackageNameValidity.OK
}

enum class ViewNameValidity {
    /** Is valid. */
    OK,

    /** Is empty. */
    EMPTY,

    /** First character is not a "Java letter". */
    ILLEGAL_START,

    /** Character other than first is not a "Java letter-or-digit". */
    ILLEGAL_PART,
}

fun validateViewName(str: CharSequence): ViewNameValidity {
    if (str.isEmpty()) {
        return ViewNameValidity.EMPTY
    }
    if (!str.first().isJavaIdentifierStart()) {
        return ViewNameValidity.ILLEGAL_START
    }
    for (c in str.subSequence(1..<str.length)) {
        if (!c.isJavaIdentifierPart()) {
            return ViewNameValidity.ILLEGAL_PART
        }
    }
    return ViewNameValidity.OK
}
