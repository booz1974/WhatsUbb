<p align="center">
  <img src="app/src/main/res/drawable-nodpi/whats_ubb_logo.png" alt="Whats Ubb" width="220">
</p>

<h1 align="center">Whats Ubb</h1>

<p align="center">
  <b>Anoniem, duidelijk en altijd bereikbaar</b><br>
  Een AI-assistent voor bewoners en begeleiders van afdeling UBB2
</p>

<p align="center">
  <img alt="Versie" src="https://img.shields.io/badge/versie-1.7.2-5B8276">
  <img alt="Android" src="https://img.shields.io/badge/Android-7.0%2B-5B8276?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-DDA77A?logo=kotlin&logoColor=white">
  <img alt="AI" src="https://img.shields.io/badge/AI-Google%20Gemini-2C3E35?logo=googlegemini&logoColor=white">
</p>

---

## Wat is Whats Ubb?

Whats Ubb is een Android-app voor een woonafdeling voor jongvolwassenen (18–30 jaar) met een licht verstandelijke
beperking, autisme, persoonlijkheidsproblematiek of multiproblematiek. Het doel van de afdeling is dat bewoners binnen
drie jaar zelfstandig kunnen wonen.

- **Bewoners** kunnen de app alles vragen: over de regels en afspraken, het weekmenu, wie er werkt, hoe je iets doet,
  maar ook als ze zich niet fijn voelen of ergens bang voor zijn.
- **Begeleiders** maken met de AI in een paar minuten werkplannen, rapportages en gespreksverslagen.
- **De beheerder** bepaalt wat de AI weet: de afspraken, documenten, de werkwijze en de rol van de AI.

Alles is gebouwd rond **anonimiteit**: er worden geen namen of persoonsgegevens van bewoners opgeslagen of verstuurd.

<p align="center">
  <img src="docs/screenshots/inloggen.png" alt="Inlogscherm" width="230">
  &nbsp;&nbsp;
  <img src="docs/screenshots/bewoner-welkom.png" alt="Welkomstscherm bewoner, met knoppen om de begeleiding te bellen of via WhatsApp te chatten" width="230">
  &nbsp;&nbsp;
  <img src="docs/screenshots/bewoner-chat.png" alt="Chat met de AI, met de 113-knoppen" width="230">
</p>

---

## Functies

### 🙋 Voor bewoners

| | |
|---|---|
| 💬 **Vraag alles** | Chat in eenvoudige taal (B1) over regels, afspraken, het weekmenu, het rooster, apparaten op de afdeling, praktische dingen en gevoelens |
| 🎙️ **Spraak** | Microfoonknop in het invoerveld: inspreken in plaats van typen |
| 👆 **Voorbeeldvragen** | Aanklikbare vragen zoals *"Wat eten we vanavond?"* en *"Ik voel me niet zo goed"* |
| 📞 **Bel- en chatknop** | Eén tik om de begeleiding te bellen of via WhatsApp te chatten; tussen 23:00 en 08:00 de bereikbare dienst (met controlevraag) |
| 📄 **Antwoord als PDF** | Elk antwoord (met de vraag erbij) opslaan als PDF of direct delen als PDF |
| 🆘 **Veilig** | Bij crisis verwijst de AI naar de begeleiding, de bereikbare dienst, 112 of 113, afhankelijk van het tijdstip |
| 💚 **113-knoppen** | Gaat het over somberheid of suïcidale gedachten? Dan verschijnen onder het antwoord knoppen om 113 te bellen of te chatten (gratis, anoniem, dag en nacht) |

### 🧑‍💼 Voor begeleiders (begeleider-PIN)

