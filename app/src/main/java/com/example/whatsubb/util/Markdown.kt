package com.example.whatsubb.util

/**
 * Heel eenvoudige Markdown-lezer voor de antwoorden van de AI:
 * kopjes (#), opsommingen (- * 1.), **vet** en tabellen (| a | b |).
 */
object Markdown {

    enum class Kind { HEADING1, HEADING2, BULLET, PARAGRAPH, BLANK }

    data class Line(val kind: Kind, val text: String)

    data class Segment(val text: String, val bold: Boolean)

    private val numbered = Regex("""^(\d+)[.)]\s+(.*)""")
    private val tableSeparator = Regex("""^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$""")

    fun parse(markdown: String): List<Line> =
        markdown.lines().mapNotNull { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> Line(Kind.BLANK, "")
                tableSeparator.matches(trimmed) -> null
                trimmed.startsWith("|") ->
                    Line(Kind.PARAGRAPH, trimmed.trim('|').split('|').joinToString("  ·  ") { it.trim() })
                trimmed.startsWith("# ") -> Line(Kind.HEADING1, trimmed.removePrefix("# ").trim())
                trimmed.startsWith("#") -> Line(Kind.HEADING2, trimmed.trimStart('#').trim())
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") ->
                    Line(Kind.BULLET, trimmed.substring(2).trim())
                numbered.matches(trimmed) -> {
                    val (number, text) = numbered.find(trimmed)!!.destructured
                    Line(Kind.PARAGRAPH, "$number. $text")
                }
                else -> Line(Kind.PARAGRAPH, trimmed)
            }
        }

    /** Splitst een regel in stukken met en zonder **vet**. */
    fun inline(text: String): List<Segment> {
        val parts = text.split("**")
        return parts.mapIndexedNotNull { index, part ->
            if (part.isEmpty()) null else Segment(part.replace("__", ""), bold = index % 2 == 1)
        }
    }
}
