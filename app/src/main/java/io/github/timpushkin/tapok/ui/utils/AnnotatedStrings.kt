package io.github.timpushkin.tapok.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

fun AnnotatedString.Builder.appendParagraphSpacer() {
    withStyle(ParagraphStyle(lineHeight = 0.75.em)) {}
}
