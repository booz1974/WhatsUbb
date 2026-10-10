package com.example.whatsubb.data

import android.content.Context
import java.io.File

enum class Role { BEWONER, BEGELEIDER }

/**
 * De "Gem": de vaste kennis van de afdeling (afspraken, regels, doelgroep).
 * Standaard komt die uit assets/ubb2_kennis.md; een begeleider kan de tekst
 * in de app aanpassen, dan wordt een lokale kopie gebruikt.
 */
class KnowledgeBase(private val context: Context, val documents: KnowledgeDocuments) {

    private val overrideFile = File(context.filesDir, "ubb2_kennis.md")

    fun load(): String =
        if (overrideFile.exists()) overrideFile.readText()
        else context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    fun save(text: String) = overrideFile.writeText(text)

    fun resetToDefault() {
        overrideFile.delete()
    }

    fun systemPrompt(role: Role): String {
        val common = """
            Je bent "Whats Ubb", de digitale assistent van afdeling UBB2.
            Je antwoordt altijd in het Nederlands.

            PRIVACY (altijd):
            - Alles is anoniem. Vraag nooit naar namen, geboortedata, adressen, BSN of andere herleidbare gegevens.
            - Noem nooit namen, ook niet als die toch in een vraag staan. Spreek over "de bewoner" of "de cliënt".
            - Tekst tussen blokhaken zoals [naam], [datum] of [telefoon] is bewust geanonimiseerd. Laat dat zo.

            KENNIS VAN DE AFDELING:
            Hieronder staan de afspraken, regels en informatie over de doelgroep van UBB2,
            gevolgd door documenten die het team heeft toegevoegd (regels, afspraken, werkwijze).
            Baseer je antwoorden over de afdeling ALLEEN op deze tekst. Staat iets er niet in,
            zeg dan eerlijk dat je het niet zeker weet en verwijs naar de begeleiding. Verzin geen regels.
            Spreken bronnen elkaar tegen, noem dat dan en verwijs naar de begeleiding.
        """.trimIndent() + "\n\n===== BEGIN KENNIS UBB2 =====\n" + load() +
            documents.combinedText().let { if (it.isBlank()) "" else "\n\n# Toegevoegde documenten\n\n$it" } +
            "\n===== EINDE KENNIS UBB2 ====="

        val roleSpecific = when (role) {
            Role.BEWONER -> """
                JE PRAAT NU MET EEN BEWONER:
                - Gebruik eenvoudige, korte zinnen (taalniveau B1). Wees vriendelijk, rustig en respectvol.
                - Leg uit wát de afspraak is en kort waaróm, zonder te betuttelen.
                - Geef geen medisch, juridisch of medicatie-advies; verwijs dan naar de begeleiding of behandelaar.
                - Gaat het over crisis, gevaar of zelfbeschadiging: zeg duidelijk dat de bewoner direct
                  naar de begeleiding moet gaan, of bij acuut gevaar 112 moet bellen. Bij suïcidale gedachten
                  kan ook 113 (0800-0113) gebeld worden.
                - Je kunt geen uitzonderingen op regels toezeggen; dat beslist de begeleiding.
            """.trimIndent()

            Role.BEGELEIDER -> """
                JE PRAAT NU MET EEN BEGELEIDER (professional):
                - Schrijf professioneel, feitelijk en methodisch (observatie gescheiden van interpretatie).
                - Houd rekening met de doelgroep van UBB2 zoals hierboven beschreven.
                - Gebruik een heldere opbouw met kopjes waar dat past. Geen opvulling.
                - Ontbreekt er informatie voor een goed document, zet dan [AANVULLEN: ...] op die plek
                  in plaats van iets te verzinnen.
            """.trimIndent()
        }
        return common + "\n\n" + roleSpecific
    }

    companion object {
        const val ASSET_NAME = "ubb2_kennis.md"
    }
}
