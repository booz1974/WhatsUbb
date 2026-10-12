package com.example.whatsubb.data

data class DocField(
    val label: String,
    val hint: String,
    val multiline: Boolean = true
)

data class DocumentType(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val instructions: String,
    val fields: List<DocField>,
    /** Als dit is ingesteld, kan een gesprek worden opgenomen; de transcriptie komt in dit veld. */
    val transcriptFieldIndex: Int? = null,
    /** De AI geeft een [Werkplan] als JSON terug; de PDF krijgt de vaste werkplan-opmaak (WerkplanPdf). */
    val werkplan: Boolean = false
)

/** De dingen die een begeleider met de AI kan maken. Breid deze lijst gerust uit. */
val documentTypes = listOf(
    DocumentType(
        id = "werkplan",
        emoji = "🗺️",
        title = "Werkplan",
        description = "Vaste aanpak voor een taak, met aftekenkaart en evaluatie",
        instructions = Werkplan.INSTRUCTIONS,
        fields = listOf(
            DocField("Taak of onderwerp", "Bijv. kamer schoonmaken, koken, boodschappen doen, naar huis gaan, logeren", multiline = false),
            DocField("Wanneer en hoe vaak?", "Bijv. elke vrijdag om 10.30, ± 45 minuten", multiline = false),
            DocField("Doel", "Wat moet de bewoner (meer) zelf leren of kunnen?"),
            DocField("Hoe gaat het nu?", "Wat lukt al, waar is hulp bij nodig, wat werkt wel of niet?"),
            DocField("Aandachtspunten", "Bijv. spanning, prikkels, medicatie, afspraken met thuis"),
            DocField("Start en evaluatie", "Bijv. start volgende week, evaluatie over 6 weken", multiline = false)
        ),
        werkplan = true
    ),
    DocumentType(
        id = "rapportage",
        emoji = "📝",
        title = "Dagrapportage",
        description = "Steekwoorden omzetten naar een nette rapportage",
        instructions = """
            Zet de steekwoorden om in een korte, objectieve dagrapportage.
            Scheid observaties (wat zag/hoorde je) van interpretaties. Gebruik geen oordelende taal.
            Eindig met eventuele aandachtspunten voor de volgende dienst.
        """.trimIndent(),
        fields = listOf(
            DocField("Dienst", "Vroege / late / nacht", multiline = false),
            DocField("Observaties (steekwoorden)", "Stemming, contact, dagbesteding, bijzonderheden..."),
            DocField("Aandachtspunten volgende dienst", "Optioneel")
        )
    ),
    DocumentType(
        id = "verslag",
        emoji = "🗣️",
        title = "Gespreksverslag",
        description = "Verslag van een gesprek, MDO of evaluatie",
        instructions = """
            Maak een gespreksverslag met: aanleiding, besproken punten, standpunt van de bewoner,
            gemaakte afspraken (wie doet wat, wanneer) en vervolg.
        """.trimIndent(),
        fields = listOf(
            DocField("Soort gesprek", "Bijv. evaluatie, MDO, mentorgesprek", multiline = false),
            DocField("Aanwezigen (rollen, geen namen)", "Bijv. bewoner, mentor, behandelaar", multiline = false),
            DocField("Aantekeningen / transcriptie", "Wat is er besproken? Of neem het gesprek op."),
            DocField("Afspraken", "Wat is er afgesproken?")
        ),
        transcriptFieldIndex = 2
    ),
    DocumentType(
        id = "signaleringsplan",
        emoji = "🚦",
        title = "Signaleringsplan",
        description = "Signalen en acties per fase (groen/oranje/rood)",
        instructions = """
            Maak een signaleringsplan met de fasen GROEN (stabiel), ORANJE (spanning loopt op),
            ROOD (crisis) en eventueel NAZORG. Geef per fase: signalen bij de bewoner,
            wat de bewoner zelf kan doen, en wat de begeleiding doet. Gebruik een tabel-achtige opbouw.
        """.trimIndent(),
        fields = listOf(
            DocField("Hoe gaat het als het goed gaat?", "Gedrag, dagritme, contact"),
            DocField("Vroege signalen van spanning", "Wat merk je als het minder gaat?"),
            DocField("Signalen van crisis", "Wat gebeurt er bij escalatie?"),
            DocField("Wat helpt?", "Wat werkt de-escalerend?")
        )
    ),
    DocumentType(
        id = "vrij",
        emoji = "✨",
        title = "Vrije opdracht",
        description = "Brief, uitleg, dagprogramma, of iets anders",
        instructions = "Voer de opdracht van de begeleider uit.",
        fields = listOf(
            DocField("Opdracht", "Bijv. 'Maak een eenvoudige uitleg van de bezoekregels voor een nieuwe bewoner'")
        )
    )
)