| | |
|---|---|
| 🗺️ **Werkplan** | Vaste aanpak voor een taak (kamer schoonmaken, koken, boodschappen, logeren...) in één vaste UBB2-stijl: wat doet het team, wat doet de bewoner, vaste volgorde, begeleidingsniveau 0-5 en eindcontrole. Met een **aftekenkaart voor de bewoner** en een **evaluatieformulier** op aparte pagina's ([voorbeeld](docs/voorbeelden/werkplan-koken.pdf)) |
| 📝 **Dagrapportage** | Van steekwoorden naar een objectieve rapportage |
| 🗣️ **Gespreksverslag** | Inclusief **gesprek opnemen** en automatisch laten uitschrijven door Gemini |
| 🚦 **Signaleringsplan** | Signalen en acties per fase (groen / oranje / rood) |
| ✨ **Vrije opdracht** | Brief, uitleg, dagprogramma of iets anders |
| 🗓️ **Weekinfo** | Foto van het weekmenu en werkrooster: de AI leest ze uit zodat bewoners ernaar kunnen vragen. Ontbreekt die voor de huidige week (ma t/m zo), dan krijg je bij het inloggen een melding. Op zondag kijkt de app naar de week die morgen begint: ontbreekt daar iets, dan kom je na het inloggen direct in Weekinfo |
| 💬 **Vraag over afspraken** | Chat over regels, afspraken en de doelgroep |
| 📄 **PDF** | Elk document als A4-PDF met logo, datum en paginanummers; ook kopiëren en delen |

<p align="center">
  <img src="docs/screenshots/begeleider-menu.png" alt="Menu voor begeleiders, met melding over ontbrekende weekinfo" width="230">
  &nbsp;&nbsp;
  <img src="docs/screenshots/weekinfo.png" alt="Weekinfo: foto van weekmenu en werkrooster" width="230">
  &nbsp;&nbsp;
  <img src="docs/screenshots/gespreksverslag.png" alt="Gespreksverslag met gesprek opnemen" width="230">
</p>

### ⚙️ Voor de beheerder (aparte beheerders-PIN)

| | |
|---|---|
| 🧠 **AI-rol** | De persoonlijkheid van de AI: gedragswetenschapper, doelgroep, methodieken (Verbindend Gezag & Geweldloos Verzet, Geef me de Vijf) en aanwezigheidstijden |
| 📎 **Documenten** | Regels, afspraken en werkwijze uploaden als PDF, Word (.docx) of tekst |
| 🔌 **Apparaten** | Foto van de wasmachine, droger, het koffieapparaat of de kookplaat: de AI leest de knoppen en programma's uit, zodat bewoners stap voor stap uitleg krijgen over precies dat apparaat (alleen de tekst wordt bewaard) |
| 📚 **Kennisbank** | Vrije tekst met de afspraken en informatie over de afdeling |
| 📞 **Telefoonnummers** | 06-nummer van de begeleiding en nummer van de bereikbare dienst |
| 👤 **Voornaam** | Voornaam van de begeleider op dit toestel: het menu begroet je ermee en je kunt de AI vragen "wanneer werk ik?". Bewoners krijgen de naam nooit mee |
| 🔑 **Toegang** | Bewonerscode (optioneel), begeleider-PIN, beheerders-PIN, AI-sleutel en model |

---

## Hoe de AI werkt

Bij elke vraag stelt de app één instructie samen voor Google Gemini:

```
1. AI-rol            wie de AI is en hoe hij reageert           (Beheer → AI-rol)
2. Datum en tijd     plus de komende 8 dagen, zodat "maandagochtend" altijd de eerstvolgende is
3. Privacyregels     nooit namen van bewoners, anoniem blijven
4. Kennisbank        afspraken en informatie over UBB2          (Beheer → Kennisbank)
5. Documenten        geüploade regels en werkwijzen             (Beheer → Documenten)
6. Weekinfo          weekmenu en rooster van deze/volgende week (Begeleider → Weekinfo)
7. Rolinstructie     taal en toon voor bewoner óf begeleider
```

De AI is opgedragen om **alleen** op deze kennis te antwoorden en eerlijk te zeggen als hij iets niet weet,
in plaats van regels te verzinnen.

---

## Privacy en veiligheid

> [!IMPORTANT]
> De anonimisering is een vangnet, geen garantie. Schrijf altijd zonder namen, gebruik voor echt gebruik een
> **betaalde** Gemini API (bij de gratis versie mag Google ingestuurde tekst gebruiken) en stem het gebruik af met de
> privacyfunctionaris (FG) van de organisatie.

- **Anonimiseren vóór verzending**: telefoonnummers, e-mailadressen, data, adressen, postcodes, BSN, IBAN en
  opgegeven namen worden vervangen door `[naam]`, `[datum]` enz.
