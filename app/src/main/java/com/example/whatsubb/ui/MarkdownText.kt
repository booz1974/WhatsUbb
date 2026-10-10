package com.example.whatsubb.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.whatsubb.util.Markdown

/** Zet AI-tekst met eenvoudige Markdown om naar opgemaakte tekst voor het scherm. */
fun markdownToAnnotated(markdown: String): AnnotatedString = buildAnnotatedString {
    val lines = Markdown.parse(markdown)
    lines.forEachIndexed { index, line ->
        when (line.kind) {
            Markdown.Kind.HEADING1 -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                append(line.text.replace("**", ""))
            }
            Markdown.Kind.HEADING2 -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                append(line.text.replace("**", ""))
            }
            Markdown.Kind.BULLET -> {
                append("•  ")
                appendInline(line.text)
            }
            Markdown.Kind.PARAGRAPH -> appendInline(line.text)
            Markdown.Kind.BLANK -> Unit
        }
        if (index < lines.lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInline(text: String) {
    Markdown.inline(text).forEach { segment ->
        if (segment.bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment.text) }
        else append(segment.text)
    }
}
