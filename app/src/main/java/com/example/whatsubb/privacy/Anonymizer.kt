package com.example.whatsubb.privacy

/**
 * Haalt herleidbare gegevens uit tekst VOORDAT die naar de AI gaat.
 * Dit is een vangnet, geen garantie: blijf zelf anoniem schrijven.
 */
object Anonymizer {

    data class Result(val text: String, val replacements: Int)

    private val patterns: List<Pair<Regex, String>> = listOf(
        Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""") to "[e-mail]",
        Regex("""\bNL\d{2}[A-Z]{4}\d{10}\b""", RegexOption.IGNORE_CASE) to "[iban]",
        Regex("""(?:\+31|0031|\b0)[\s-]?(?:\d[\s-]?){9}\b""") to "[telefoon]",
        Regex("""\b\d{9}\b""") to "[bsn]",
        Regex("""\b\d{4}\s?[A-Z]{2}\b""") to "[postcode]",
        Regex("""\b\d{1,2}[-/.]\d{1,2}[-/.](?:\d{4}|\d{2})\b""") to "[datum]",
        Regex(
            """\b\d{1,2}\s+(?:jan|feb|mrt|maart|apr|april|mei|jun|juni|jul|juli|aug|sep|sept|okt|nov|dec)[a-z]*\.?\s+\d{4}\b""",
            RegexOption.IGNORE_CASE
        ) to "[datum]",
        Regex(
            """\b[A-Z][a-z]+(?:straat|laan|weg|plein|singel|gracht|kade|dreef|hof|pad)\s+\d+[a-zA-Z]?\b"""
        ) to "[adres]",
    )

    /**
     * @param extraNames namen (of bijnamen) die de gebruiker opgeeft; die worden
     *                   overal vervangen door [naam], ongeacht hoofdletters.
     */
    fun scrub(input: String, extraNames: List<String> = emptyList()): Result {
        var text = input
        var count = 0

        extraNames
            .map { it.trim() }
            .filter { it.length >= 2 }
            .sortedByDescending { it.length }
            .forEach { name ->
                val regex = Regex("""\b${Regex.escape(name)}\b""", RegexOption.IGNORE_CASE)
                count += regex.findAll(text).count()
                text = regex.replace(text, "[naam]")
            }

        for ((regex, label) in patterns) {
            count += regex.findAll(text).count()
            text = regex.replace(text, label)
        }
        return Result(text, count)
    }

    fun parseNames(raw: String): List<String> =
        raw.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
}