- **Niets bewaren**: gesprekken en gemaakte documenten worden niet opgeslagen; na uitloggen is alles weg. Gedeelde
  PDF's worden bij de volgende keer delen en bij het starten van de app gewist.
- **Opnames**: alleen na een toestemmingsvinkje; na het uitschrijven direct bij Google verwijderd en van het toestel
  gewist zodra het scherm wordt verlaten (tenzij zelf opgeslagen).
- **Foto's** van weekmenu en rooster: alleen de uitgelezen tekst wordt bewaard, nooit de foto. Verlopen weken worden
  automatisch verwijderd.
- **Codes** worden gehasht opgeslagen; Android-back-ups staan uit.
- Namen van **medewerkers** uit het rooster mag de AI noemen; namen van **bewoners** nooit.

---

## Aan de slag

### Benodigd
- [Android Studio](https://developer.android.com/studio) (met JDK 17+)
- Een Gemini API-sleutel van [Google AI Studio](https://aistudio.google.com/apikey)
- Een Android-toestel of emulator met Android 7.0 of hoger

### Bouwen

```bash
git clone https://github.com/booz1974/WhatsUbb.git
cd WhatsUbb
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unittests
```

### AI-sleutel instellen

Kies één van beide:

1. **In de app** — Begeleider → ⚙️ Beheer → AI-sleutel *(aanbevolen voor gedeelde toestellen)*
2. **Bij het bouwen** — voeg toe aan `local.properties` (staat in `.gitignore`):
   ```properties
   GEMINI_API_KEY=jouw_sleutel
   ```

> [!WARNING]
> Een sleutel die in de APK is ingebouwd kan eruit worden gehaald. Deel zo'n APK niet met anderen.

### Eerste keer gebruiken
1. Tik op **Ik ben begeleider** en stel de begeleider-PIN in.
2. Open **⚙️ Beheer** en stel de beheerders-PIN in.
3. Vul de **AI-sleutel**, de **kennisbank**, de **telefoonnummers** en eventueel **documenten** in.
4. Zet via **🗓️ Weekinfo** het weekmenu en rooster van deze week klaar.
5. Voeg in **⚙️ Beheer** bij **🔌 Apparaten** foto's toe van de wasmachine, droger, het koffieapparaat en andere apparaten.

---

## Projectstructuur

```
app/src/main/
├── assets/
│   ├── ai_rol.md              standaard AI-rol (prompt)
│   ├── werkplan_voorbeeld.json voorbeeld-werkplan (stijl en opbouw voor de AI)
│   └── ubb2_kennis.md         standaard kennisbank (sjabloon)
└── java/com/example/whatsubb/
    ├── MainActivity.kt        navigatie en AppServices
    ├── ai/                    GeminiClient: chat, Files API (audio, PDF, foto's)
    ├── audio/                 opnemen van gesprekken (AAC)
    ├── data/                  instellingen, kennisbank, documenten, weekinfo, apparaten, documenttypes
    ├── privacy/               Anonymizer
    ├── ui/                    Compose-schermen
    └── util/                  PDF-export (ook werkplan-opmaak), Markdown, documentimport, foto's, weken
design/                        origineel logo
docs/screenshots/              screenshots voor deze README
docs/voorbeelden/              voorbeeld-PDF's
```

### Techniek
- **Kotlin** + **Jetpack Compose** (Material 3), minSdk 24, targetSdk 37
- **Google Gemini** via de REST-API (`generateContent` en de Files API) — geen extra AI-libraries
- PDF met de ingebouwde `PdfDocument`, Word-import zonder externe libraries
- Spraakherkenning via de spraakdienst van het toestel

---

## Bekende beperkingen en ideeën

- Alle instellingen, documenten en weekinfo staan **per toestel**. Een kleine gedeelde server zou ervoor zorgen dat
  de beheerder één keer uploadt voor alle toestellen, en dat de AI-sleutel niet meer in de app hoeft.
- Opnames zijn AAC in plaats van MP3 (Android kan geen MP3 opnemen).
- Het standaardmodel is `gemini-flash-latest`; reageert dat traag, kies dan in Beheer bijvoorbeeld
  `gemini-flash-lite-latest`.

---

<p align="center"><sub>Privéproject voor afdeling UBB2 · gemaakt met ❤️ en Claude Code</sub></p>
