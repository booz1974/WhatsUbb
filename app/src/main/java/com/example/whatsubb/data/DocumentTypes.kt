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
    val transcriptFieldIndex: Int? = null
)

/** De dingen die een begeleider met de AI kan maken. Breid deze lijst gerust uit. */
val documentTypes = listOf(
    DocumentType(
        id = "werkplan",
        emoji = "🗺️",
        title = "Werkplan",
        description = "Doelen, acties en afspraken voor een bewoner",
        instructions = """
            Maak een werkplan/begeleidingsplan. Gebruik deze opbouw:
            1. Hulpvraag (in de woorden van de bewoner waar mogelijk)
            2. Sterke kanten en beschermende factoren
            3. Doelen: formuleer elk doel SMART
            4. Acties per doel: wat doet de bewoner, wat doet de begeleiding
            5. Afspraken en aandachtspunten in de omgang
            6. Evaluatiemoment
        """.trimIndent(),
        fields = listOf(
            DocField("Hulpvraag / situatie", "Waar loopt de bewoner tegenaan? Wat wil de bewoner bereiken?"),
            DocField("Sterke kanten", "Wat gaat goed, wat helpt?"),
            DocField("Gewenste doelen", "Ruwe doelen, de AI maakt ze SMART"),
            DocField("Aandachtspunten", "Risico's, triggers, wat werkt wel/niet"),
            DocField("Evaluatietermijn", "Bijv. over 6 weken", multiline = false)
        )
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
        id = "incident",
        emoji = "⚠️",
        title = "Incidentmelding",
        description = "Feitelijke beschrijving voor een MIC/VIM-melding",
        instructions = """
            Maak een feitelijke incidentbeschrijving: situatie vooraf, aanleiding, verloop,
            interventies van de begeleiding, afloop, nazorg en lering/verbeterpunten.
            Strikt objectief, in chronologische volgorde.
        """.trimIndent(),
        fields = listOf(
            DocField("Wat is er gebeurd?", "Zo feitelijk mogelijk"),
            DocField("Aanleiding / situatie vooraf", "Wat ging eraan vooraf?"),
            DocField("Interventies", "Wat heeft de begeleiding gedaan?"),
            DocField("Afloop en nazorg", "Hoe is het afgelopen?")
        )
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
