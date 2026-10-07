package io.github.timpushkin.tapok.ui.utils

import android.graphics.Typeface
import android.text.Annotation
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import androidx.annotation.ArrayRes
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

@Composable
fun annotatedStringArrayResource(@ArrayRes id: Int) =
    LocalResources.current.getTextArray(id)

@Composable
inline fun rememberAnnotatedString(
    vararg keys: Any?,
    crossinline builder: AnnotatedString.Builder.() -> Unit
): AnnotatedString = remember(keys = keys) { buildAnnotatedString(builder) }

fun AnnotatedString.Builder.appendAnnotatedParagraphs(
    paragraphs: Array<out CharSequence>,
    links: Iterable<LinkAnnotation.Clickable> = emptyList()
) {
    paragraphs.forEachIndexed { i, paragraph ->
        appendAnnotated(paragraph, links)
        if (i != paragraphs.lastIndex) appendParagraphSpacer()
    }
}

// Value is the link's tag
private const val ANNOTATION_KEY_LINK = "link"

// Value is inline content ID
private const val ANNOTATION_KEY_INLINE = "inline"

private fun AnnotatedString.Builder.appendAnnotated(
    text: CharSequence,
    links: Iterable<LinkAnnotation.Clickable> = emptyList()
) {
    if (text !is Spanned) {
        append(text)
        return
    }

    val textStart = length

    // <annotation> tags
    val linksMap = links.associateBy { it.tag }
    var lastAnnotationEnd = 0
    text.getSpans(0, text.length, Annotation::class.java)
        .map { Triple(it, text.getSpanStart(it), text.getSpanEnd(it)) }
        .sortedBy { (_, start, _) -> start }
        .forEach { (annotation, start, end) ->
            if (start < lastAnnotationEnd) {
                throw UnsupportedOperationException("Nested <annotation> tags are not supported")
            }

            append(text.substring(lastAnnotationEnd, start))
            lastAnnotationEnd = end

            val annotationText = text.substring(start, end)
            when (annotation.key) {
                ANNOTATION_KEY_LINK -> withLink(linksMap.getValue(annotation.value)) {
                    append(annotationText)
                }

                ANNOTATION_KEY_INLINE -> appendInlineContent(
                    annotation.value, alternateText = annotationText
                )

                else -> throw UnsupportedOperationException(
                    "Unsupported annotation key ${annotation.key}"
                )
            }
        }
    append(text.substring(lastAnnotationEnd))

    // HTML styling tags (<b>, <i>...)
    for (span in text.getSpans(0, text.length, CharacterStyle::class.java)) {
        // More styles can easily be supported if a need arises in the future
        if (span !is StyleSpan) {
            throw UnsupportedOperationException("$span is not supported")
        }
        val style = when (span.style) {
            Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            else -> throw UnsupportedOperationException("${span.style} is not supported")
        }
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        addStyle(style, textStart + start, textStart + end)
    }

    // HTML list tags (<ul>, <li>)
    for (span in text.getSpans(0, text.length, BulletSpan::class.java)) {
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        addBullet(Bullet.Default, Bullet.DefaultIndentation, textStart + start, textStart + end)
    }
}

private fun AnnotatedString.Builder.appendParagraphSpacer() {
    withStyle(ParagraphStyle(lineHeight = 0.75.em)) {}
}

@Composable
fun rememberTextLinkStyles(): TextLinkStyles {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    return remember(primaryColor, onPrimaryColor) {
        TextLinkStyles(
            // Default
            style = SpanStyle(
                color = primaryColor,
                textDecoration = TextDecoration.Underline
            ),
            // By default, this is the same as above which makes it not possible to see that it is
            // focused
            focusedStyle = SpanStyle(
                color = onPrimaryColor,
                background = primaryColor,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}
