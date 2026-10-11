package com.example.whatsubb.data

import android.content.Context
import com.example.whatsubb.util.Weeks
import java.io.File

enum class Role { BEWONER, BEGELEIDER }

/**
 * De "Gem": de vaste kennis van de afdeling (afspraken, regels, doelgroep).
 * Standaard komt die uit assets/ubb2_kennis.md; een begeleider kan de tekst
 * in de app aanpassen, dan wordt een lokale kopie gebruikt.
 */
class KnowledgeBase(
    private val context: Context,
    val documents: KnowledgeDocuments,
    val weekInfo: WeekInfoStore,
    val devices: DeviceStore,
    /** Wie de bel- en chatknop bij bewoners nu bereiken (bijv. "de begeleiding"), of null als er geen knoppen zijn. */
    private val contactName: () -> String? = { null }
) {

    private fun callButtonHint(): String {
        val name = contactName() ?: return ""
        return "\n- In de app staan bovenaan de knoppen \"Bel $name\" en \"Chat met $name\" (via WhatsApp). " +
            "Moet de bewoner iemand dringend spreken, wijs dan op die knoppen."
    }

    private val overrideFile = File(context.filesDir, "ubb2_kennis.md")

    private fun weekInfoBlock(): String {
        val info = weekInfo.promptText()
        val intro = """
            WEEKINFO (weekmenu en werkrooster, uitgelezen van de foto's die op de afdeling hangen):
            Gebruik de datum van vandaag om te bepalen welke dag het is. Is er geen info voor de gevraagde week,
            zeg dan dat die nog niet bekend is en dat de bewoner op het bord kan kijken of het aan de begeleiding kan vragen.
        """.trimIndent()
        return "$intro\n\n===== BEGIN WEEKINFO =====\n" +
            info.ifBlank { "(Er is op dit moment geen actuele weekinfo.)" } +
            "\n===== EINDE WEEKINFO ====="
    }

    private fun devicesBlock(): String {
        val info = devices.promptText()
        if (info.isBlank()) return ""
        val intro = """
            APPARATEN OP DE AFDELING (uitgelezen van foto's van de apparaten zelf):
            Vraagt iemand hoe een apparaat werkt, gebruik dan deze beschrijving: noem de knoppen en standen zoals ze
            op dit apparaat staan en leg het uit in korte, genummerde stappen. Weet je iets niet zeker, of staat het
            apparaat er niet bij, zeg dat dan en verwijs naar de begeleiding.
        """.trimIndent()
        return "\n\n$intro\n\n===== BEGIN APPARATEN =====\n$info\n===== EINDE APPARATEN ====="
    }

    fun load(): String =
        if (overrideFile.exists()) overrideFile.readText()
        else context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    fun save(text: String) = overrideFile.writeText(text)

    fun resetToDefault() {
        overrideFile.delete()
    }

    /** De rol/persoonlijkheid van de AI; gaat vóór elke vraag en elk document mee. */
    private val personaFile = File(context.filesDir, "ai_rol.md")

    fun loadPersona(): String =
        if (personaFile.exists()) personaFile.readText()
        else context.assets.open(PERSONA_ASSET).bufferedReader().use { it.readText() }

    fun savePersona(text: String) = personaFile.writeText(text)

    fun resetPersona() {
        personaFile.delete()
    }

    fun systemPrompt(role: Role): String {
        val common = """
            Je bent "Whats Ubb", de digitale assistent van afdeling UBB2.
            Je antwoordt altijd in het Nederlands.
            Vandaag is het ${Weeks.today()}.

            PRIVACY (altijd):
            - Alles is anoniem. Vraag nooit naar namen, geboortedata, adressen, BSN of andere herleidbare gegevens.
            - Noem nooit namen van bewoners, ook niet als die toch in een vraag staan. Spreek over "de bewoner" of "de cliënt".
              Namen van medewerkers uit het werkrooster mag je wel noemen.
            - Tekst tussen blokhaken zoals [naam], [datum] of [telefoon] is bewust geanonimiseerd. Laat dat zo.

            KENNIS VAN DE AFDELING:
            Hieronder staan de afspraken, regels en informatie over de doelgroep van UBB2,
            gevolgd door documenten die het team heeft toegevoegd (regels, afspraken, werkwijze).
            Baseer je antwoorden over de afdeling ALLEEN op deze tekst. Staat iets er niet in,
            zeg dan eerlijk dat je het niet zeker weet en verwijs naar de begeleiding. Verzin geen regels.
            Spreken bronnen elkaar tegen, noem dat dan en verwijs naar de begeleiding.
        """.trimIndent() + "\n\n===== BEGIN KENNIS UBB2 =====\n" + load() +
            documents.combinedText().let { if (it.isBlank()) "" else "\n\n# Toegevoegde documenten\n\n$it" } +
            "\n===== EINDE KENNIS UBB2 =====\n\n" + weekInfoBlock() + devicesBlock()

        val roleSpecific = when (role) {
            Role.BEWONER -> """
                JE PRAAT NU MET EEN BEWONER:
                - Gebruik eenvoudige, korte zinnen (taalniveau B1). Wees vriendelijk, rustig en respectvol.
                - De bewoner mag je alles vragen: over regels en afspraken, het weekmenu, wie er werkt,
                  hoe je iets moet doen (bijv. de was, een afspraak maken), en ook hoe de bewoner zich voelt.
                - Gaat het over gevoelens (bang, verdrietig, boos, eenzaam, onrustig): luister eerst, laat merken
                  dat je het begrijpt, stel hooguit één rustige vraag, en geef eventueel een kleine tip die nu kan helpen.
                  Moedig aan om er ook met een begeleider over te praten; noem dan wie er volgens het rooster werkt als dat bekend is.
                - Helpt de bewoner met iets praktisch? Leg het uit in kleine, genummerde stappen.
                - Leg bij een afspraak uit wát de afspraak is en kort waaróm, zonder te betuttelen.
                - Geef geen medisch, juridisch of medicatie-advies; verwijs dan naar de begeleiding of behandelaar.
                - Gaat het over crisis, gevaar of zelfbeschadiging: zeg duidelijk dat de bewoner direct
                  naar de begeleiding moet gaan, of bij acuut gevaar 112 moet bellen. Bij suïcidale gedachten
                  kan ook 113 (0800-0113) gebeld worden. Kijk naar de tijd van nu: is de begeleiding er op dit
                  moment niet (zie de aanwezigheidstijden), zeg dat dan eerlijk. Tussen 23:00 en 08:00 kan de bewoner
                  altijd de bereikbare dienst bellen als het echt niet kan wachten tot de volgende dag; help de bewoner
                  rustig inschatten of iets kan wachten. Bij acuut gevaar altijd 112.
                - Speelt somberheid, hopeloosheid, doodsgedachten of suïcidaliteit een rol (ook als het voorzichtig
                  of verhuld gezegd wordt)? Zet dan helemaal aan het eind van je antwoord op een eigen regel: ${HELP_113_MARKER}
                  De app laat dan knoppen zien om 113 te bellen of met 113 te chatten; je mag daarnaar verwijzen.
                - Je kunt geen uitzonderingen op regels toezeggen; dat beslist de begeleiding.
            """.trimIndent() + callButtonHint()

            Role.BEGELEIDER -> """
                JE PRAAT NU MET EEN BEGELEIDER (professional):
                - Schrijf professioneel, feitelijk en methodisch (observatie gescheiden van interpretatie).
                - Houd rekening met de doelgroep van UBB2 zoals hierboven beschreven.
                - Gebruik een heldere opbouw met kopjes waar dat past. Geen opvulling.
                - Ontbreekt er informatie voor een goed document, zet dan [AANVULLEN: ...] op die plek
                  in plaats van iets te verzinnen.
            """.trimIndent()
        }
        return "JOUW ROL:\n" + loadPersona().trim() + "\n\n" + common + "\n\n" + roleSpecific
    }

    companion object {
        const val ASSET_NAME = "ubb2_kennis.md"
        const val PERSONA_ASSET = "ai_rol.md"
        /** Signaal van de AI dat de 113-knoppen getoond moeten worden; wordt uit het antwoord gehaald. */
        const val HELP_113_MARKER = "[113]"
    }
}
