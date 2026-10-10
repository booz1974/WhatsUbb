package com.example.whatsubb

import com.example.whatsubb.util.Markdown
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun parsesHeadingsBulletsAndTables() {
        val lines = Markdown.parse("# Werkplan\n## Doelen\n- eerste\n1. stap\n| a | b |\n|---|---|\n| c | d |")
        assertEquals(
            listOf(
                Markdown.Line(Markdown.Kind.HEADING1, "Werkplan"),
                Markdown.Line(Markdown.Kind.HEADING2, "Doelen"),
                Markdown.Line(Markdown.Kind.BULLET, "eerste"),
                Markdown.Line(Markdown.Kind.PARAGRAPH, "1. stap"),
                Markdown.Line(Markdown.Kind.PARAGRAPH, "a  ·  b"),
                Markdown.Line(Markdown.Kind.PARAGRAPH, "c  ·  d")
            ),
            lines
        )
    }

    @Test
    fun splitsBold() {
        assertEquals(
            listOf(Markdown.Segment("Doel: ", false), Markdown.Segment("rust", true), Markdown.Segment(" vinden", false)),
            Markdown.inline("Doel: **rust** vinden")
        )
    }
}
