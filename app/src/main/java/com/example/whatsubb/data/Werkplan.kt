package com.example.whatsubb.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Een werkplan voor één taak (kamer schoonmaken, koken, boodschappen, logeren...).
 * De AI vult alleen de inhoud; opmaak, begeleidingsniveaus, rapportage, aftekenkaart
 * en evaluatieformulier zijn vast (zie WerkplanPdf), zodat elk werkplan er hetzelfde uitziet.
 */
data class Werkplan(
    val taak: String,
    val frequentie: String,
    val vasteDag: String,
    val vasteTijd: String,
    val begeleider: String,
    val duur: String,
    val startdatum: String,
    val evaluatiedatum: String,
    val doel: String,
    val uitgangspunten: List<String>,
    val werkwijze: List<Stap>,
    val volgordeTitel: String,
    val volgorde: List<String>,
    val eindcontrole: List<String>,
    val teamafspraak: String,
    val kaart: Kaart
) {
    data class Stap(val stap: String, val wat: String, val bewoner: String, val begeleiding: String)

    data class KaartTaak(val emoji: String, val taak: String, val watDoeIk: String)

    data class Kaart(val titel: String, val taken: List<KaartTaak>, val hulp: String, val doel: String)

    /** Leesbare versie voor in de app, om te kopiëren of te delen. */
    fun toMarkdown(): String = buildString {
        appendLine("# Werkplan: $taak")
        appendLine()
        appendLine("**Bewoner:** [bewoner]  ")
        appendLine("**Frequentie:** $frequentie · **Vaste dag:** $vasteDag · **Vaste tijd:** $vasteTijd  ")
        appendLine("**Begeleider:** $begeleider · **Duur:** $duur  ")
        appendLine("**Startdatum:** ${startdatum.ifBlank { "…" }} · **Evaluatiedatum:** ${evaluatiedatum.ifBlank { "…" }}")
        appendLine()
        appendLine("## 1. Doel")
        appendLine(doel)
        appendLine()
        appendLine("## 2. Uitgangspunten voor het team")
        uitgangspunten.forEach { appendLine("- $it") }
        appendLine()
        appendLine("## 3. Vaste werkwijze")
        werkwijze.forEachIndexed { i, s ->
            appendLine("- **${i + 1}. ${s.stap}:** ${s.wat} Bewoner: ${s.bewoner} Begeleiding: ${s.begeleiding}")
        }
        appendLine()
        appendLine("## 4. $volgordeTitel")
        volgorde.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
        appendLine()
        appendLine("## 5. Begeleidingsniveau")
        appendLine("Vaste schaal 0 (zelfstandig) t/m 5 (tijdelijk overnemen), zie PDF.")
        appendLine()
        appendLine("## 6. Eindcontrole")
        eindcontrole.forEach { appendLine("- ☐ $it") }
        appendLine()
        appendLine("## 7. Rapportage na afloop")
        appendLine("Vast formulier in de PDF.")
        appendLine()
        appendLine("**Teamafspraak:** $teamafspraak")
        appendLine()
        appendLine("## ${kaart.titel} (voor de bewoner)")
        kaart.taken.forEach { appendLine("- ☐ ${it.emoji} **${it.taak}:** ${it.watDoeIk}") }
        appendLine()
        appendLine("**Hulp nodig?** ${kaart.hulp}  ")
        appendLine("**Mijn doel:** ${kaart.doel}")
        appendLine()
        appendLine("## Evaluatie")
        append("Vast evaluatieformulier op de laatste pagina van de PDF.")
    }

    companion object {

        /** Haalt het werkplan uit het AI-antwoord (ook als er ```json of tekst omheen staat), of null. */
        fun parse(reply: String): Werkplan? = runCatching {
            val start = reply.indexOf('{')
            val end = reply.lastIndexOf('}')
            fromJson(JSONObject(reply.substring(start, end + 1)))
        }.getOrNull()?.takeIf { it.taak.isNotBlank() && it.werkwijze.isNotEmpty() }

        private fun fromJson(o: JSONObject): Werkplan {
            val kaart = o.optJSONObject("kaart") ?: JSONObject()
            return Werkplan(
                taak = o.str("taak"),
                frequentie = o.str("frequentie"),
                vasteDag = o.str("vasteDag"),
                vasteTijd = o.str("vasteTijd"),
                begeleider = o.str("begeleider").ifBlank { "Wisselend" },
                duur = o.str("duur"),
                startdatum = o.str("startdatum"),
                evaluatiedatum = o.str("evaluatiedatum"),
                doel = o.str("doel"),
                uitgangspunten = o.strings("uitgangspunten"),
                werkwijze = o.objects("werkwijze").map {
                    Stap(it.str("stap"), it.str("wat"), it.str("bewoner"), it.str("begeleiding"))
                },
                volgordeTitel = o.str("volgordeTitel").ifBlank { "Vaste volgorde" },
                volgorde = o.strings("volgorde"),
                eindcontrole = o.strings("eindcontrole"),
                teamafspraak = o.str("teamafspraak"),
                kaart = Kaart(
                    titel = kaart.str("titel").ifBlank { "MIJN KAART" }.uppercase(),
                    taken = kaart.objects("taken").map { KaartTaak(it.str("emoji"), it.str("taak"), it.str("watDoeIk")) },
                    hulp = kaart.str("hulp").ifBlank { "Vraag begeleiding om één stap samen te doen." },
                    doel = kaart.str("doel").ifBlank { "Steeds iets meer zelf kunnen." }
                )
            )
        }

        private fun JSONObject.str(key: String) = optString(key, "").trim().let { if (it == "null") "" else it }

        private fun JSONObject.strings(key: String): List<String> {
            val array = optJSONArray(key) ?: JSONArray()
            return (0 until array.length()).map { array.optString(it).trim() }.filter { it.isNotEmpty() }
        }

        private fun JSONObject.objects(key: String): List<JSONObject> {
            val array = optJSONArray(key) ?: JSONArray()
            return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
        }

        /** Opdracht aan de AI; het voorbeeld (assets/werkplan_voorbeeld.json) wordt erachter geplakt. */
        val INSTRUCTIONS = """
            Maak een werkplan voor één concrete taak of situatie van een bewoner (bijv. kamer schoonmaken, koken,
            boodschappen doen, naar huis gaan, logeren). Volg precies de stijl van het voorbeeld hieronder:
            - Simpel en concreet. Korte zinnen. Eén handeling per stap.
            - "werkwijze": 4 tot 7 stappen van aankondigen tot rapporteren. Per stap: wat doen we, de rol van de bewoner
              en de rol van de begeleiding. Heeft de bewoner bij een stap geen rol, schrijf dan "—".
            - "volgorde": de vaste volgorde van de taak zelf, 5 tot 12 korte stappen. Geef een passende "volgordeTitel"
              (bijv. "Vaste schoonmaakvolgorde", "Vaste kookvolgorde", "Vaste volgorde bij vertrek").
            - "uitgangspunten": 5 tot 8 afspraken voor het team (vaste volgorde, korte opdrachten, zelf laten doen,
              positieve feedback, keuzes binnen kader, wat te doen bij spanning), afgestemd op deze taak.
            - "eindcontrole": korte controlepunten die je kunt aftekenen.
            - "kaart": de aftekenkaart voor de bewoner, in de ik-vorm en op B1-niveau. "titel" in hoofdletters
              (bijv. "MIJN SCHOONMAAKKAART", "MIJN KOOKKAART"). 5 tot 10 taken, elk met één passende emoji,
              een korte taak ("Ik ...") en "watDoeIk" in een paar woorden.
            - Gebruik nooit een naam van de bewoner; de bewoner heet altijd [bewoner] of "de bewoner".
            - Datums: gebruik de datum van vandaag als startdatum als er niets is opgegeven. Bereken de evaluatiedatum
              uit de opgegeven termijn (schrijf als "29 november 2026"). Weet je iets niet, laat het veld dan leeg ("").
            - Verzin geen afspraken die niet passen bij de gegevens of de kennis van de afdeling.

            Antwoord met ALLEEN geldige JSON (geen uitleg, geen Markdown), met precies deze velden:
            taak, frequentie, vasteDag, vasteTijd, begeleider, duur, startdatum, evaluatiedatum, doel,
            uitgangspunten[], werkwijze[{stap, wat, bewoner, begeleiding}], volgordeTitel, volgorde[],
            eindcontrole[], teamafspraak, kaart{titel, taken[{emoji, taak, watDoeIk}], hulp, doel}.
        """.trimIndent()

        /** Vaste schaal voor de hoeveelheid ondersteuning (in PDF en Word). */
        val BEGELEIDINGSNIVEAUS = listOf(
            "0 – Zelfstandig" to "Bewoner voert de taak zelfstandig uit.",
            "1 – Reminder" to "Alleen een korte herinnering is nodig.",
            "2 – Mondelinge instructie" to "Begeleiding vertelt concreet wat de bewoner moet doen.",
            "3 – Voordoen" to "Begeleiding doet de handeling voor; bewoner doet deze daarna zelf.",
            "4 – Samen doen" to "Bewoner en begeleiding voeren de taak samen uit.",
            "5 – Tijdelijk overnemen" to "Begeleiding neemt de taak tijdelijk over omdat de bewoner deze op dat moment niet kan uitvoeren."
        )

        const val EXAMPLE_ASSET = "werkplan_voorbeeld.json"
    }
}
